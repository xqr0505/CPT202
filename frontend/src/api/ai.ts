import request from './request'
import { AI_API_CHAT_PATH } from '@/constants/ai'

interface ChatRequestPayload {
  message: string
}

export const postChatMessage = (message: string): Promise<string> => {
  const payload: ChatRequestPayload = { message }
  return request.post<ChatRequestPayload, string>(AI_API_CHAT_PATH, payload)
}
