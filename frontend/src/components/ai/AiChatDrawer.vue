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

        <div v-if="aiChatStore.messages.length === 0" class="ai-chat-drawer__welcome">
          <div class="ai-chat-drawer__welcome-text">
            <h3 class="welcome-title">Welcome! I'm your ExpertLink AI Assistant.</h3>
            <p class="welcome-desc">I can answer questions about the platform and help you book, reschedule, or cancel consultations.</p>
            <p class="welcome-hint">Try asking me:</p>
          </div>
          <div class="ai-chat-drawer__welcome-prompts">
            <el-button
              v-for="(prompt, index) in SUGGESTED_PROMPTS"
              :key="index"
              class="ai-chat-drawer__prompt-btn"
              @click="handlePromptClick(prompt)"
            >
              {{ prompt }}
            </el-button>
          </div>
        </div>

        <AiMessageList v-else :messages="aiChatStore.messages" />
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
    <BookingRescheduleDialog
      v-model="showRescheduleDialog"
      :booking="selectedRescheduleBooking"
      :booking-id="selectedRescheduleBookingId"
      :prefill-date="selectedRescheduleDate"
      :prefill-slot-id="selectedRescheduleSlotId"
      @success="handleRescheduleSuccess"
    />

    <el-dialog
      v-model="showBookingDialog"
      title="Confirm Booking"
      width="520px"
      :close-on-click-modal="false"
      append-to-body
    >
      <div v-if="selectedBookingPreview" class="ai-booking-dialog__content">
        <div class="ai-booking-dialog__row">
          <span>Specialist</span>
          <strong>{{ selectedBookingPreview.specialistName || `#${selectedBookingPreview.specialistId}` }}</strong>
        </div>
        <div class="ai-booking-dialog__row">
          <span>Time</span>
          <strong>{{ selectedBookingPreview.slotDate }} {{ selectedBookingPreview.startTime }} - {{ selectedBookingPreview.endTime }}</strong>
        </div>
        <div class="ai-booking-dialog__row">
          <span>Price</span>
          <strong>{{ formatFee(selectedBookingPreview.consultationFee) }}</strong>
        </div>

        <el-form label-position="top" class="ai-booking-dialog__form">
          <el-form-item label="Topic" required>
            <el-select
              v-model="selectedBookingTopic"
              placeholder="Select a topic"
              :disabled="bookingSubmitting"
            >
              <el-option
                v-for="topic in bookingTopicOptions"
                :key="topic"
                :label="topic"
                :value="topic"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Notes">
            <el-input
              v-model="selectedBookingNotes"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="Optional details you want the specialist to know"
              :disabled="bookingSubmitting"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="ai-booking-dialog__footer">
          <CustomButton :disabled="bookingSubmitting" @click="closeBookingDialog">Cancel</CustomButton>
          <CustomButton type="primary" :disabled="bookingSubmitting" @click="submitBookingFromDialog">
            {{ bookingSubmitting ? 'Submitting...' : 'Confirm booking' }}
          </CustomButton>
        </div>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CustomButton from '@/components/common/CustomButton.vue'
import BookingCancelDialog from '@/components/business/BookingCancelDialog.vue'
import BookingRescheduleDialog from '@/components/business/BookingRescheduleDialog.vue'
import { useAiChatStore } from '@/stores/aiChat'
import {
  AI_CHAT_CLEAR_BUTTON_TEXT,
  AI_DRAWER_APPEND_TO_BODY,
  AI_DRAWER_DIRECTION,
  AI_DRAWER_SIZE,
  AI_DRAWER_TITLE
} from '@/constants/ai'
import { createBooking, getBookingDetail, getBookingTopics, type BookingListItem } from '@/api/booking'
import AiComposer from './AiComposer.vue'
import AiMessageList from './AiMessageList.vue'

const aiChatStore = useAiChatStore()
const router = useRouter()
const AI_BOOKING_CANCEL_MODAL_EVENT = 'ai-booking-cancel-modal'
const AI_BOOKING_RESCHEDULE_MODAL_EVENT = 'ai-booking-reschedule-modal'
const AI_BOOKING_SUBMIT_PREVIEW_EVENT = 'ai-booking-submit-preview'

interface AiBookingCancelModalPayload {
  bookingId: number
}

interface AiBookingRescheduleModalPayload {
  bookingId: number
  targetDate?: string | null
  suggestedSlotId?: number | null
}

interface AiBookingSubmitPreviewPayload {
  specialistId: number
  slotId: number
  slotDate: string
  startTime: string
  endTime: string
  specialistName?: string | null
  consultationFee?: number | null
  topic: string
  customerNotes?: string | null
  availableTopics?: string[]
}

const showCancelDialog = ref(false)
const selectedCancelBookingId = ref<number | null>(null)
const showRescheduleDialog = ref(false)
const selectedRescheduleBooking = ref<BookingListItem | null>(null)
const selectedRescheduleBookingId = ref<number | null>(null)
const selectedRescheduleDate = ref('')
const selectedRescheduleSlotId = ref<number | null>(null)
const showBookingDialog = ref(false)
const selectedBookingPreview = ref<AiBookingSubmitPreviewPayload | null>(null)
const selectedBookingTopic = ref('')
const selectedBookingNotes = ref('')
const bookingTopicOptions = ref<string[]>([])
const bookingSubmitting = ref(false)
const isMobile = ref(window.innerWidth <= 640)

const SUGGESTED_PROMPTS = [
  "What is the platform's rescheduling policy?",
  "Help me check my scheduled consultation from last month.",
  "Show me doctors available tomorrow."
]

const handlePromptClick = (prompt: string) => {
  aiChatStore.setInput(prompt)
  void aiChatStore.sendMessage()
}

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

const resetRescheduleDialogState = () => {
  selectedRescheduleBooking.value = null
  selectedRescheduleBookingId.value = null
  selectedRescheduleDate.value = ''
  selectedRescheduleSlotId.value = null
}

const resetBookingDialogState = () => {
  selectedBookingPreview.value = null
  selectedBookingTopic.value = ''
  selectedBookingNotes.value = ''
  bookingTopicOptions.value = []
  bookingSubmitting.value = false
}

const handleRescheduleSuccess = () => {
  resetRescheduleDialogState()
}

const closeBookingDialog = () => {
  showBookingDialog.value = false
  resetBookingDialogState()
}

const onAiBookingCancelModal = (event: Event): void => {
  const customEvent = event as CustomEvent<AiBookingCancelModalPayload>
  const bookingId = Number(customEvent.detail?.bookingId || 0)
  if (!Number.isFinite(bookingId) || bookingId <= 0) {
    return
  }
  openCancelModalFromAi(bookingId)
}

const mapBookingDetailToListItem = (detail: Awaited<ReturnType<typeof getBookingDetail>>): BookingListItem => ({
  id: String(detail.bookingId),
  specialistId: String(detail.specialistId),
  specialistName: detail.specialistName,
  specialistAvatar: detail.specialistAvatar,
  appointmentDateTime: `${detail.slotDate} ${detail.startTime}`,
  serviceName: detail.topic || 'Consultation',
  status: String(detail.status || ''),
  amount: Number(detail.price ?? 0)
})

const openRescheduleModalFromAi = async (payload: AiBookingRescheduleModalPayload) => {
  if (!payload.bookingId || payload.bookingId <= 0) {
    return
  }
  const detail = await getBookingDetail(payload.bookingId)
  selectedRescheduleBooking.value = mapBookingDetailToListItem(detail)
  selectedRescheduleBookingId.value = payload.bookingId
  selectedRescheduleDate.value = payload.targetDate || ''
  selectedRescheduleSlotId.value = payload.suggestedSlotId || null
  showRescheduleDialog.value = true
}

const onAiBookingRescheduleModal = (event: Event): void => {
  const customEvent = event as CustomEvent<AiBookingRescheduleModalPayload>
  const bookingId = Number(customEvent.detail?.bookingId || 0)
  if (!Number.isFinite(bookingId) || bookingId <= 0) {
    return
  }
  void openRescheduleModalFromAi({
    bookingId,
    targetDate: customEvent.detail?.targetDate || null,
    suggestedSlotId: customEvent.detail?.suggestedSlotId || null
  })
}

const formatFee = (value?: number | null): string => {
  if (value === null || value === undefined) {
    return 'N/A'
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return 'N/A'
  }
  return `CNY ${parsed.toFixed(2)}`
}

const openBookingDialogFromAi = async (payload: AiBookingSubmitPreviewPayload) => {
  let topics: string[] = []
  try {
    topics = await getBookingTopics()
  } catch {
    topics = []
  }
  const normalizedTopics = Array.isArray(topics)
    ? topics.filter(topic => typeof topic === 'string').map(topic => topic.trim()).filter(Boolean)
    : []
  const mergedTopics = Array.from(new Set([
    ...normalizedTopics,
    ...(payload.availableTopics || []).filter(topic => typeof topic === 'string' && topic.trim()).map(topic => topic.trim()),
  ]))

  selectedBookingPreview.value = payload
  bookingTopicOptions.value = mergedTopics
  selectedBookingTopic.value = mergedTopics.includes(payload.topic)
    ? payload.topic
    : (mergedTopics[0] || payload.topic || '')
  selectedBookingNotes.value = payload.customerNotes || ''
  showBookingDialog.value = true
}

const onAiBookingSubmitPreview = (event: Event): void => {
  const customEvent = event as CustomEvent<AiBookingSubmitPreviewPayload>
  const payload = customEvent.detail
  if (
    !payload ||
    !Number.isFinite(Number(payload.specialistId)) ||
    !Number.isFinite(Number(payload.slotId)) ||
    Number(payload.specialistId) <= 0 ||
    Number(payload.slotId) <= 0
  ) {
    return
  }
  void openBookingDialogFromAi(payload).catch(() => {
    ElMessage.error('Failed to open booking dialog from AI preview.')
  })
}

const submitBookingFromDialog = async () => {
  if (bookingSubmitting.value || !selectedBookingPreview.value) {
    return
  }
  const topic = selectedBookingTopic.value.trim()
  if (!topic) {
    ElMessage.warning('Please choose a booking topic.')
    return
  }

  bookingSubmitting.value = true
  try {
    const createdBooking = await createBooking({
      specialistId: selectedBookingPreview.value.specialistId,
      slotId: selectedBookingPreview.value.slotId,
      topic,
      customerNotes: selectedBookingNotes.value.trim(),
    }, true)
    closeBookingDialog()
    aiChatStore.closeDrawer()
    ElMessage.success(`Booking created successfully. Status: ${createdBooking.status}.`)
    await router.push('/customer/bookings')
  } catch (error: any) {
    ElMessage.error(error?.message || 'Failed to create booking.')
  } finally {
    bookingSubmitting.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', updateMobileState)
  window.addEventListener(AI_BOOKING_CANCEL_MODAL_EVENT, onAiBookingCancelModal as EventListener)
  window.addEventListener(AI_BOOKING_RESCHEDULE_MODAL_EVENT, onAiBookingRescheduleModal as EventListener)
  window.addEventListener(AI_BOOKING_SUBMIT_PREVIEW_EVENT, onAiBookingSubmitPreview as EventListener)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateMobileState)
  window.removeEventListener(AI_BOOKING_CANCEL_MODAL_EVENT, onAiBookingCancelModal as EventListener)
  window.removeEventListener(AI_BOOKING_RESCHEDULE_MODAL_EVENT, onAiBookingRescheduleModal as EventListener)
  window.removeEventListener(AI_BOOKING_SUBMIT_PREVIEW_EVENT, onAiBookingSubmitPreview as EventListener)
  showCancelDialog.value = false
  showRescheduleDialog.value = false
  showBookingDialog.value = false
  resetCancelDialogState()
  resetRescheduleDialogState()
  resetBookingDialogState()
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
    showRescheduleDialog.value = false
    showBookingDialog.value = false
    resetCancelDialogState()
    resetRescheduleDialogState()
    resetBookingDialogState()
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

.ai-chat-drawer__welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: var(--space-6) var(--space-4);
  text-align: center;
}

.ai-chat-drawer__welcome-text {
  text-align: center;
  margin-bottom: var(--space-6);

  .welcome-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--color-primary);
    margin: 0 0 var(--space-3) 0;
  }

  .welcome-desc {
    font-size: 14px;
    line-height: 1.6;
    color: var(--color-text-regular);
    margin: 0 0 var(--space-4) 0;
  }

  .welcome-hint {
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-secondary);
    margin: 0;
  }
}

.ai-chat-drawer__welcome-prompts {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  max-width: 400px;
}

.ai-chat-drawer__prompt-btn {
  width: 100%;
  margin: 0 !important; /* Fix Element Plus sibling button left margin */
  justify-content: flex-start;
  text-align: left;
  white-space: normal;
  height: auto;
  padding: var(--space-3);
  line-height: 1.4;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background-color: var(--color-background-soft);
  color: var(--color-text-primary);
  transition: all 0.2s ease;

  &:hover {
    background-color: var(--color-primary-light);
    border-color: var(--color-primary);
    color: var(--color-primary);
  }
}

.ai-chat-drawer__footer {
  flex-shrink: 0;
  position: relative;
  z-index: 10;
  padding-top: var(--space-4);
  background-color: var(--color-bg-surface);
}

.ai-booking-dialog__content {
  display: grid;
  gap: var(--space-3);
}

.ai-booking-dialog__row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);

  span {
    color: var(--color-text-secondary);
  }

  strong {
    color: var(--color-text-primary);
    text-align: right;
  }
}

.ai-booking-dialog__form {
  margin-top: var(--space-2);
}

.ai-booking-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
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
