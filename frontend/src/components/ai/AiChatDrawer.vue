<template>
  <el-drawer
    v-model="drawerVisible"
    :size="AI_DRAWER_SIZE"
    :direction="AI_DRAWER_DIRECTION"
    :append-to-body="AI_DRAWER_APPEND_TO_BODY"
    class="ai-chat-drawer"
  >
    <template #header>
      <div class="ai-chat-drawer__header">
        <div class="ai-chat-drawer__title-group">
          <span class="ai-chat-drawer__title">{{ AI_DRAWER_TITLE }}</span>
          <span class="ai-chat-drawer__subtitle">{{ AI_CHAT_EMPTY_STATE_TEXT }}</span>
        </div>
        <CustomButton
          type="default"
          :disabled="aiChatStore.isLoading"
          @click="aiChatStore.clearConversation"
        >
          {{ AI_CHAT_CLEAR_BUTTON_TEXT }}
        </CustomButton>
      </div>
    </template>

    <div class="ai-chat-drawer__body">
      <el-alert
        v-if="aiChatStore.errorMessage"
        :title="aiChatStore.errorMessage"
        type="error"
        :closable="false"
      />

      <AiMessageList :messages="aiChatStore.messages" />
      <AiComposer
        :model-value="aiChatStore.inputMessage"
        :loading="aiChatStore.isLoading"
        @update:model-value="aiChatStore.setInput"
        @focus="aiChatStore.resetError"
        @submit="aiChatStore.sendMessage"
      />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import CustomButton from '@/components/common/CustomButton.vue'
import { useAiChatStore } from '@/stores/aiChat'
import {
  AI_CHAT_CLEAR_BUTTON_TEXT,
  AI_CHAT_EMPTY_STATE_TEXT,
  AI_DRAWER_APPEND_TO_BODY,
  AI_DRAWER_DIRECTION,
  AI_DRAWER_SIZE,
  AI_DRAWER_TITLE
} from '@/constants/ai'
import AiComposer from './AiComposer.vue'
import AiMessageList from './AiMessageList.vue'

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
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.ai-chat-drawer__header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.ai-chat-drawer__title-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.ai-chat-drawer__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.ai-chat-drawer__subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.ai-chat-drawer__body {
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: var(--ai-chat-panel-gap);
}

:deep(.ai-chat-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--ai-chat-toolbar-border-color);
}

:deep(.ai-chat-drawer .el-drawer__body) {
  padding: var(--ai-chat-body-padding);
}

@media (max-width: 640px) {
  .ai-chat-drawer__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
