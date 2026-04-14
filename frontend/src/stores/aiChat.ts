import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { postChatMessage } from '@/api/ai'
import {
  AI_CHAT_DEFAULT_ERROR,
  AI_CHAT_EMPTY_INPUT_ERROR,
  AI_CHAT_EMPTY_RESPONSE_TEXT,
  AI_CHAT_STATE,
  AI_CHAT_STORE_ID,
  AI_CHAT_THINKING_TEXT,
  type AiChatState
} from '@/constants/ai'

const resolveErrorMessage = (error: unknown): string => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return AI_CHAT_DEFAULT_ERROR
}

export const useAiChatStore = defineStore(AI_CHAT_STORE_ID, () => {
  const isDrawerOpen = ref<boolean>(false)
  const inputMessage = ref<string>('')
  const answerMessage = ref<string>('')
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

  const resetError = (): void => {
    errorMessage.value = ''
    if (state.value === AI_CHAT_STATE.error) {
      state.value = AI_CHAT_STATE.idle
    }
  }

  const sendMessage = async (): Promise<void> => {
    const message = inputMessage.value.trim()

    if (!message) {
      state.value = AI_CHAT_STATE.error
      errorMessage.value = AI_CHAT_EMPTY_INPUT_ERROR
      return
    }

    state.value = AI_CHAT_STATE.loading
    errorMessage.value = ''
    answerMessage.value = AI_CHAT_THINKING_TEXT

    try {
      const reply = await postChatMessage(message)
      answerMessage.value = reply?.trim() ? reply : AI_CHAT_EMPTY_RESPONSE_TEXT
      state.value = AI_CHAT_STATE.success
    } catch (error) {
      answerMessage.value = ''
      errorMessage.value = resolveErrorMessage(error)
      state.value = AI_CHAT_STATE.error
    }
  }

  return {
    isDrawerOpen,
    inputMessage,
    answerMessage,
    errorMessage,
    state,
    isLoading,
    openDrawer,
    closeDrawer,
    setInput,
    resetError,
    sendMessage
  }
})
