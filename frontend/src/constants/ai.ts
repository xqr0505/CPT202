export const AI_API_CHAT_PATH = '/api/v1/ai/chat'
export const AI_API_CHAT_MEMORY_PATH = '/api/v1/ai/chat/memory'
export const AI_CHAT_STREAM_EVENT = 'chat'
export const AI_CHAT_STREAM_DONE_EVENT = 'done'

export const AI_CHAT_STORE_ID = 'aiChat'

export const AI_NAV_MENU_KEY = '__customer_ai_chat__'
export const AI_NAV_MENU_LABEL = 'AI Assistant'

export const AI_DRAWER_TITLE = 'AI Assistant'
export const AI_DRAWER_DIRECTION = 'rtl'
export const AI_DRAWER_SIZE = 'var(--ai-chat-drawer-width)'
export const AI_DRAWER_APPEND_TO_BODY = true

export const AI_CHAT_INPUT_PLACEHOLDER = 'Ask a question about your consultation needs...'
export const AI_CHAT_SEND_BUTTON_TEXT = 'Send'
export const AI_CHAT_CLEAR_BUTTON_TEXT = 'Clear conversation'
export const AI_CHAT_THINKING_TEXT = 'AI is thinking...'
export const AI_CHAT_EMPTY_RESPONSE_TEXT = 'No response received. Please try again.'
export const AI_CHAT_EMPTY_STATE_TEXT = 'Ask about bookings, account access, schedules, or platform usage.'
export const AI_CHAT_EMPTY_INPUT_ERROR = 'Please enter a message before sending.'
export const AI_CHAT_DEFAULT_ERROR = 'Failed to get AI response. Please try again.'
export const AI_CHAT_AUTH_ERROR = 'Unauthorized, please login again.'
export const AI_CHAT_USER_LABEL = 'You'
export const AI_CHAT_ASSISTANT_LABEL = 'AI Assistant'

export const AI_CHAT_TEXTAREA_ROWS = 4
export const AI_CHAT_TEXTAREA_MAX_LENGTH = 1000

export const AI_CHAT_MESSAGE_ROLE = {
  user: 'user',
  assistant: 'assistant'
} as const

export type AiChatMessageRole = typeof AI_CHAT_MESSAGE_ROLE[keyof typeof AI_CHAT_MESSAGE_ROLE]

export const AI_CHAT_MESSAGE_STATUS = {
  streaming: 'streaming',
  done: 'done'
} as const

export type AiChatMessageStatus =
  typeof AI_CHAT_MESSAGE_STATUS[keyof typeof AI_CHAT_MESSAGE_STATUS]

export interface AiChatMessage {
  id: string
  role: AiChatMessageRole
  content: string
  status: AiChatMessageStatus
}

export const AI_CHAT_STATE = {
  idle: 'idle',
  loading: 'loading',
  success: 'success',
  error: 'error'
} as const

export type AiChatState = typeof AI_CHAT_STATE[keyof typeof AI_CHAT_STATE]
