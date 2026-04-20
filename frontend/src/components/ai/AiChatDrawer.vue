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

    <el-dialog
      v-model="bookingConfirmDialogVisible"
      title="Confirm Booking"
      width="520px"
      :close-on-click-modal="false"
      :z-index="3500"
      append-to-body
    >
      <div v-if="bookingPreview" class="booking-confirm-content">
        <div class="booking-confirm-row">
          <span>Specialist</span>
          <strong>{{ bookingPreview.specialistName || `#${bookingPreview.specialistId}` }}</strong>
        </div>
        <div class="booking-confirm-row">
          <span>Time</span>
          <strong>{{ bookingPreview.slotDate }} {{ bookingPreview.startTime }} - {{ bookingPreview.endTime }}</strong>
        </div>
        <div class="booking-confirm-row">
          <span>Price</span>
          <strong>{{ formatFee(bookingPreview.consultationFee) }}</strong>
        </div>
        <div class="booking-confirm-row">
          <span>Topic</span>
          <strong>{{ bookingPreview.topic }}</strong>
        </div>
        <div class="booking-confirm-row">
          <span>Notes</span>
          <strong>{{ bookingPreview.customerNotes || 'No notes provided' }}</strong>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <CustomButton :disabled="bookingSubmitting" @click="dismissBookingPreview">
            Cancel
          </CustomButton>
          <CustomButton type="primary" :loading="bookingSubmitting" @click="confirmBookingFromPreview">
            Confirm booking
          </CustomButton>
        </span>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CustomButton from '@/components/common/CustomButton.vue'
import { createBooking } from '@/api/booking'
import { getUser } from '@/api/request'
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
const router = useRouter()
const AI_BOOKING_SUBMIT_PREVIEW_EVENT = 'ai-booking-submit-preview'
const AI_BOOKING_CONTEXT_STORAGE_KEY = 'ai.booking.context'

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
  warnings?: string[]
}

interface StoredSessionUser {
  userId?: number | string | null
  id?: number | string | null
}

interface AiBookingContextSlot {
  id?: number
  slotDate?: string
  startTime?: string
  endTime?: string
  status?: string
}

interface AiBookingContextSpecialist {
  id?: number
  name?: string
  consultationFee?: number
}

interface AiBookingPageContext {
  specialistId?: number
  specialistName?: string
  consultationFee?: number
  selectedDate?: string
  selectedSlotId?: number
  selectedSlotStartTime?: string
  selectedSlotEndTime?: string
  availableSlots?: AiBookingContextSlot[]
  visibleSpecialists?: AiBookingContextSpecialist[]
  selectedTopic?: string
  selectedCustomerNotes?: string
}

const bookingSubmitting = ref(false)
const bookingPreview = ref<AiBookingSubmitPreviewPayload | null>(null)
const bookingConfirmDialogVisible = ref(false)
const lastPreviewKey = ref('')

const isMobile = ref(window.innerWidth <= 640)

const updateMobileState = () => {
  isMobile.value = window.innerWidth <= 640
}

const responsiveDirection = computed(() => isMobile.value ? 'btt' : AI_DRAWER_DIRECTION)
const responsiveSize = computed(() => isMobile.value ? '85%' : AI_DRAWER_SIZE)

const normalizeString = (value: unknown): string | null => {
  if (typeof value !== 'string') {
    return null
  }
  const trimmed = value.trim()
  return trimmed || null
}

const normalizeNumericId = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return null
  }
  return Math.trunc(parsed)
}

const normalizeTime = (value: string | null): string | null => {
  if (!value) {
    return null
  }
  const trimmed = value.trim()
  const hhmm = trimmed.match(/^(\d{1,2}):(\d{2})$/)
  if (hhmm) {
    const hour = Number(hhmm[1])
    const minute = Number(hhmm[2])
    if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
      return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:00`
    }
  }
  const hhmmss = trimmed.match(/^(\d{1,2}):(\d{2}):(\d{2})$/)
  if (hhmmss) {
    const hour = Number(hhmmss[1])
    const minute = Number(hhmmss[2])
    const second = Number(hhmmss[3])
    if (
      hour >= 0 && hour <= 23 &&
      minute >= 0 && minute <= 59 &&
      second >= 0 && second <= 59
    ) {
      return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
    }
  }
  return null
}

const normalizeDate = (value: string | null): string | null => {
  if (!value) {
    return null
  }
  const trimmed = value.trim()
  const dateMatch = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!dateMatch) {
    return null
  }
  return `${dateMatch[1]}-${dateMatch[2]}-${dateMatch[3]}`
}

const normalizeStatus = (value: unknown): string => {
  if (typeof value !== 'string') {
    return ''
  }
  return value.trim().toUpperCase()
}

const normalizeDoctorName = (value: string | null): string => {
  if (!value) {
    return ''
  }
  return value
    .replace(/^doctor\s+/i, '')
    .replace(/^dr\.?\s*/i, '')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase()
}

const resolveSpecialistIdByName = (pageContext: AiBookingPageContext | null, specialistName: string | null): number | null => {
  const normalizedTargetName = normalizeDoctorName(specialistName)
  const visibleSpecialists = pageContext?.visibleSpecialists
  if (!normalizedTargetName || !Array.isArray(visibleSpecialists) || visibleSpecialists.length === 0) {
    return null
  }

  const normalizedItems = visibleSpecialists
    .map(item => ({
      id: normalizeNumericId(item.id),
      normalizedName: normalizeDoctorName(normalizeString(item.name)),
    }))
    .filter(item => Boolean(item.id) && Boolean(item.normalizedName))

  const exact = normalizedItems.find(item => item.normalizedName === normalizedTargetName)
  if (exact?.id) {
    return exact.id
  }

  const fuzzy = normalizedItems.find(item =>
    item.normalizedName.includes(normalizedTargetName) ||
    normalizedTargetName.includes(item.normalizedName)
  )
  return fuzzy?.id || null
}

const extractSpecialistNameFromContent = (content: string): string | null => {
  const fromLabel = normalizeLineValue(extractFirstMatch(content, [
    /(?:\*\*|__)?\s*specialistName\s*(?:\*\*|__)?\s*[:\uFF1A=]\s*([^\n\r]+)/i,
    /(?:\*\*|__)?\s*(?:\u533b\u751f|\u4e13\u5bb6|specialist|doctor)\s*(?:\*\*|__)?\s*[:\uFF1A=]\s*([^\n\r]+)/i,
  ]))
  if (fromLabel) {
    return fromLabel
  }

  const drMatched = content.match(/\bDr\.?\s+[A-Za-z][A-Za-z\s'.-]{1,80}/)
  if (drMatched?.[0]) {
    return normalizeLineValue(drMatched[0])
  }

  return null
}

const resolveCurrentPageSpecialistId = (): number | null => {
  if (typeof window === 'undefined') {
    return null
  }
  const matched = (window.location.pathname || '').match(/^\/customer\/specialists\/(\d+)\/book$/)
  if (!matched?.[1]) {
    return null
  }
  return normalizeNumericId(matched[1])
}

const resolveSlotIdByContext = (
  pageContext: AiBookingPageContext | null,
  slotDate: string | null,
  startTime: string | null,
  endTime: string | null
): number | null => {
  const contextSlots = pageContext?.availableSlots
  if (!Array.isArray(contextSlots) || contextSlots.length === 0) {
    return null
  }

  const targetDate = normalizeDate(slotDate) || normalizeDate(normalizeString(pageContext?.selectedDate))
  const targetStartTime = normalizeTime(startTime)
  const targetEndTime = normalizeTime(endTime)

  const normalizedSlots = contextSlots
    .map(slot => ({
      id: normalizeNumericId(slot.id),
      slotDate: normalizeDate(normalizeString(slot.slotDate)),
      startTime: normalizeTime(normalizeString(slot.startTime)),
      endTime: normalizeTime(normalizeString(slot.endTime)),
      status: normalizeStatus(slot.status),
    }))
    .filter(slot => Boolean(slot.id))

  const candidates = normalizedSlots.filter(slot => {
    if (targetDate && slot.slotDate !== targetDate) {
      return false
    }
    if (targetStartTime && slot.startTime !== targetStartTime) {
      return false
    }
    if (targetEndTime && slot.endTime !== targetEndTime) {
      return false
    }
    return true
  })

  if (!candidates.length) {
    return null
  }

  const available = candidates.find(slot => slot.status === 'AVAILABLE')
  return available?.id || candidates[0]?.id || null
}

const isSlotAvailableInContext = (pageContext: AiBookingPageContext | null, slotId: number): boolean | null => {
  const contextSlots = pageContext?.availableSlots
  if (!Array.isArray(contextSlots) || contextSlots.length === 0) {
    return null
  }
  const matched = contextSlots.find(slot => normalizeNumericId(slot.id) === slotId)
  if (!matched) {
    return null
  }
  return normalizeStatus(matched.status) === 'AVAILABLE'
}

const normalizeLineValue = (value: string | null): string | null => {
  if (!value) {
    return null
  }
  const sanitized = value
    .replace(/^[\u2022\-*\d.)\s]+/, '')
    .replace(/[\uFF08(][^)\uFF09]*?(specialistId|slotId)\s*[:=\uFF1A\uFF1D].*$/i, '')
    .trim()
  return sanitized || null
}

const extractFirstMatch = (content: string, patterns: RegExp[]): string | null => {
  for (const pattern of patterns) {
    const matched = content.match(pattern)
    if (matched?.[1]) {
      return matched[1].trim()
    }
  }
  return null
}

const extractTimeRangeFromContent = (content: string): [string | null, string | null] => {
  const lineCandidate = extractFirstMatch(content, [
    /(?:\u65f6\u6bb5|time(?:\s*slot)?|slot)\s*[:=\uFF1A\uFF1D?-]?\s*([^\n\r]+)/i,
  ])
  const segment = lineCandidate || content
  const segmentTimes = segment.match(/\d{1,2}:\d{2}(?::\d{2})?/g)
  if (segmentTimes && segmentTimes.length >= 2) {
    return [segmentTimes[0] ?? null, segmentTimes[1] ?? null]
  }

  const allTimes = content.match(/\d{1,2}:\d{2}(?::\d{2})?/g)
  if (allTimes && allTimes.length >= 2) {
    return [allTimes[0] ?? null, allTimes[1] ?? null]
  }

  return [null, null]
}

const normalizeNumber = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const resolveCurrentUserId = (): number | null => {
  const storedUser = getUser() as StoredSessionUser | null
  const rawUserId = storedUser?.userId ?? storedUser?.id
  const parsedUserId = Number(rawUserId)
  if (!Number.isFinite(parsedUserId) || parsedUserId <= 0) {
    return null
  }
  return Math.trunc(parsedUserId)
}

const resolveAiBookingContextStorageKey = (): string => {
  const currentUserId = resolveCurrentUserId()
  return currentUserId
    ? `${AI_BOOKING_CONTEXT_STORAGE_KEY}:${currentUserId}`
    : AI_BOOKING_CONTEXT_STORAGE_KEY
}

const readAiBookingPageContext = (): AiBookingPageContext | null => {
  if (typeof window === 'undefined') {
    return null
  }
  try {
    const scopedStorageKey = resolveAiBookingContextStorageKey()
    let raw = window.sessionStorage.getItem(scopedStorageKey)
    if (!raw && scopedStorageKey !== AI_BOOKING_CONTEXT_STORAGE_KEY) {
      raw = window.sessionStorage.getItem(AI_BOOKING_CONTEXT_STORAGE_KEY)
    }
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') {
      return null
    }
    return parsed as AiBookingPageContext
  } catch {
    return null
  }
}

const getParsedValueByAliases = (parsed: Record<string, unknown>, aliases: string[]): unknown => {
  for (const alias of aliases) {
    if (alias in parsed) {
      return parsed[alias]
    }
  }
  return undefined
}

const parseJsonLikeContent = (content: string): Record<string, unknown> | null => {
  const trimmed = content.trim()
  if (!trimmed) {
    return null
  }

  const codeBlockMatch = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/i)
  const candidate = codeBlockMatch?.[1]?.trim() || trimmed

  const tryParse = (raw: string): Record<string, unknown> | null => {
    try {
      const parsed = JSON.parse(raw)
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
        ? (parsed as Record<string, unknown>)
        : null
    } catch {
      return null
    }
  }

  const parsedCandidate = tryParse(candidate)
  if (parsedCandidate) {
    return parsedCandidate
  }

  const start = candidate.indexOf('{')
  const end = candidate.lastIndexOf('}')
  if (start < 0 || end <= start) {
    return null
  }

  return tryParse(candidate.slice(start, end + 1))
}

const parsePreviewFromAssistantMessage = (content: string): AiBookingSubmitPreviewPayload | null => {
  const pageContext = readAiBookingPageContext()

  const parsed = parseJsonLikeContent(content)
  if (parsed) {
    const specialistIdFromJsonRaw = normalizeNumericId(
      getParsedValueByAliases(parsed, ['specialistId', 'specialist_id', '\u533b\u751fID', '\u4e13\u5bb6ID'])
    )
    const slotIdFromJsonRaw = normalizeNumericId(
      getParsedValueByAliases(parsed, ['slotId', 'slot_id', '\u65f6\u6bb5ID'])
    )
    const topicFromJson = normalizeString(
      getParsedValueByAliases(parsed, ['topic', '\u4e3b\u9898'])
    )
    const slotDateFromJson = normalizeString(
      getParsedValueByAliases(parsed, ['slotDate', 'slot_date', '\u65e5\u671f'])
    )
    const startTimeFromJson = normalizeTime(normalizeString(
      getParsedValueByAliases(parsed, ['startTime', 'start_time', '\u5f00\u59cb\u65f6\u95f4'])
    ))
    const endTimeFromJson = normalizeTime(normalizeString(
      getParsedValueByAliases(parsed, ['endTime', 'end_time', '\u7ed3\u675f\u65f6\u95f4'])
    ))
    const specialistNameFromJson = normalizeString(
      getParsedValueByAliases(parsed, ['specialistName', 'specialist_name', '\u533b\u751f', '\u4e13\u5bb6'])
    )
    const specialistNameFromText = extractSpecialistNameFromContent(content)
    const resolvedSpecialistName = specialistNameFromJson || specialistNameFromText
    const customerNotesFromJson = normalizeString(
      getParsedValueByAliases(parsed, ['customerNotes', 'customer_notes', 'notes', '\u5907\u6ce8'])
    )
    const consultationFeeFromJson = normalizeNumber(
      getParsedValueByAliases(parsed, ['consultationFee', 'consultation_fee', 'fee', 'price', '\u54a8\u8be2\u8d39'])
    )

    const resolvedSlotDateFromJson = slotDateFromJson || normalizeString(pageContext?.selectedDate)
    const resolvedStartTimeFromJson =
      startTimeFromJson || normalizeTime(normalizeString(pageContext?.selectedSlotStartTime))
    const resolvedEndTimeFromJson =
      endTimeFromJson || normalizeTime(normalizeString(pageContext?.selectedSlotEndTime))

    const specialistIdFromJson =
      specialistIdFromJsonRaw ||
      resolveCurrentPageSpecialistId() ||
      resolveSpecialistIdByName(pageContext, resolvedSpecialistName) ||
      normalizeNumericId(pageContext?.specialistId)
    const slotIdFromJson =
      slotIdFromJsonRaw ||
      resolveSlotIdByContext(pageContext, resolvedSlotDateFromJson, resolvedStartTimeFromJson, resolvedEndTimeFromJson) ||
      normalizeNumericId(pageContext?.selectedSlotId)

    if (specialistIdFromJson && slotIdFromJson) {
      return {
        specialistId: specialistIdFromJson,
        slotId: slotIdFromJson,
        slotDate: resolvedSlotDateFromJson || 'N/A',
        startTime: resolvedStartTimeFromJson || '--:--:--',
        endTime: resolvedEndTimeFromJson || '--:--:--',
        specialistName: resolvedSpecialistName || normalizeString(pageContext?.specialistName),
        consultationFee: consultationFeeFromJson ?? normalizeNumber(pageContext?.consultationFee),
        topic: topicFromJson || normalizeString(pageContext?.selectedTopic) || '',
        customerNotes: customerNotesFromJson || normalizeString(pageContext?.selectedCustomerNotes),
      }
    }
  }

  const specialistIdRaw = normalizeNumericId(
    extractFirstMatch(content, [
      /specialistId\s*[:=\uFF1A\uFF1D]\s*(\d+)/i,
      /specialistId\s*[^\d\n\r]{0,8}(\d+)/i,
      /specialist\s*id\s*[:=\uFF1A\uFF1D]?\s*(\d+)/i,
      /specialist\s+id\s*[:=\uFF1A\uFF1D]\s*(\d+)/i,
      /(?:\u533b\u751f|\u4e13\u5bb6)\s*id\s*[:=\uFF1A\uFF1D]\s*(\d+)/i,
    ])
  )
  const slotIdRaw = normalizeNumericId(
    extractFirstMatch(content, [
      /slotId\s*[:=\uFF1A\uFF1D]\s*(\d+)/i,
      /slotId\s*[^\d\n\r]{0,8}(\d+)/i,
      /slot\s*id\s*[:=\uFF1A\uFF1D]?\s*(\d+)/i,
      /slot\s+id\s*[:=\uFF1A\uFF1D]\s*(\d+)/i,
      /(?:\u65f6\u6bb5|time\s*slot)\s*id\s*[:=\uFF1A\uFF1D?]?\s*(\d+)/i,
      /(?:slot\s*id|\u65f6\u6bb5\s*id|\u65f6\u6bb5id)\s*[^\d\n\r]{0,8}(\d+)/i,
    ])
  )
  const slotDate = normalizeString(
    extractFirstMatch(content, [
      /slotDate\s*[:=\uFF1A\uFF1D]\s*(\d{4}-\d{2}-\d{2})/i,
      /(\d{4}-\d{2}-\d{2})/,
    ])
  )

  const startTimeDirect = extractFirstMatch(content, [
    /startTime\s*[:=\uFF1A\uFF1D]\s*(\d{1,2}:\d{2}(?::\d{2})?)/i,
  ])
  const endTimeDirect = extractFirstMatch(content, [
    /endTime\s*[:=\uFF1A\uFF1D]\s*(\d{1,2}:\d{2}(?::\d{2})?)/i,
  ])
  const [rangeStartTime, rangeEndTime] = extractTimeRangeFromContent(content)
  const startTime = normalizeTime(startTimeDirect || rangeStartTime || null)
  const endTime = normalizeTime(endTimeDirect || rangeEndTime || null)

  const topic = normalizeLineValue(extractFirstMatch(content, [
    /(?:\*\*|__)?\s*topic\s*(?:\*\*|__)?\s*[:\uFF1A=]\s*([^\n\r]+)/i,
    /(?:\*\*|__)?\s*customer\s*topic\s*(?:\*\*|__)?\s*[:\uFF1A=]\s*([^\n\r]+)/i,
    /(?:\*\*|__)?\s*\u4e3b\u9898\s*(?:\*\*|__)?\s*[:\uFF1A=]\s*([^\n\r]+)/,
    /(?:\*\*|__)?\s*(?:\u4e3b\u9898|topic)\s*[^\n\r]{0,4}\s*([^\n\r]+)/i,
  ]))
  const customerNotes = normalizeLineValue(extractFirstMatch(content, [
    /(?:\*\*|__)?\s*customerNotes\s*(?:\*\*|__)?\s*[:\uFF1A=]\s*([^\n\r]+)/i,
    /(?:\*\*|__)?\s*notes?\s*(?:\*\*|__)?\s*[:\uFF1A=]\s*([^\n\r]+)/i,
    /(?:\*\*|__)?\s*\u5907\u6ce8\s*(?:\*\*|__)?\s*[:\uFF1A=]\s*([^\n\r]+)/,
    /(?:\*\*|__)?\s*(?:\u5907\u6ce8|notes?|customerNotes)\s*[^\n\r]{0,4}\s*([^\n\r]+)/i,
  ]))
  const specialistName = extractSpecialistNameFromContent(content)
  const consultationFee = normalizeNumber(extractFirstMatch(content, [
    /consultationFee\s*[:=\uFF1A\uFF1D]\s*([0-9]+(?:\.[0-9]+)?)/i,
    /(?:\*\*|__)?\s*(?:consultation\s*fee|fee|price)\s*(?:\*\*|__)?\s*[:\uFF1A=]?\s*[^\d\n\r]*([0-9]+(?:\.[0-9]+)?)/i,
    /(?:\*\*|__)?\s*\u54a8\u8be2\u8d39\s*(?:\*\*|__)?\s*[:\uFF1A=]?\s*[^\d\n\r]*([0-9]+(?:\.[0-9]+)?)/,
    /(?:\u8d39\u7528|\u54a8\u8be2\u8d39|price|fee|consultation\s*fee)\s*[^\d\n\r]{0,6}([0-9]+(?:\.[0-9]+)?)/i,
  ]))

  const resolvedSlotDate = slotDate || normalizeString(pageContext?.selectedDate)
  const resolvedStartTime =
    startTime || normalizeTime(normalizeString(pageContext?.selectedSlotStartTime))
  const resolvedEndTime =
    endTime || normalizeTime(normalizeString(pageContext?.selectedSlotEndTime))
  const specialistId =
    specialistIdRaw ||
    resolveCurrentPageSpecialistId() ||
    resolveSpecialistIdByName(pageContext, specialistName) ||
    normalizeNumericId(pageContext?.specialistId)
  const slotId =
    slotIdRaw ||
    resolveSlotIdByContext(pageContext, resolvedSlotDate, resolvedStartTime, resolvedEndTime) ||
    normalizeNumericId(pageContext?.selectedSlotId)
  if (!specialistId || !slotId) {
    return null
  }

  return {
    specialistId,
    slotId,
    slotDate: resolvedSlotDate || 'N/A',
    startTime: resolvedStartTime || '--:--:--',
    endTime: resolvedEndTime || '--:--:--',
    specialistName: specialistName || normalizeString(pageContext?.specialistName),
    consultationFee: consultationFee ?? normalizeNumber(pageContext?.consultationFee),
    topic: topic || normalizeString(pageContext?.selectedTopic) || '',
    customerNotes: customerNotes || normalizeString(pageContext?.selectedCustomerNotes),
  }
}

const buildPreviewKey = (preview: AiBookingSubmitPreviewPayload): string => {
  return [
    preview.specialistId,
    preview.slotId,
    preview.slotDate,
    preview.startTime,
    preview.endTime,
    preview.topic,
    preview.customerNotes || '',
  ].join('|')
}

const BOOKING_CONFLICT_PATTERN = /("success"\s*:\s*false|"readyToSubmit"\s*:\s*false|readyToSubmit\s*[:=\uFF1A\uFF1D]\s*false|not\s+available|already\s+booked|booking\s+conflict|requested\s+slot\s+is\s+not\s+available|time\s+slot\s+is\s+no\s+longer\s+available|please\s+choose\s+another|failed\s+to\s+create\s+booking|failed\s+to\s+submit\s+booking|\u9884\u7ea6\u51b2\u7a81|\u5df2\u88ab\u9884\u7ea6|\u5df2\u88ab\u5360\u7528|\u4e0d\u53ef\u7528|\u9884\u7ea6\u5931\u8d25|\u65f6\u6bb5\u4e0d\u53ef\u7528|\u8bf7\u9009\u62e9\u5176\u4ed6\u65f6\u6bb5)/i

const isBookingConflictContent = (content: string): boolean => {
  return BOOKING_CONFLICT_PATTERN.test(content || '')
}

const openBookingPreview = (preview: AiBookingSubmitPreviewPayload): void => {
  const latestAssistant = aiChatStore.messages
    .slice()
    .reverse()
    .find(message => message.role === 'assistant' && message.content?.trim())
  if (latestAssistant?.content && isBookingConflictContent(latestAssistant.content)) {
    return
  }
  const fallback = latestAssistant ? parsePreviewFromAssistantMessage(latestAssistant.content) : null
  const pageContext = readAiBookingPageContext()
  const resolvedSlotDate =
    (preview.slotDate && preview.slotDate !== 'N/A' ? preview.slotDate : null) ||
    fallback?.slotDate ||
    normalizeString(pageContext?.selectedDate) ||
    'N/A'
  const resolvedStartTime =
    (preview.startTime && preview.startTime !== '--:--:--' ? preview.startTime : null) ||
    fallback?.startTime ||
    normalizeTime(normalizeString(pageContext?.selectedSlotStartTime)) ||
    '--:--:--'
  const resolvedEndTime =
    (preview.endTime && preview.endTime !== '--:--:--' ? preview.endTime : null) ||
    fallback?.endTime ||
    normalizeTime(normalizeString(pageContext?.selectedSlotEndTime)) ||
    '--:--:--'
  const resolvedSpecialistId =
    preview.specialistId ||
    fallback?.specialistId ||
    resolveCurrentPageSpecialistId() ||
    resolveSpecialistIdByName(pageContext, preview.specialistName || fallback?.specialistName || null) ||
    normalizeNumericId(pageContext?.specialistId) ||
    0
  const resolvedSlotId =
    preview.slotId ||
    fallback?.slotId ||
    resolveSlotIdByContext(pageContext, resolvedSlotDate, resolvedStartTime, resolvedEndTime) ||
    normalizeNumericId(pageContext?.selectedSlotId) ||
    0
  if (!resolvedSpecialistId || !resolvedSlotId) {
    return
  }
  const slotAvailable = isSlotAvailableInContext(pageContext, resolvedSlotId)
  if (slotAvailable === false) {
    return
  }

  const mergedPreview: AiBookingSubmitPreviewPayload = {
    specialistId: resolvedSpecialistId,
    slotId: resolvedSlotId,
    slotDate: resolvedSlotDate,
    startTime: resolvedStartTime,
    endTime: resolvedEndTime,
    specialistName: preview.specialistName || fallback?.specialistName || normalizeString(pageContext?.specialistName) || null,
    consultationFee: preview.consultationFee ?? fallback?.consultationFee ?? normalizeNumber(pageContext?.consultationFee),
    topic: (preview.topic || fallback?.topic || normalizeString(pageContext?.selectedTopic) || '').trim(),
    customerNotes: (preview.customerNotes || fallback?.customerNotes || normalizeString(pageContext?.selectedCustomerNotes) || '').trim() || null,
    warnings: preview.warnings
  }

  const previewKey = buildPreviewKey(mergedPreview)
  if (previewKey === lastPreviewKey.value && bookingConfirmDialogVisible.value) {
    return
  }
  lastPreviewKey.value = previewKey
  bookingPreview.value = mergedPreview
  bookingConfirmDialogVisible.value = true
}

const BOOKING_PREVIEW_HINT_PATTERN = /(readyToSubmit|ready to submit|confirm booking|submit booking|slot\s*id|bookingid|\u9884\u7ea6|\u786e\u8ba4\u9884\u7ea6|\u786e\u8ba4\u63d0\u4ea4|\u4e0b\u5355)/i

const buildFallbackPreviewFromPageContext = (): AiBookingSubmitPreviewPayload | null => {
  const pageContext = readAiBookingPageContext()
  const specialistId = normalizeNumericId(pageContext?.specialistId)
  const slotDate = normalizeString(pageContext?.selectedDate)
  const startTime = normalizeTime(normalizeString(pageContext?.selectedSlotStartTime))
  const endTime = normalizeTime(normalizeString(pageContext?.selectedSlotEndTime))
  const slotId =
    normalizeNumericId(pageContext?.selectedSlotId) ||
    resolveSlotIdByContext(pageContext, slotDate, startTime, endTime)
  if (!specialistId || !slotId) {
    return null
  }
  const slotAvailable = isSlotAvailableInContext(pageContext, slotId)
  if (slotAvailable === false) {
    return null
  }

  return {
    specialistId,
    slotId,
    slotDate: slotDate || 'N/A',
    startTime: startTime || '--:--:--',
    endTime: endTime || '--:--:--',
    specialistName: normalizeString(pageContext?.specialistName) || null,
    consultationFee: normalizeNumber(pageContext?.consultationFee),
    topic: normalizeString(pageContext?.selectedTopic) || '',
    customerNotes: normalizeString(pageContext?.selectedCustomerNotes) || null,
  }
}

const onAiBookingSubmitPreview = (event: Event): void => {
  const customEvent = event as CustomEvent<AiBookingSubmitPreviewPayload>
  if (!customEvent.detail) {
    return
  }
  openBookingPreview(customEvent.detail)

  if (customEvent.detail.warnings?.length) {
    ElMessage.warning(`AI draft warning: ${customEvent.detail.warnings[0]}`)
  }
}

const dismissBookingPreview = () => {
  if (bookingSubmitting.value) {
    return
  }
  bookingConfirmDialogVisible.value = false
  bookingPreview.value = null
}

const formatFee = (fee?: number | null) => {
  if (fee === null || fee === undefined) {
    return 'N/A'
  }
  const amount = Number(fee)
  return Number.isFinite(amount) ? `CNY ${amount.toFixed(2)}` : 'N/A'
}

const confirmBookingFromPreview = async () => {
  if (!bookingPreview.value || bookingSubmitting.value) {
    return
  }

  bookingSubmitting.value = true
  try {
    const created = await createBooking({
      specialistId: bookingPreview.value.specialistId,
      slotId: bookingPreview.value.slotId,
      topic: bookingPreview.value.topic,
      customerNotes: bookingPreview.value.customerNotes || '',
    }, true)

    bookingPreview.value = null
    bookingConfirmDialogVisible.value = false
    aiChatStore.closeDrawer()
    ElMessage.success(`Booking created successfully. Status: ${created.status}.`)
    void router.push({
      path: '/customer/bookings'
    })
  } catch (error: any) {
    const message = error?.message || 'Failed to create booking.'
    ElMessage.error(message)
  } finally {
    bookingSubmitting.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', updateMobileState)
  window.addEventListener(AI_BOOKING_SUBMIT_PREVIEW_EVENT, onAiBookingSubmitPreview as EventListener)
})

watch(
  () => {
    const latest = aiChatStore.messages[aiChatStore.messages.length - 1]
    if (!latest) {
      return ''
    }
    return `${latest.id}|${latest.role}|${latest.status}|${latest.content}`
  },
  snapshot => {
    if (!snapshot) {
      return
    }
    const latestMessage = aiChatStore.messages[aiChatStore.messages.length - 1]
    if (!latestMessage || latestMessage.role !== 'assistant' || latestMessage.status !== 'done') {
      return
    }
    if (isBookingConflictContent(latestMessage.content || '')) {
      return
    }
    const parsedPreview = parsePreviewFromAssistantMessage(latestMessage.content || '')
    if (parsedPreview) {
      openBookingPreview(parsedPreview)
      return
    }

    if (BOOKING_PREVIEW_HINT_PATTERN.test(latestMessage.content || '')) {
      const fallbackPreview = buildFallbackPreviewFromPageContext()
      if (fallbackPreview) {
        openBookingPreview(fallbackPreview)
      }
    }
  }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateMobileState)
  window.removeEventListener(AI_BOOKING_SUBMIT_PREVIEW_EVENT, onAiBookingSubmitPreview as EventListener)
  bookingConfirmDialogVisible.value = false
  bookingPreview.value = null
  bookingSubmitting.value = false
})

const drawerVisible = computed<boolean>({
  get: () => aiChatStore.isDrawerOpen,
  set: (value: boolean) => {
    if (value) {
      aiChatStore.openDrawer()
      return
    }

    aiChatStore.closeDrawer()
    bookingConfirmDialogVisible.value = false
    bookingPreview.value = null
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

.booking-confirm-content {
  display: grid;
  gap: 10px;
}

.booking-confirm-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
  border-top: 1px solid var(--color-border);
}

.booking-confirm-row:first-child {
  border-top: none;
  padding-top: 0;
}

.booking-confirm-row span {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.booking-confirm-row strong {
  color: var(--color-text-primary);
  text-align: right;
}

:deep(.ai-chat-drawer__message-area .ai-message-list) {
  flex: 1;
  min-height: 0;
}

@media (max-width: 640px) {
  .ai-chat-drawer__header {
    align-items: center;
    flex-direction: row; // Keep header items side by side on mobile for space efficiency
  }
}
</style>

<style lang="scss">
.el-drawer.ai-chat-drawer {
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-surface);
  border-radius: 28px 0 0 28px !important; // Desktop rounded
  box-shadow: none !important;
  border-left: 1px solid var(--color-border);

  &.btt {
    border-radius: 28px 28px 0 0 !important; // Mobile bottom-to-top mode rounded top
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

