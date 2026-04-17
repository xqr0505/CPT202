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
        <div class="ai-message__content" v-html="renderMessageContent(message)" />
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
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
const markdownRenderer = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
  typographer: true
})

const defaultLinkOpenRenderer =
  markdownRenderer.renderer.rules.link_open ??
  ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))

markdownRenderer.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  if (token) {
    token.attrSet('target', '_blank')
    token.attrSet('rel', 'noopener noreferrer')
  }

  return defaultLinkOpenRenderer(tokens, idx, options, env, self)
}

const lastMessageSignature = computed<string>(() => {
  const lastMessage = props.messages[props.messages.length - 1]
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

const renderMessageContent = (message: AiChatMessage): string => {
  return markdownRenderer.render(messageContent(message))
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
  border-radius: var(--radius-lg);
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
  word-break: break-word;
  line-height: 1.65;
}

.ai-message__content :deep(p) {
  margin: 0;
}

.ai-message__content :deep(p + p) {
  margin-top: var(--space-3);
}

.ai-message__content :deep(ul),
.ai-message__content :deep(ol) {
  margin: var(--space-2) 0 0;
  padding-left: 1.4em;
}

.ai-message__content :deep(li + li) {
  margin-top: var(--space-1);
}

.ai-message__content :deep(blockquote) {
  margin: var(--space-3) 0 0;
  padding-left: var(--space-3);
  border-left: 3px solid rgba(15, 23, 42, 0.16);
  color: var(--color-text-secondary);
}

.ai-message__content :deep(h1),
.ai-message__content :deep(h2),
.ai-message__content :deep(h3),
.ai-message__content :deep(h4) {
  margin: var(--space-4) 0 var(--space-2);
  font-size: 1em;
  line-height: 1.4;
}

.ai-message__content :deep(hr) {
  margin: var(--space-4) 0;
  border: none;
  border-top: 1px solid rgba(15, 23, 42, 0.1);
}

.ai-message__content :deep(pre) {
  margin: var(--space-2) 0 0;
  padding: var(--space-3);
  overflow-x: auto;
  border-radius: var(--radius-md);
  background: rgba(15, 23, 42, 0.08);
}

.ai-message__content :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
    monospace;
}

.ai-message__content :deep(:not(pre) > code) {
  padding: 0.15em 0.35em;
  border-radius: var(--radius-sm);
  background: rgba(15, 23, 42, 0.08);
}

.ai-message__content :deep(a) {
  color: inherit;
  text-decoration: underline;
}

.ai-message__content :deep(table) {
  width: 100%;
  margin-top: var(--space-3);
  border-collapse: collapse;
  font-size: 14px;
}

.ai-message__content :deep(th),
.ai-message__content :deep(td) {
  padding: var(--space-2);
  border: 1px solid rgba(15, 23, 42, 0.12);
  text-align: left;
  vertical-align: top;
}

.ai-message__content :deep(th) {
  font-weight: 600;
  background: rgba(15, 23, 42, 0.04);
}

@media (max-width: 640px) {
  .ai-message__bubble {
    max-width: 92%;
  }
}
</style>
