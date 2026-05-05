import {
  AI_API_CHAT_MEMORY_PATH,
  AI_API_CHAT_PATH,
  AI_CHAT_AUTH_ERROR,
  AI_CHAT_DEFAULT_ERROR,
  AI_CHAT_STREAM_DONE_EVENT,
  AI_CHAT_STREAM_EVENT
} from '@/constants/ai'
import { apiBaseUrl } from '@/config/api'
import request, { getAuthToken, logout } from './request'

interface ChatRequestPayload {
  message: string
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

interface ChatStreamChunk {
  content: string
  done: boolean
}

interface StreamCallbacks {
  onChunk: (chunk: string) => void
  onDone: () => void
}

const parseErrorMessage = async (response: Response): Promise<string> => {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    const json = (await response.json()) as Partial<ApiResult<unknown>>
    return json.message?.trim() || AI_CHAT_DEFAULT_ERROR
  }
  return (await response.text()).trim() || AI_CHAT_DEFAULT_ERROR
}

const handleSseEvent = (block: string, callbacks: StreamCallbacks): void => {
  const lines = block.split('\n')
  let eventName = ''
  const dataParts: string[] = []

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataParts.push(line.slice(5).trim())
    }
  }

  if (!dataParts.length) {
    return
  }

  const payload = JSON.parse(dataParts.join('\n')) as ApiResult<ChatStreamChunk>
  if (payload.code !== 200) {
    throw new Error(payload.message || AI_CHAT_DEFAULT_ERROR)
  }

  if (eventName === AI_CHAT_STREAM_DONE_EVENT || payload.data?.done) {
    callbacks.onDone()
    return
  }

  if (eventName === AI_CHAT_STREAM_EVENT && payload.data?.content) {
    callbacks.onChunk(payload.data.content)
  }
}

export const postChatMessage = async (message: string, callbacks: StreamCallbacks): Promise<void> => {
  const payload: ChatRequestPayload = { message }
  const token = getAuthToken()
  const response = await fetch(`${apiBaseUrl}${AI_API_CHAT_PATH}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload)
  })

  if (response.status === 401) {
    logout()
    throw new Error(AI_CHAT_AUTH_ERROR)
  }

  if (!response.ok || !response.body) {
    throw new Error(await parseErrorMessage(response))
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    buffer = buffer.replace(/\r\n/g, '\n')

    let boundaryIndex = buffer.indexOf('\n\n')
    while (boundaryIndex >= 0) {
      const block = buffer.slice(0, boundaryIndex).trim()
      if (block) {
        handleSseEvent(block, callbacks)
      }
      buffer = buffer.slice(boundaryIndex + 2)
      boundaryIndex = buffer.indexOf('\n\n')
    }
  }

  const finalBlock = buffer.trim()
  if (finalBlock) {
    handleSseEvent(finalBlock, callbacks)
  }
}

export const deleteChatMemory = async (): Promise<void> => {
  await request.delete(AI_API_CHAT_MEMORY_PATH, {
    suppressErrorMessage: true
  })
}
