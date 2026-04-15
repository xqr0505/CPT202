<template>
  <div ref="scrollContainer" class="ai-message-list" role="log" aria-live="polite">
    <div v-if="!messages.length" class="ai-message-list__empty">
      <p class="ai-message-list__empty-title">{{ AI_DRAWER_TITLE }}</p>
      <p class="ai-message-list__empty-text">{{ AI_CHAT_EMPTY_STATE_TEXT }}</p>
    </div>

    <article
      v-for="message in messages"
      :key="message.id"
      class="ai-message"
      :class="messageClassName(message)"
    >
      <p class="ai-message__label">{{ messageLabel(message.role) }}</p>
      <div class="ai-message__bubble">
        <p class="ai-message__content">{{ messageContent(message) }}</p>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import {
  AI_CHAT_EMPTY_STATE_TEXT,
  AI_CHAT_MESSAGE_ROLE,
  AI_CHAT_MESSAGE_STATUS,
  AI_CHAT_THINKING_TEXT,
  AI_CHAT_USER_LABEL,
  AI_CHAT_ASSISTANT_LABEL,
  AI_DRAWER_TITLE,
  type AiChatMessage,
  type AiChatMessageRole
} from '@/constants/ai'

interface Props {
  messages: AiChatMessage[]
}

const props = defineProps<Props>()
const scrollContainer = ref<HTMLElement | null>(null)

const lastMessageSignature = computed<string>(() => {
  const lastMessage = props.messages.at(-1)
  if (!lastMessage) {
    return ''
  }

  return `${lastMessage.id}:${lastMessage.status}:${lastMessage.content}`
})

const scrollToBottom = async (): Promise<void> => {
  await nextTick()
  if (scrollContainer.value) {
    scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
  }
}

watch(
  () => props.messages.length,
  () => {
    void scrollToBottom()
  }
)

watch(lastMessageSignature, () => {
  void scrollToBottom()
})

const messageClassName = (message: AiChatMessage): string => {
  return message.role === AI_CHAT_MESSAGE_ROLE.user
    ? 'ai-message--user'
    : 'ai-message--assistant'
}

const messageLabel = (role: AiChatMessageRole): string => {
  return role === AI_CHAT_MESSAGE_ROLE.user
    ? AI_CHAT_USER_LABEL
    : AI_CHAT_ASSISTANT_LABEL
}

const messageContent = (message: AiChatMessage): string => {
  if (
    message.role === AI_CHAT_MESSAGE_ROLE.assistant &&
    message.status === AI_CHAT_MESSAGE_STATUS.streaming &&
    !message.content.trim()
  ) {
    return AI_CHAT_THINKING_TEXT
  }

  return message.content
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.ai-message-list {
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--ai-chat-message-gap);
  padding: var(--ai-chat-list-padding);
  background: var(--ai-chat-list-background);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
}

.ai-message-list__empty {
  margin: auto 0;
  padding: var(--space-8) var(--space-4);
  text-align: center;
}

.ai-message-list__empty-title,
.ai-message-list__empty-text,
.ai-message__label,
.ai-message__content {
  margin: 0;
}

.ai-message-list__empty-title {
  margin-bottom: var(--space-2);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.ai-message-list__empty-text {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.ai-message {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.ai-message--user {
  align-items: flex-end;
}

.ai-message--assistant {
  align-items: flex-start;
}

.ai-message__label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.ai-message__bubble {
  max-width: min(85%, 440px);
  padding: var(--ai-chat-bubble-padding);
  border-radius: var(--ai-chat-bubble-radius);
  box-shadow: 0 10px 24px var(--color-shadow);
}

.ai-message--user .ai-message__bubble {
  background: var(--ai-chat-user-bubble-bg);
  color: var(--ai-chat-user-bubble-color);
  border-bottom-right-radius: var(--radius-sm);
}

.ai-message--assistant .ai-message__bubble {
  background: var(--ai-chat-assistant-bubble-bg);
  color: var(--ai-chat-assistant-bubble-color);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: var(--radius-sm);
}

.ai-message__content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.65;
}

@media (max-width: 640px) {
  .ai-message__bubble {
    max-width: 92%;
  }
}
</style>
