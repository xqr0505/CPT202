<template>
  <el-drawer
    v-model="drawerVisible"
    :size="responsiveSize"
    :direction="responsiveDirection"
    :append-to-body="AI_DRAWER_APPEND_TO_BODY"
    class="ai-chat-drawer"
  >
    <template #header>
      <div class="ai-chat-drawer__header">
        <div class="ai-chat-drawer__title-group">
          <span class="ai-chat-drawer__title">{{ AI_DRAWER_TITLE }}</span>
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
      <div class="ai-chat-drawer__message-area">
        <el-alert
          v-if="aiChatStore.errorMessage"
          :title="aiChatStore.errorMessage"
          type="error"
          :closable="false"
        />

        <AiMessageList :messages="aiChatStore.messages" />
      </div>

      <div class="ai-chat-drawer__footer">
        <AiComposer
          :model-value="aiChatStore.inputMessage"
          :loading="aiChatStore.isLoading"
          @update:model-value="aiChatStore.setInput"
          @focus="aiChatStore.resetError"
          @submit="aiChatStore.sendMessage"
        />
      </div>
    </div>

    <BookingCancelDialog
      v-model="showCancelDialog"
      :booking-id="selectedCancelBookingId"
      @success="handleCancelSuccess"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import CustomButton from '@/components/common/CustomButton.vue'
import BookingCancelDialog from '@/components/business/BookingCancelDialog.vue'
import { useAiChatStore } from '@/stores/aiChat'
import {
  AI_CHAT_CLEAR_BUTTON_TEXT,
  AI_DRAWER_APPEND_TO_BODY,
  AI_DRAWER_DIRECTION,
  AI_DRAWER_SIZE,
  AI_DRAWER_TITLE
} from '@/constants/ai'
import AiComposer from './AiComposer.vue'
import AiMessageList from './AiMessageList.vue'

const aiChatStore = useAiChatStore()
const AI_BOOKING_CANCEL_MODAL_EVENT = 'ai-booking-cancel-modal'

interface AiBookingCancelModalPayload {
  bookingId: number
}

const showCancelDialog = ref(false)
const selectedCancelBookingId = ref<number | null>(null)
const isMobile = ref(window.innerWidth <= 640)

const updateMobileState = () => {
  isMobile.value = window.innerWidth <= 640
}

const responsiveDirection = computed(() => isMobile.value ? 'btt' : AI_DRAWER_DIRECTION)
const responsiveSize = computed(() => isMobile.value ? '85%' : AI_DRAWER_SIZE)

const openCancelModalFromAi = (bookingId: number) => {
  if (!bookingId || bookingId <= 0) {
    return
  }
  selectedCancelBookingId.value = bookingId
  showCancelDialog.value = true
}

const resetCancelDialogState = () => {
  selectedCancelBookingId.value = null
}

const handleCancelSuccess = () => {
  resetCancelDialogState()
}

const onAiBookingCancelModal = (event: Event): void => {
  const customEvent = event as CustomEvent<AiBookingCancelModalPayload>
  const bookingId = Number(customEvent.detail?.bookingId || 0)
  if (!Number.isFinite(bookingId) || bookingId <= 0) {
    return
  }
  openCancelModalFromAi(bookingId)
}

onMounted(() => {
  window.addEventListener('resize', updateMobileState)
  window.addEventListener(AI_BOOKING_CANCEL_MODAL_EVENT, onAiBookingCancelModal as EventListener)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateMobileState)
  window.removeEventListener(AI_BOOKING_CANCEL_MODAL_EVENT, onAiBookingCancelModal as EventListener)
  showCancelDialog.value = false
  resetCancelDialogState()
})

const drawerVisible = computed<boolean>({
  get: () => aiChatStore.isDrawerOpen,
  set: (value: boolean) => {
    if (value) {
      aiChatStore.openDrawer()
      return
    }

    aiChatStore.closeDrawer()
    showCancelDialog.value = false
    resetCancelDialogState()
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

.ai-chat-drawer__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.ai-chat-drawer__message-area {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ai-chat-panel-gap);
  overflow-y: auto;
  padding-bottom: var(--space-4);

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--color-border);
    border-radius: 3px;
  }
}

.ai-chat-drawer__footer {
  flex-shrink: 0;
  position: relative;
  z-index: 10;
  padding-top: var(--space-4);
  background-color: var(--color-bg-surface);
}

:deep(.ai-chat-drawer__message-area .ai-message-list) {
  flex: 1;
  min-height: 0;
}

@media (max-width: 640px) {
  .ai-chat-drawer__header {
    align-items: center;
    flex-direction: row;
  }
}
</style>

<style lang="scss">
.el-drawer.ai-chat-drawer {
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-surface);
  border-radius: 28px 0 0 28px !important;
  box-shadow: none !important;
  border-left: 1px solid var(--color-border);

  &.btt {
    border-radius: 28px 28px 0 0 !important;
    border-left: none;
    border-top: 1px solid var(--color-border);
  }

  .el-drawer__header {
    flex: 0 0 auto;
    margin-bottom: 0;
    padding: var(--space-5) var(--space-6);
    border-bottom: 1px solid var(--color-border-light);

    .el-drawer__close-btn {
      font-size: 20px;
    }
  }

  .el-drawer__body {
    flex: 1;
    min-height: 0;
    padding: var(--space-5);
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}
</style>
