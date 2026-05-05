import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { deleteChatMemory, postChatMessage } from '@/api/ai'
import { getUser } from '@/api/request'
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

const AI_BOOKING_DRAFT_EVENT = 'ai-booking-form-draft'
const AI_BOOKING_SUBMIT_PREVIEW_EVENT = 'ai-booking-submit-preview'
const AI_BOOKING_CANCEL_MODAL_EVENT = 'ai-booking-cancel-modal'
const AI_BOOKING_RESCHEDULE_MODAL_EVENT = 'ai-booking-reschedule-modal'
const AI_BOOKING_CONTEXT_STORAGE_KEY = 'ai.booking.context'
const CANCEL_MODAL_PATTERN = /\[TRIGGER_CANCEL_MODAL:(\d+)\]/i
const RESCHEDULE_MODAL_PATTERN = /\[TRIGGER_RESCHEDULE_MODAL:(\d+):((?:\d{4}-\d{2}-\d{2})|N\/A)?:(\d*)\]/i
const WORKFLOW_ABORT_MARKER_PATTERN = /\[(?:BOOKING|CANCEL|RESCHEDULE)_TASK_ABORTED\]\s*/gi

interface StoredSessionUser {
  userId?: number | string | null
  id?: number | string | null
}

interface AiBookingDraftPayload {
  specialistId?: number | null
  slotId?: number | null
  topic?: string | null
  customerNotes?: string | null
  availableTopics?: string[]
  warnings?: string[]
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
  warnings?: string[]
}

interface AiBookingCancelModalPayload {
  bookingId: number
}

interface AiBookingRescheduleModalPayload {
  bookingId: number
  targetDate?: string | null
  suggestedSlotId?: number | null
}

const resolveErrorMessage = (error: unknown): string => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return AI_CHAT_DEFAULT_ERROR
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

const resolveBookingContextStorageKey = (): string => {
  const currentUserId = resolveCurrentUserId()
  return currentUserId
    ? `${AI_BOOKING_CONTEXT_STORAGE_KEY}:${currentUserId}`
    : AI_BOOKING_CONTEXT_STORAGE_KEY
}

const resolveChatAccountScopeKey = (): string => {
  const currentUserId = resolveCurrentUserId()
  return currentUserId ? `user:${currentUserId}` : 'anonymous'
}

const normalizeNumericId = (value: unknown): number | null | undefined => {
  if (value === null || value === undefined || value === '') {
    return null
  }

  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return null
  }
  return Math.trunc(parsed)
}

const normalizeString = (value: unknown): string | null => {
  if (typeof value !== 'string') {
    return null
  }
  const trimmed = value.trim()
  return trimmed || null
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

const tryExtractBookingDraft = (content: string): AiBookingDraftPayload | null => {
  const parsed = parseJsonLikeContent(content)
  if (!parsed) {
    return null
  }

  const specialistId = normalizeNumericId(parsed.specialistId)
  const slotId = normalizeNumericId(parsed.slotId)
  const topic = normalizeString(parsed.topic)
  const customerNotes = normalizeString(parsed.customerNotes)
  const availableTopics = Array.isArray(parsed.availableTopics)
    ? parsed.availableTopics.filter(item => typeof item === 'string').map(item => item.trim()).filter(Boolean)
    : undefined
  const warnings = Array.isArray(parsed.warnings)
    ? parsed.warnings.filter(item => typeof item === 'string').map(item => item.trim()).filter(Boolean)
    : undefined

  const hasDraftFields =
    specialistId !== undefined ||
    slotId !== undefined ||
    topic !== null ||
    customerNotes !== null ||
    Boolean(availableTopics?.length) ||
    Boolean(warnings?.length)

  if (!hasDraftFields) {
    return null
  }

  return {
    specialistId,
    slotId,
    topic,
    customerNotes,
    availableTopics,
    warnings
  }
}

const emitBookingDraft = (assistantContent: string): void => {
  if (typeof window === 'undefined') {
    return
  }

  const draft = tryExtractBookingDraft(assistantContent)
  if (!draft) {
    return
  }

  window.dispatchEvent(new CustomEvent<AiBookingDraftPayload>(AI_BOOKING_DRAFT_EVENT, {
    detail: draft
  }))
}

const normalizeNumber = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const getParsedValueByAliases = (parsed: Record<string, unknown>, aliases: string[]): unknown => {
  for (const alias of aliases) {
    if (alias in parsed) {
      return parsed[alias]
    }
  }
  return undefined
}

const parseBooleanFlag = (value: unknown): boolean => {
  if (value === true) {
    return true
  }
  const normalized = String(value ?? '').trim().toLowerCase()
  return normalized === 'true' || normalized === '1' || normalized === 'yes'
}

const normalizeTime = (value: string | null): string | null => {
  if (!value) {
    return null
  }
  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }
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

const BOOKING_CONFLICT_PATTERN = /("success"\s*:\s*false|"readyToSubmit"\s*:\s*false|readyToSubmit\s*[:=\uFF1A\uFF1D]\s*false|not\s+available|already\s+booked|booking\s+conflict|time\s+slot\s+is\s+no\s+longer\s+available|please\s+choose\s+another|failed\s+to\s+create\s+booking|failed\s+to\s+submit\s+booking|\u9884\u7ea6\u51b2\u7a81|\u5df2\u88ab\u9884\u7ea6|\u5df2\u88ab\u5360\u7528|\u4e0d\u53ef\u7528|\u9884\u7ea6\u5931\u8d25|\u65f6\u6bb5\u4e0d\u53ef\u7528|\u8bf7\u9009\u62e9\u5176\u4ed6\u65f6\u6bb5)/i

const isBookingConflictContent = (content: string): boolean => {
  return BOOKING_CONFLICT_PATTERN.test(content || '')
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

const tryExtractBookingSubmitPreviewFromText = (content: string): AiBookingSubmitPreviewPayload | null => {
  if (isBookingConflictContent(content)) {
    return null
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
      /\u65e5\u671f\s*[:\uFF1A]\s*(\d{4}-\d{2}-\d{2})/,
      /date\s*[:\uFF1A]\s*(\d{4}-\d{2}-\d{2})/i,
    ])
  )

  const startTimeMatch = extractFirstMatch(content, [
    /startTime\s*[:=\uFF1A\uFF1D]\s*(\d{1,2}:\d{2}(?::\d{2})?)/i,
  ])
  const endTimeMatch = extractFirstMatch(content, [
    /endTime\s*[:=\uFF1A\uFF1D]\s*(\d{1,2}:\d{2}(?::\d{2})?)/i,
  ])

  const [rangeStartTime, rangeEndTime] = extractTimeRangeFromContent(content)
  const startTime = normalizeTime(startTimeMatch || rangeStartTime || null)
  const endTime = normalizeTime(endTimeMatch || rangeEndTime || null)

  const topic = normalizeLineValue(extractFirstMatch(content, [
    /topic\s*[:=\uFF1A\uFF1D]\s*([^\n\r]+)/i,
    /\u4e3b\u9898\s*[:\uFF1A]\s*([^\n\r]+)/,
    /(?:\u4e3b\u9898|topic)\s*[^\n\r]{0,4}\s*([^\n\r]+)/i,
  ]))
  const customerNotes = normalizeLineValue(extractFirstMatch(content, [
    /customerNotes\s*[:=\uFF1A\uFF1D]\s*([^\n\r]+)/i,
    /\u5907\u6ce8\s*[:\uFF1A]\s*([^\n\r]+)/,
    /(?:\u5907\u6ce8|notes?|customerNotes)\s*[^\n\r]{0,4}\s*([^\n\r]+)/i,
  ]))

  const specialistName = normalizeLineValue(extractFirstMatch(content, [
    /specialistName\s*[:=\uFF1A\uFF1D]\s*([^\n\r]+)/i,
    /(?:\u533b\u751f|\u4e13\u5bb6|specialist)\s*[:\uFF1A]\s*([^\n\r]+)/i,
  ]))
  const consultationFee = normalizeNumber(
    extractFirstMatch(content, [
      /consultationFee\s*[:=\uFF1A\uFF1D]\s*([0-9]+(?:\.[0-9]+)?)/i,
      /(?:consultation\s*fee|fee|price)\s*[:=\uFF1A\uFF1D]\s*[\u00A5\uFFE5]?\s*([0-9]+(?:\.[0-9]+)?)/i,
      /\u54a8\u8be2\u8d39\s*[:\uFF1A]\s*[\u00A5\uFFE5]?\s*([0-9]+(?:\.[0-9]+)?)/,
      /(?:\u8d39\u7528|\u54a8\u8be2\u8d39|price|fee|consultation\s*fee)\s*[^\d\n\r]{0,6}([0-9]+(?:\.[0-9]+)?)/i,
    ])
  )

  const pageContext = readBookingPageContext()
  const specialistId = specialistIdRaw || normalizeNumericId(pageContext?.specialistId)
  const slotId = slotIdRaw || normalizeNumericId(pageContext?.selectedSlotId)
  if (!specialistId && !specialistName) {
    return null
  }

  return {
    specialistId: specialistId || 0,
    slotId: slotId || 0,
    topic: topic || normalizeString(pageContext?.selectedTopic) || '',
    slotDate: slotDate || normalizeString(pageContext?.selectedDate) || 'N/A',
    startTime: startTime || normalizeTime(normalizeString(pageContext?.selectedSlotStartTime)) || '--:--:--',
    endTime: endTime || normalizeTime(normalizeString(pageContext?.selectedSlotEndTime)) || '--:--:--',
    specialistName: specialistName || normalizeString(pageContext?.specialistName),
    consultationFee: consultationFee ?? normalizeNumber(pageContext?.consultationFee),
    customerNotes: customerNotes || normalizeString(pageContext?.selectedCustomerNotes),
  }
}

const tryExtractBookingSubmitPreview = (content: string): AiBookingSubmitPreviewPayload | null => {
  const parsed = parseJsonLikeContent(content)
  if (parsed) {
    const success = parseBooleanFlag(
      getParsedValueByAliases(parsed, ['success', '\u6210\u529f'])
    )
    const readyToSubmit = parseBooleanFlag(
      getParsedValueByAliases(parsed, ['readyToSubmit', 'ready_to_submit', '\u5f85\u786e\u8ba4', '\u53ef\u63d0\u4ea4'])
    )
    const specialistIdRaw = normalizeNumericId(
      getParsedValueByAliases(parsed, ['specialistId', 'specialist_id', '\u533b\u751fID', '\u4e13\u5bb6ID'])
    )
    const slotIdRaw = normalizeNumericId(
      getParsedValueByAliases(parsed, ['slotId', 'slot_id', '\u65f6\u6bb5ID'])
    )
    const topic = normalizeString(
      getParsedValueByAliases(parsed, ['topic', '\u4e3b\u9898'])
    )
    const slotDate = normalizeString(
      getParsedValueByAliases(parsed, ['slotDate', 'slot_date', '\u65e5\u671f'])
    )
    const startTime = normalizeTime(normalizeString(
      getParsedValueByAliases(parsed, ['startTime', 'start_time', '\u5f00\u59cb\u65f6\u95f4'])
    ))
    const endTime = normalizeTime(normalizeString(
      getParsedValueByAliases(parsed, ['endTime', 'end_time', '\u7ed3\u675f\u65f6\u95f4'])
    ))
    const specialistName = normalizeString(
      getParsedValueByAliases(parsed, ['specialistName', 'specialist_name', '\u533b\u751f', '\u4e13\u5bb6'])
    )
    const customerNotes = normalizeString(
      getParsedValueByAliases(parsed, ['customerNotes', 'customer_notes', 'notes', '\u5907\u6ce8'])
    )
    const consultationFee = normalizeNumber(
      getParsedValueByAliases(parsed, ['consultationFee', 'consultation_fee', 'fee', 'price', '\u54a8\u8be2\u8d39'])
    )
    const warningsRaw = getParsedValueByAliases(parsed, ['warnings', '\u8b66\u544a'])
    const warnings = Array.isArray(warningsRaw)
      ? warningsRaw.filter(item => typeof item === 'string').map(item => item.trim()).filter(Boolean)
      : undefined
    const availableTopicsRaw = getParsedValueByAliases(parsed, ['availableTopics', 'available_topics', '\u53ef\u9009\u4e3b\u9898'])
    const availableTopics = Array.isArray(availableTopicsRaw)
      ? availableTopicsRaw.filter(item => typeof item === 'string').map(item => item.trim()).filter(Boolean)
      : undefined

    const hasStructuredBookingFields = Boolean(
      specialistIdRaw || slotIdRaw || topic || slotDate || startTime || endTime || specialistName
    )

    if (success && readyToSubmit || hasStructuredBookingFields) {
      const pageContext = readBookingPageContext()
      const specialistId = specialistIdRaw || normalizeNumericId(pageContext?.specialistId)
      const slotId = slotIdRaw || normalizeNumericId(pageContext?.selectedSlotId)
      if (!specialistId && !specialistName) {
        return null
      }

      return {
        specialistId: specialistId || 0,
        slotId: slotId || 0,
        topic: topic || normalizeString(pageContext?.selectedTopic) || '',
        slotDate: slotDate || normalizeString(pageContext?.selectedDate) || 'N/A',
        startTime: startTime || normalizeTime(normalizeString(pageContext?.selectedSlotStartTime)) || '--:--:--',
        endTime: endTime || normalizeTime(normalizeString(pageContext?.selectedSlotEndTime)) || '--:--:--',
        specialistName: specialistName || normalizeString(pageContext?.specialistName),
        consultationFee: consultationFee ?? normalizeNumber(pageContext?.consultationFee),
        customerNotes: customerNotes || normalizeString(pageContext?.selectedCustomerNotes),
        availableTopics,
        warnings
      }
    }
  }

  return tryExtractBookingSubmitPreviewFromText(content)
}

const emitBookingSubmitPreview = (assistantContent: string): void => {
  if (typeof window === 'undefined') {
    return
  }
  const preview = tryExtractBookingSubmitPreview(assistantContent)
  if (!preview) {
    return
  }
  window.dispatchEvent(new CustomEvent<AiBookingSubmitPreviewPayload>(AI_BOOKING_SUBMIT_PREVIEW_EVENT, {
    detail: preview
  }))
}

const extractCancelModalBookingId = (content: string): number | null => {
  const matched = content.match(CANCEL_MODAL_PATTERN)
  if (!matched?.[1]) {
    return null
  }
  const parsed = Number(matched[1])
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return null
  }
  return Math.trunc(parsed)
}

const extractRescheduleModalPayload = (content: string): AiBookingRescheduleModalPayload | null => {
  const matched = content.match(RESCHEDULE_MODAL_PATTERN)
  if (!matched?.[1]) {
    return null
  }
  const bookingId = Number(matched[1])
  if (!Number.isFinite(bookingId) || bookingId <= 0) {
    return null
  }
  const rawTargetDate = normalizeString(matched[2] || null)
  const targetDate = rawTargetDate === 'N/A' ? null : rawTargetDate
  const suggestedSlotId = normalizeNumericId(matched[3] || null) ?? null
  return {
    bookingId: Math.trunc(bookingId),
    targetDate,
    suggestedSlotId
  }
}

const stripCancelModalMarker = (content: string): string => {
  return content.replace(CANCEL_MODAL_PATTERN, '').replace(/\n{3,}/g, '\n\n').trim()
}

const stripRescheduleModalMarker = (content: string): string => {
  return content.replace(RESCHEDULE_MODAL_PATTERN, '').replace(/\n{3,}/g, '\n\n').trim()
}

const stripWorkflowAbortMarker = (content: string): string => {
  return content.replace(WORKFLOW_ABORT_MARKER_PATTERN, '').trim()
}

const emitCancelModal = (assistantContent: string): string => {
  if (typeof window === 'undefined') {
    return assistantContent
  }
  const bookingId = extractCancelModalBookingId(assistantContent)
  if (bookingId) {
    window.dispatchEvent(new CustomEvent<AiBookingCancelModalPayload>(AI_BOOKING_CANCEL_MODAL_EVENT, {
      detail: { bookingId }
    }))
  }
  return stripCancelModalMarker(assistantContent)
}

const emitRescheduleModal = (assistantContent: string): string => {
  if (typeof window === 'undefined') {
    return assistantContent
  }
  const payload = extractRescheduleModalPayload(assistantContent)
  if (payload) {
    window.dispatchEvent(new CustomEvent<AiBookingRescheduleModalPayload>(AI_BOOKING_RESCHEDULE_MODAL_EVENT, {
      detail: payload
    }))
  }
  return stripRescheduleModalMarker(assistantContent)
}

interface AiBookingPageContext {
  specialistId?: number
  specialistName?: string
  consultationFee?: number
  selectedDate?: string
  selectedSlotId?: number
  selectedSlotStartTime?: string
  selectedSlotEndTime?: string
  selectedTopic?: string
  selectedCustomerNotes?: string
}

const readBookingPageContext = (): AiBookingPageContext | null => {
  if (typeof window === 'undefined') {
    return null
  }
  try {
    const scopedStorageKey = resolveBookingContextStorageKey()
    let raw = window.sessionStorage.getItem(scopedStorageKey)
    if (!raw && scopedStorageKey !== AI_BOOKING_CONTEXT_STORAGE_KEY) {
      raw = window.sessionStorage.getItem(AI_BOOKING_CONTEXT_STORAGE_KEY)
      if (raw) {
        window.sessionStorage.setItem(scopedStorageKey, raw)
      }
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

const getSpecialistBookingPageContext = (): string => {
  if (typeof window === 'undefined') {
    return ''
  }

  const pathname = window.location.pathname || ''
  const matched = pathname.match(/^\/customer\/specialists\/(\d+)\/book$/)
  if (!matched || !matched[1]) {
    return ''
  }

  const specialistId = Number(matched[1])
  const bookingPageContext = readBookingPageContext()
  const isMatchingSpecialistContext = bookingPageContext?.specialistId === specialistId
  const selectedDate = isMatchingSpecialistContext ? bookingPageContext?.selectedDate?.trim() || '' : ''
  const selectedSlotId = isMatchingSpecialistContext ? bookingPageContext?.selectedSlotId : undefined
  const isSlotIdValid = Number.isFinite(selectedSlotId) && Number(selectedSlotId) > 0
  const selectedSlotStartTime = isMatchingSpecialistContext
    ? normalizeTime(normalizeString(bookingPageContext?.selectedSlotStartTime))
    : null
  const selectedSlotEndTime = isMatchingSpecialistContext
    ? normalizeTime(normalizeString(bookingPageContext?.selectedSlotEndTime))
    : null

  const contextLines = [
    `Current page context: customer is on booking page for specialistId=${specialistId}.`
  ]

  if (bookingPageContext?.specialistId && bookingPageContext.specialistId !== specialistId) {
    contextLines.push(`Ignore stale stored booking context from specialistId=${bookingPageContext.specialistId}.`)
  }

  if (selectedDate) {
    contextLines.push(`Selected booking date on page: ${selectedDate}.`)
  }

  if (isSlotIdValid) {
    contextLines.push(`Selected slotId on page: ${selectedSlotId}.`)
  }
  if (selectedSlotStartTime && selectedSlotEndTime) {
    contextLines.push(`Selected slot time on page: ${selectedSlotStartTime} - ${selectedSlotEndTime}.`)
  }

  contextLines.push('If user asks to place booking for "this doctor", use this specialistId context.')
  if (selectedDate) {
    contextLines.push('If user omits booking date but asks to book now on this page, use the selected booking date context.')
  }
  if (selectedDate && selectedSlotStartTime) {
    contextLines.push('If user asks to book now on this page, call submitCurrentCustomerBooking with selected date and selected slot time.')
  }
  if (typeof bookingPageContext?.selectedTopic === 'string' && bookingPageContext.selectedTopic.trim()) {
    contextLines.push(`Current selected topic on page: ${bookingPageContext.selectedTopic.trim()}.`)
  }
  if (typeof bookingPageContext?.selectedCustomerNotes === 'string' && bookingPageContext.selectedCustomerNotes.trim()) {
    contextLines.push(`Current selected customer notes on page: ${bookingPageContext.selectedCustomerNotes.trim()}.`)
  }

  return contextLines.join('\n')
}

const buildContextualUserMessage = (userMessage: string): string => {
  const pageContext = getSpecialistBookingPageContext()
  if (!pageContext) {
    return userMessage
  }

  return `${pageContext}\n\nUser message:\n${userMessage}`
}

let aiChatMessageSequence = 0

export const useAiChatStore = defineStore(AI_CHAT_STORE_ID, () => {
  const isDrawerOpen = ref<boolean>(false)
  const inputMessage = ref<string>('')
  const messages = ref<AiChatMessage[]>([])
  const errorMessage = ref<string>('')
  const state = ref<AiChatState>(AI_CHAT_STATE.idle)
  const accountScopeKey = ref<string>(resolveChatAccountScopeKey())

  const isLoading = computed<boolean>(() => state.value === AI_CHAT_STATE.loading)

  const openDrawer = (): void => {
    ensureCurrentAccountScope()
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
      const normalizedContent = stripWorkflowAbortMarker(emitRescheduleModal(emitCancelModal(message.content.trim())))
      message.content = normalizedContent.trim()
        ? normalizedContent
        : AI_CHAT_EMPTY_RESPONSE_TEXT
      message.status = AI_CHAT_MESSAGE_STATUS.done
      emitBookingDraft(message.content)
      emitBookingSubmitPreview(message.content)
    })
  }

  const resetConversationState = (): void => {
    messages.value = []
    inputMessage.value = ''
    errorMessage.value = ''
    state.value = AI_CHAT_STATE.idle
  }

  const ensureCurrentAccountScope = (): void => {
    const latestScopeKey = resolveChatAccountScopeKey()
    if (accountScopeKey.value === latestScopeKey) {
      return
    }
    accountScopeKey.value = latestScopeKey
    resetConversationState()
    isDrawerOpen.value = false
  }

  const resetError = (): void => {
    errorMessage.value = ''
    if (state.value === AI_CHAT_STATE.error) {
      state.value = AI_CHAT_STATE.idle
    }
  }

  const sendMessage = async (): Promise<void> => {
    ensureCurrentAccountScope()
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
    const contextualMessage = buildContextualUserMessage(message)
    const assistantMessageId = appendMessage(
      AI_CHAT_MESSAGE_ROLE.assistant,
      '',
      AI_CHAT_MESSAGE_STATUS.streaming
    )

    try {
      let hasDoneEvent = false
      await postChatMessage(contextualMessage, {
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
          currentMessage.content = stripWorkflowAbortMarker(emitRescheduleModal(emitCancelModal(currentMessage.content)))
          currentMessage.status = AI_CHAT_MESSAGE_STATUS.done
        })
        const finalAssistantMessage = messages.value.find(messageItem => messageItem.id === assistantMessageId)
        if (finalAssistantMessage) {
          emitBookingDraft(finalAssistantMessage.content)
          emitBookingSubmitPreview(finalAssistantMessage.content)
        }
      }
      errorMessage.value = resolveErrorMessage(error)
      state.value = AI_CHAT_STATE.error
    }
  }

  const clearConversation = async (): Promise<void> => {
    ensureCurrentAccountScope()
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
