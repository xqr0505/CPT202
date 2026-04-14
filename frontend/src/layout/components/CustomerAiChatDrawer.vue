<template>
  <el-drawer
    v-model="drawerVisible"
    :title="AI_DRAWER_TITLE"
    :direction="AI_DRAWER_DIRECTION"
    :size="AI_DRAWER_SIZE"
    :append-to-body="AI_DRAWER_APPEND_TO_BODY"
  >
    <div class="ai-chat-panel">
      <div class="ai-chat-response" role="status" aria-live="polite">
        <p v-if="displayResponse" class="ai-chat-response-text">{{ displayResponse }}</p>
        <p v-else class="ai-chat-empty">{{ AI_CHAT_INPUT_PLACEHOLDER }}</p>
      </div>

      <el-alert
        v-if="aiChatStore.errorMessage"
        :title="aiChatStore.errorMessage"
        type="error"
        :closable="false"
        class="ai-chat-error"
      />

      <div class="ai-chat-input-section">
        <el-input
          :model-value="aiChatStore.inputMessage"
          type="textarea"
          :rows="AI_CHAT_TEXTAREA_ROWS"
          :maxlength="AI_CHAT_TEXTAREA_MAX_LENGTH"
          :placeholder="AI_CHAT_INPUT_PLACEHOLDER"
          resize="none"
          show-word-limit
          @update:model-value="handleInputChange"
          @focus="aiChatStore.resetError"
        />
        <CustomButton
          type="primary"
          :loading="aiChatStore.isLoading"
          @click="aiChatStore.sendMessage"
        >
          {{ AI_CHAT_SEND_BUTTON_TEXT }}
        </CustomButton>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import CustomButton from '@/components/common/CustomButton.vue'
import { useAiChatStore } from '@/stores/aiChat'
import {
  AI_CHAT_INPUT_PLACEHOLDER,
  AI_CHAT_SEND_BUTTON_TEXT,
  AI_CHAT_TEXTAREA_MAX_LENGTH,
  AI_CHAT_TEXTAREA_ROWS,
  AI_CHAT_THINKING_TEXT,
  AI_DRAWER_APPEND_TO_BODY,
  AI_DRAWER_DIRECTION,
  AI_DRAWER_SIZE,
  AI_DRAWER_TITLE
} from '@/constants/ai'

const aiChatStore = useAiChatStore()

const drawerVisible = computed<boolean>({
  get: () => aiChatStore.isDrawerOpen,
  set: (value: boolean) => {
    if (value) {
      aiChatStore.openDrawer()
      return
    }

    aiChatStore.closeDrawer()
  }
})

const displayResponse = computed<string>(() => {
  if (aiChatStore.isLoading && !aiChatStore.answerMessage) {
    return AI_CHAT_THINKING_TEXT
  }

  return aiChatStore.answerMessage
})

const handleInputChange = (value: string): void => {
  aiChatStore.setInput(value)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.ai-chat-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.ai-chat-response {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.ai-chat-response-text,
.ai-chat-empty {
  margin: 0;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  line-height: 1.6;
}

.ai-chat-empty {
  color: var(--color-text-secondary);
}

.ai-chat-error {
  margin-bottom: 0;
}

.ai-chat-input-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

:deep(.el-textarea__inner) {
  background: var(--color-bg-surface);
  border-color: var(--color-border);
  color: var(--color-text-primary);
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary);
}

:deep(.el-input__count) {
  color: var(--color-text-secondary);
  background: var(--color-bg-surface);
}
</style>
