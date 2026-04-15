import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { deleteChatMemory, postChatMessage } from '@/api/ai'
import {
  AI_CHAT_DEFAULT_ERROR,
  AI_CHAT_EMPTY_INPUT_ERROR,
  AI_CHAT_MESSAGE_ROLE,
  AI_CHAT_MESSAGE_STATUS,
  AI_CHAT_EMPTY_RESPONSE_TEXT,
  AI_CHAT_STATE,
  AI_CHAT_STORE_ID,
  type AiChatMessage,
  type AiChatMessageRole,
  type AiChatMessageStatus,
  type AiChatState
} from '@/constants/ai'

const resolveErrorMessage = (error: unknown): string => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return AI_CHAT_DEFAULT_ERROR
}

let aiChatMessageSequence = 0

export const useAiChatStore = defineStore(AI_CHAT_STORE_ID, () => {
  const isDrawerOpen = ref<boolean>(false)
  const inputMessage = ref<string>('')
  const messages = ref<AiChatMessage[]>([])
  const errorMessage = ref<string>('')
  const state = ref<AiChatState>(AI_CHAT_STATE.idle)

  const isLoading = computed<boolean>(() => state.value === AI_CHAT_STATE.loading)

  const openDrawer = (): void => {
    isDrawerOpen.value = true
  }

  const closeDrawer = (): void => {
    isDrawerOpen.value = false
  }

  const setInput = (message: string): void => {
    inputMessage.value = message
  }

  const createMessageId = (): string => {
    aiChatMessageSequence += 1
    return `ai-chat-message-${aiChatMessageSequence}`
  }

  const createMessage = (
    role: AiChatMessageRole,
    content: string,
    status: AiChatMessageStatus = AI_CHAT_MESSAGE_STATUS.done
  ): AiChatMessage => ({
    id: createMessageId(),
    role,
    content,
    status
  })

  const appendMessage = (
    role: AiChatMessageRole,
    content: string,
    status: AiChatMessageStatus = AI_CHAT_MESSAGE_STATUS.done
  ): string => {
    const message = createMessage(role, content, status)
    messages.value.push(message)
    return message.id
  }

  const updateMessage = (messageId: string, updater: (message: AiChatMessage) => void): void => {
    const targetMessage = messages.value.find(message => message.id === messageId)
    if (targetMessage) {
      updater(targetMessage)
    }
  }

  const removeMessage = (messageId: string): void => {
    messages.value = messages.value.filter(message => message.id !== messageId)
  }

  const finalizeAssistantMessage = (messageId: string): void => {
    updateMessage(messageId, message => {
      message.content = message.content.trim()
        ? message.content
        : AI_CHAT_EMPTY_RESPONSE_TEXT
      message.status = AI_CHAT_MESSAGE_STATUS.done
    })
  }

  const resetConversationState = (): void => {
    messages.value = []
    inputMessage.value = ''
    errorMessage.value = ''
    state.value = AI_CHAT_STATE.idle
  }

  const resetError = (): void => {
    errorMessage.value = ''
    if (state.value === AI_CHAT_STATE.error) {
      state.value = AI_CHAT_STATE.idle
    }
  }

  const sendMessage = async (): Promise<void> => {
    if (isLoading.value) {
      return
    }

    const message = inputMessage.value.trim()

    if (!message) {
      state.value = AI_CHAT_STATE.error
      errorMessage.value = AI_CHAT_EMPTY_INPUT_ERROR
      return
    }

    state.value = AI_CHAT_STATE.loading
    errorMessage.value = ''
    appendMessage(AI_CHAT_MESSAGE_ROLE.user, message)
    inputMessage.value = ''
    const assistantMessageId = appendMessage(
      AI_CHAT_MESSAGE_ROLE.assistant,
      '',
      AI_CHAT_MESSAGE_STATUS.streaming
    )

    try {
      let hasDoneEvent = false
      await postChatMessage(message, {
        onChunk: (chunk: string) => {
          updateMessage(assistantMessageId, currentMessage => {
            currentMessage.content += chunk
          })
        },
        onDone: () => {
          hasDoneEvent = true
        }
      })

      finalizeAssistantMessage(assistantMessageId)

      if (hasDoneEvent) {
        state.value = AI_CHAT_STATE.success
        return
      }

      state.value = AI_CHAT_STATE.success
    } catch (error) {
      const assistantMessage = messages.value.find(messageItem => messageItem.id === assistantMessageId)
      if (!assistantMessage?.content.trim()) {
        removeMessage(assistantMessageId)
      } else {
        updateMessage(assistantMessageId, currentMessage => {
          currentMessage.status = AI_CHAT_MESSAGE_STATUS.done
        })
      }
      errorMessage.value = resolveErrorMessage(error)
      state.value = AI_CHAT_STATE.error
    }
  }

  const clearConversation = async (): Promise<void> => {
    if (isLoading.value) {
      return
    }

    resetConversationState()

    try {
      await deleteChatMemory()
    } catch (error) {
      errorMessage.value = resolveErrorMessage(error)
      state.value = AI_CHAT_STATE.error
    }
  }

  return {
    isDrawerOpen,
    inputMessage,
    messages,
    errorMessage,
    state,
    isLoading,
    openDrawer,
    closeDrawer,
    setInput,
    resetError,
    sendMessage,
    clearConversation
  }
})
