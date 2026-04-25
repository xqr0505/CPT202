<template>
  <section class="booking-page">
    <div class="page-actions">
      <CustomButton @click="goBack">Back to details</CustomButton>
    </div>

    <section v-if="loading" class="page-loading card">
      <el-skeleton animated :rows="8" />
    </section>

    <template v-else-if="specialist">
      <section class="overview card">
        <div class="overview-main">
          <el-avatar :src="specialist.avatarUrl" :size="88">
            {{ specialist.name.charAt(0).toUpperCase() }}
          </el-avatar>

          <div class="identity">
            <p class="eyebrow">Booking page</p>
            <h1>Book {{ specialist.name }}</h1>
            <div class="tag-row">
              <el-tag effect="plain">{{ specialist.categoryName || 'Unassigned category' }}</el-tag>
              <el-tag>{{ formatLevel(specialist.level) }}</el-tag>
              <el-tag :type="specialist.status === 'ACTIVE' ? 'success' : 'info'">
                {{ formatStatus(specialist.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <aside class="price-panel">
          <span class="panel-label">Consultation fee</span>
          <strong class="panel-price">&#165;{{ Number(specialist.consultationFee || 0).toFixed(2) }}</strong>
          <p class="panel-copy">Choose an available slot, then complete the booking request.</p>
        </aside>
      </section>

      <article class="card booking-card">
        <div class="booking-head">
          <div>
            <h2>Booking form</h2>
            <p>{{ bookingIntro }}</p>
          </div>
          <el-tag v-if="selectedSlot" type="success" effect="plain">
            {{ selectedDate }} {{ selectedSlot.startTime }} - {{ selectedSlot.endTime }}
          </el-tag>
        </div>

        <div class="booking-layout">
          <div class="booking-main">
            <section class="availability-panel">
              <div class="availability-head">
                <div>
                  <h3>Available time slots</h3>
                  <p>Only available slots can be selected.</p>
                </div>

                <el-date-picker
                  v-model="selectedDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="Select date"
                />
              </div>

              <div v-if="availabilityLoading" class="availability-loading">
                <el-skeleton animated :rows="4" />
              </div>

              <EmptyPlaceholder
                v-else-if="!isSpecialistActive"
                description="This specialist is inactive and cannot accept new bookings."
              />

              <EmptyPlaceholder
                v-else-if="!availability.length"
                description="No slots were published for the selected date."
              />

              <div v-else class="slot-grid">
                <button
                  v-for="slot in availability"
                  :key="slot.id"
                  type="button"
                  class="slot-chip"
                  :class="{
                    active: bookingForm.slotId === slot.id,
                    unavailable: isSlotUnavailable(slot),
                  }"
                  @click="selectSlot(slot)"
                >
                  <span>{{ slot.startTime }}</span>
                  <small>{{ slot.endTime }}</small>
                  <em class="slot-state">{{ getSlotStatusLabel(slot) }}</em>
                </button>
              </div>
            </section>

            <el-form label-position="top" class="booking-form">
              <el-form-item label="Topic" required>
                <el-select
                  v-model="bookingForm.topic"
                  placeholder="Select a topic"
                  class="topic-select"
                >
                  <el-option
                    v-for="topic in bookingTopics"
                    :key="topic"
                    :label="topic"
                    :value="topic"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="Notes">
                <el-input
                  v-model="bookingForm.customerNotes"
                  type="textarea"
                  :rows="4"
                  maxlength="500"
                  show-word-limit
                  placeholder="Optional details you want the specialist to know"
                />
                <p v-if="notesFormatError" class="form-error">{{ notesFormatError }}</p>
              </el-form-item>
            </el-form>
          </div>

          <aside class="booking-summary">
            <h3>Booking summary</h3>
            <div class="summary-row">
              <span>Specialist</span>
              <strong>{{ specialist.name }}</strong>
            </div>
            <div class="summary-row">
              <span>Time slot</span>
              <strong>{{ selectedSlot ? `${selectedDate} ${selectedSlot.startTime} - ${selectedSlot.endTime}` : 'Please choose a slot' }}</strong>
            </div>
            <div class="summary-row">
              <span>Total cost</span>
              <strong>&#165;{{ Number(specialist.consultationFee || 0).toFixed(2) }}</strong>
            </div>
            <div class="summary-row">
              <span>Topic</span>
              <strong>{{ bookingForm.topic || 'Please choose a topic' }}</strong>
            </div>
            <div class="summary-row">
              <span>Notes</span>
              <strong>{{ bookingForm.customerNotes.trim() || 'No notes provided' }}</strong>
            </div>
            <div class="summary-row">
              <span>Status after submission</span>
              <strong>Pending</strong>
            </div>
          </aside>
        </div>

        <div class="booking-actions">
          <span class="booking-tip">
            {{ selectedSlot ? `Selected ${selectedDate} ${selectedSlot.startTime}` : 'Please choose an available time slot first.' }}
          </span>
          <CustomButton
            :disabled="!isSpecialistActive || Boolean(notesFormatError)"
            @click="submitBooking"
          >
            {{ bookingSubmitting ? 'Submitting...' : 'Confirm booking' }}
          </CustomButton>
        </div>
      </article>
    </template>

    <EmptyPlaceholder
      v-else
      description="The requested specialist could not be found."
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createBooking, getBookingTopics } from '@/api/booking'
import { getUser } from '@/api/request'
import { fetchSpecialistAvailability, fetchSpecialistDetail } from '@/api/specialist'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import type { SpecialistAvailabilitySlot, SpecialistDetail } from '@/types/specialist'

defineOptions({ name: 'SpecialistBooking' })

const CUSTOMER_NOTES_PATTERN = /^[\p{L}\p{N}\p{P}\p{Z}\r\n]*$/u
const AI_BOOKING_DRAFT_EVENT = 'ai-booking-form-draft'
const AI_BOOKING_CONTEXT_STORAGE_KEY = 'ai.booking.context'
const BOOKING_FORM_DRAFT_STORAGE_KEY = 'customer.booking.form.draft'

interface StoredSessionUser {
  userId?: number | string | null
  id?: number | string | null
}

interface PersistedBookingFormDraft {
  topic?: string
  customerNotes?: string
}

const route = useRoute()
const router = useRouter()

const specialist = ref<SpecialistDetail | null>(null)
const availability = ref<SpecialistAvailabilitySlot[]>([])
const bookingTopics = ref<string[]>([])
const loading = ref(false)
const availabilityLoading = ref(false)
const bookingSubmitting = ref(false)
const duplicateSubmitClickCount = ref(0)

const DUPLICATE_SUBMIT_CLICK_WARNING_THRESHOLD = 3
const BOOKING_MIN_LEAD_TIME_MS = 2 * 60 * 60 * 1000
const BOOKING_MIN_LEAD_TIME_WARNING = 'You cannot book a slot within 2 hours from now. Please choose another time slot.'

const bookingForm = ref({
  slotId: null as number | null,
  topic: '',
  customerNotes: '',
})

const toLocalDateString = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = `${now.getMonth() + 1}`.padStart(2, '0')
  const day = `${now.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const selectedDate = ref(
  typeof route.query.date === 'string' && route.query.date ? route.query.date : toLocalDateString(),
)

const specialistId = computed(() => Number(route.params.id))
const isSpecialistActive = computed(() => specialist.value?.status === 'ACTIVE')
const selectedSlot = computed(
  () => availability.value.find((slot) => slot.id === bookingForm.value.slotId) ?? null,
)
const bookingIntro = computed(() =>
  isSpecialistActive.value
    ? 'Select a time slot, choose a topic, and optionally leave a note.'
    : 'This specialist is inactive. Booking is disabled and no slots can be reserved.'
)
const notesFormatError = computed(() => {
  const notes = bookingForm.value.customerNotes
  if (!notes.trim()) {
    return ''
  }
  return CUSTOMER_NOTES_PATTERN.test(notes)
    ? ''
    : 'Notes contain unsupported characters.'
})

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

const resolveBookingFormDraftStorageKey = (): string => {
  const currentUserId = resolveCurrentUserId()
  return currentUserId
    ? `${BOOKING_FORM_DRAFT_STORAGE_KEY}:${currentUserId}`
    : BOOKING_FORM_DRAFT_STORAGE_KEY
}

const restoreBookingFormDraft = () => {
  if (typeof window === 'undefined') {
    return
  }

  try {
    const scopedStorageKey = resolveBookingFormDraftStorageKey()
    let raw = window.sessionStorage.getItem(scopedStorageKey)
    if (!raw && scopedStorageKey !== BOOKING_FORM_DRAFT_STORAGE_KEY) {
      raw = window.sessionStorage.getItem(BOOKING_FORM_DRAFT_STORAGE_KEY)
      if (raw) {
        window.sessionStorage.setItem(scopedStorageKey, raw)
      }
    }
    if (!raw) {
      return
    }
    const parsed = JSON.parse(raw) as PersistedBookingFormDraft
    if (parsed && typeof parsed === 'object') {
      if (typeof parsed.topic === 'string') {
        bookingForm.value.topic = parsed.topic
      }
      if (typeof parsed.customerNotes === 'string') {
        bookingForm.value.customerNotes = parsed.customerNotes
      }
    }
  } catch {
    // Ignore malformed browser session cache
  }
}

const syncBookingFormDraft = () => {
  if (typeof window === 'undefined') {
    return
  }

  const scopedStorageKey = resolveBookingFormDraftStorageKey()
  const hasTopic = bookingForm.value.topic.trim().length > 0
  const hasNotes = bookingForm.value.customerNotes.trim().length > 0
  if (!hasTopic && !hasNotes) {
    window.sessionStorage.removeItem(scopedStorageKey)
    if (scopedStorageKey !== BOOKING_FORM_DRAFT_STORAGE_KEY) {
      window.sessionStorage.removeItem(BOOKING_FORM_DRAFT_STORAGE_KEY)
    }
    return
  }

  window.sessionStorage.setItem(
    scopedStorageKey,
    JSON.stringify({
      topic: bookingForm.value.topic,
      customerNotes: bookingForm.value.customerNotes,
    } satisfies PersistedBookingFormDraft)
  )
  if (scopedStorageKey !== BOOKING_FORM_DRAFT_STORAGE_KEY) {
    window.sessionStorage.removeItem(BOOKING_FORM_DRAFT_STORAGE_KEY)
  }
}

const syncAiBookingPageContext = () => {
  if (typeof window === 'undefined') {
    return
  }

  const contextStorageKey = resolveAiBookingContextStorageKey()
  const currentSpecialistId = Number(specialistId.value)
  if (!Number.isFinite(currentSpecialistId) || currentSpecialistId <= 0) {
    window.sessionStorage.removeItem(contextStorageKey)
    if (contextStorageKey !== AI_BOOKING_CONTEXT_STORAGE_KEY) {
      window.sessionStorage.removeItem(AI_BOOKING_CONTEXT_STORAGE_KEY)
    }
    return
  }

  window.sessionStorage.setItem(
    contextStorageKey,
    JSON.stringify({
      specialistId: Math.trunc(currentSpecialistId),
      specialistName: specialist.value?.name || undefined,
      consultationFee: specialist.value?.consultationFee ?? undefined,
      selectedDate: selectedDate.value || undefined,
      selectedSlotId: bookingForm.value.slotId ?? undefined,
      selectedSlotStartTime: selectedSlot.value?.startTime || undefined,
      selectedSlotEndTime: selectedSlot.value?.endTime || undefined,
      availableSlots: availability.value.map(slot => ({
        id: slot.id,
        slotDate: slot.slotDate || selectedDate.value,
        startTime: slot.startTime,
        endTime: slot.endTime,
        status: slot.status,
      })),
      selectedTopic: bookingForm.value.topic || undefined,
      selectedCustomerNotes: bookingForm.value.customerNotes.trim() || undefined,
    })
  )
  if (contextStorageKey !== AI_BOOKING_CONTEXT_STORAGE_KEY) {
    window.sessionStorage.removeItem(AI_BOOKING_CONTEXT_STORAGE_KEY)
  }
}

const loadDetail = async () => {
  if (!Number.isInteger(specialistId.value) || specialistId.value <= 0) {
    specialist.value = null
    return
  }

  loading.value = true
  try {
    specialist.value = await fetchSpecialistDetail(specialistId.value)
  } finally {
    loading.value = false
  }
}

const loadAvailability = async () => {
  if (!specialist.value || !selectedDate.value) {
    availability.value = []
    syncAiBookingPageContext()
    return
  }
  if (!isSpecialistActive.value) {
    availability.value = []
    bookingForm.value.slotId = null
    syncAiBookingPageContext()
    return
  }

  availabilityLoading.value = true
  try {
    availability.value = await fetchSpecialistAvailability(specialist.value.id, selectedDate.value)
    if (!availability.value.some((slot) => slot.id === bookingForm.value.slotId && slot.status === 'AVAILABLE')) {
      bookingForm.value.slotId = null
    }
    syncAiBookingPageContext()
  } finally {
    availabilityLoading.value = false
  }
}

const loadBookingTopics = async () => {
  bookingTopics.value = await getBookingTopics()
}

const resolveSlotStartAt = (slot: SpecialistAvailabilitySlot): Date | null => {
  const slotDate = (slot.slotDate || selectedDate.value || '').trim()
  const slotTime = slot.startTime.trim()
  if (!slotDate || !slotTime) {
    return null
  }
  const normalizedTime = /^\d{2}:\d{2}$/.test(slotTime) ? `${slotTime}:00` : slotTime
  const slotStartAt = new Date(`${slotDate}T${normalizedTime}`)
  return Number.isNaN(slotStartAt.getTime()) ? null : slotStartAt
}

const isSlotWithinLeadTime = (slot: SpecialistAvailabilitySlot): boolean => {
  const slotStartAt = resolveSlotStartAt(slot)
  if (!slotStartAt) {
    return false
  }
  return slotStartAt.getTime() - Date.now() < BOOKING_MIN_LEAD_TIME_MS
}

const isSlotUnavailable = (slot: SpecialistAvailabilitySlot) =>
  slot.status !== 'AVAILABLE' || isSlotWithinLeadTime(slot)

const getSlotStatusLabel = (slot: SpecialistAvailabilitySlot) => {
  if (slot.status === 'BOOKED') {
    return 'Booked'
  }
  if (slot.status === 'LOCKED') {
    return 'Unavailable'
  }
  if (isSlotWithinLeadTime(slot)) {
    return 'Unavailable'
  }
  return 'Available'
}

const selectSlot = (slot: SpecialistAvailabilitySlot) => {
  if (slot.status !== 'AVAILABLE') {
    ElMessage.warning('This time slot has already been booked. Please choose another time slot.')
    return
  }
  if (isSlotWithinLeadTime(slot)) {
    ElMessage.warning(BOOKING_MIN_LEAD_TIME_WARNING)
    return
  }
  bookingForm.value.slotId = slot.id
  syncAiBookingPageContext()
}

const resetBookingForm = () => {
  bookingForm.value.slotId = null
  bookingForm.value.topic = ''
  bookingForm.value.customerNotes = ''
  syncAiBookingPageContext()
}

interface AiBookingDraftPayload {
  specialistId?: number | null
  slotId?: number | null
  topic?: string | null
  customerNotes?: string | null
  availableTopics?: string[]
  warnings?: string[]
}

const applyAiBookingDraft = (draft: AiBookingDraftPayload) => {
  const draftSpecialistId = Number(draft.specialistId)
  if (Number.isFinite(draftSpecialistId) && draftSpecialistId > 0 && draftSpecialistId !== specialistId.value) {
    ElMessage.warning(`AI draft is for specialist #${draftSpecialistId}. Please open that specialist booking page first.`)
    return
  }

  if (draft.topic && bookingTopics.value.includes(draft.topic)) {
    bookingForm.value.topic = draft.topic
  }

  if (typeof draft.customerNotes === 'string') {
    bookingForm.value.customerNotes = draft.customerNotes
  }

  const draftSlotId = Number(draft.slotId)
  if (Number.isFinite(draftSlotId) && draftSlotId > 0) {
    const matchedSlot = availability.value.find(slot => slot.id === draftSlotId && slot.status === 'AVAILABLE')
    if (matchedSlot) {
      bookingForm.value.slotId = draftSlotId
    } else {
      ElMessage.warning(`AI draft slot #${draftSlotId} is not available on the current date.`)
    }
  }

  if (Array.isArray(draft.warnings) && draft.warnings.length > 0) {
    ElMessage.warning(`AI draft warning: ${draft.warnings[0]}`)
  } else {
    ElMessage.success('AI draft has been applied to the booking form.')
  }
  syncAiBookingPageContext()
}

const onAiDraftEvent = (event: Event) => {
  const customEvent = event as CustomEvent<AiBookingDraftPayload>
  if (!customEvent.detail) {
    return
  }
  applyAiBookingDraft(customEvent.detail)
}

const submitBooking = async () => {
  if (bookingSubmitting.value) {
    duplicateSubmitClickCount.value += 1
    if (duplicateSubmitClickCount.value >= DUPLICATE_SUBMIT_CLICK_WARNING_THRESHOLD) {
      ElMessage.warning('You are clicking too fast. Please wait for the current booking request to finish.')
      duplicateSubmitClickCount.value = 0
    }
    return
  }

  if (!specialist.value) {
    return
  }
  if (!isSpecialistActive.value) {
    ElMessage.warning('This specialist is inactive and cannot accept bookings.')
    return
  }
  if (!bookingForm.value.slotId) {
    ElMessage.warning('Please choose a time slot first.')
    return
  }
  if (selectedSlot.value && isSlotWithinLeadTime(selectedSlot.value)) {
    ElMessage.warning(BOOKING_MIN_LEAD_TIME_WARNING)
    return
  }
  if (!bookingForm.value.topic.trim()) {
    ElMessage.warning('Please choose a booking topic.')
    return
  }
  if (notesFormatError.value) {
    ElMessage.warning(notesFormatError.value)
    return
  }

  duplicateSubmitClickCount.value = 0
  bookingSubmitting.value = true
  try {
    const createdBooking = await createBooking({
      specialistId: specialist.value.id,
      slotId: bookingForm.value.slotId,
      topic: bookingForm.value.topic.trim(),
      customerNotes: bookingForm.value.customerNotes.trim(),
    }, true)
    ElMessage.success(`Booking created successfully. Status: ${createdBooking.status}.`)
    resetBookingForm()
    await router.push({
      path: '/customer/bookings',
      query: { bookingId: String(createdBooking.bookingId) },
    })
  } catch (error: any) {
    const message = error?.message || 'Failed to create booking.'

    if (message.includes('Time slot already booked')) {
      await loadAvailability()
      ElMessage.warning('This time slot is no longer available. Your topic and notes were kept. Please choose another slot.')
      return
    }

    if (message.includes('Duplicate request')) {
      ElMessage.warning('Booking request is being processed. Please wait a moment.')
      return
    }

    if (message.includes('unsupported characters')) {
      ElMessage.warning('Notes contain unsupported characters. Please adjust them and try again.')
      return
    }

    ElMessage.error(message)
  } finally {
    bookingSubmitting.value = false
    duplicateSubmitClickCount.value = 0
  }
}

const goBack = () => {
  const from = typeof route.query.from === 'string' ? route.query.from : ''

  if (from) {
    router.push(from)
    return
  }

  router.push({
    name: 'CustomerSpecialistDetail',
    params: { id: specialistId.value },
    query: selectedDate.value ? { date: selectedDate.value } : undefined,
  })
}

const formatLevel = (level: string) => level.charAt(0).toUpperCase() + level.slice(1).toLowerCase()
const formatStatus = (status: string) =>
  status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()

onMounted(async () => {
  window.addEventListener(AI_BOOKING_DRAFT_EVENT, onAiDraftEvent as EventListener)
  restoreBookingFormDraft()
  syncAiBookingPageContext()
  await loadBookingTopics()
  if (bookingForm.value.topic && !bookingTopics.value.includes(bookingForm.value.topic)) {
    bookingForm.value.topic = ''
  }
  syncBookingFormDraft()
  syncAiBookingPageContext()
})

onBeforeUnmount(() => {
  window.removeEventListener(AI_BOOKING_DRAFT_EVENT, onAiDraftEvent as EventListener)
})

watch(
  () => [route.params.id, route.query.date],
  async () => {
    selectedDate.value =
      typeof route.query.date === 'string' && route.query.date ? route.query.date : toLocalDateString()
    bookingForm.value.slotId = null
    syncAiBookingPageContext()
    await loadDetail()
    await loadAvailability()
  },
  { immediate: true },
)

watch(selectedDate, async () => {
  syncAiBookingPageContext()
  if (specialist.value) {
    await loadAvailability()
  }
})

watch(
  () => [bookingForm.value.topic, bookingForm.value.customerNotes],
  () => {
    syncBookingFormDraft()
    syncAiBookingPageContext()
  }
)
</script>

<style scoped lang="scss">
.booking-page {
  display: grid;
  gap: var(--space-6);
}

.page-actions {
  display: flex;
  justify-content: flex-start;
}

.overview,
.page-loading,
.booking-card {
  padding: var(--space-6);
}

.overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
}

.overview-main {
  display: flex;
  align-items: center;
  gap: var(--space-5);
}

.identity {
  display: grid;
  gap: var(--space-3);
}

.identity h1,
.booking-head h2,
.availability-head h3 {
  margin: 0;
}

.eyebrow {
  margin: 0;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.price-panel {
  min-width: 260px;
  padding: var(--space-5);
  border-radius: var(--radius-lg);
  background: linear-gradient(
    145deg,
    rgba(var(--color-primary-rgb), 0.12),
    rgba(var(--color-primary-rgb), 0.02)
  );
  border: 1px solid var(--color-border);
  display: grid;
  gap: var(--space-3);
}

.panel-label {
  color: var(--color-text-secondary);
  text-transform: uppercase;
  font-size: 12px;
  letter-spacing: 0.08em;
}

.panel-price {
  font-size: 32px;
  color: var(--color-primary);
}

.panel-copy {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.booking-card {
  display: grid;
  gap: var(--space-5);
}

.booking-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  align-items: flex-start;
}

.booking-head p,
.availability-head p {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
}

.booking-layout {
  display: grid;
  gap: var(--space-5);
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.9fr);
  align-items: start;
}

.booking-main {
  display: grid;
  gap: var(--space-5);
}

.availability-panel {
  display: grid;
  gap: var(--space-4);
}

.availability-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  align-items: flex-start;
}

.slot-grid {
  display: grid;
  gap: var(--space-3);
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
}

.slot-chip {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
  display: grid;
  gap: var(--space-1);
  justify-items: center;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

.slot-chip:hover {
  transform: translateY(-1px);
}

.slot-chip.active {
  border-color: var(--color-primary);
  background: rgba(var(--color-primary-rgb), 0.1);
}

.slot-chip.unavailable {
  background: var(--color-bg-muted);
  border-color: var(--color-border);
  cursor: not-allowed;
  opacity: 0.85;
}

.slot-chip.unavailable:hover {
  transform: none;
}

.slot-chip span {
  color: var(--color-text-primary);
  font-weight: 700;
}

.slot-chip small {
  color: var(--color-text-secondary);
}

.slot-state {
  color: var(--color-text-secondary);
  font-size: 11px;
  font-style: normal;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.topic-select {
  width: 100%;
}

.form-error {
  margin: var(--space-2) 0 0;
  color: var(--color-danger);
  font-size: 13px;
}

.booking-summary {
  align-self: start;
  position: sticky;
  top: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.booking-summary h3 {
  margin: 0 0 var(--space-3);
  color: var(--color-text-primary);
  font-size: 16px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-4);
  padding: 8px 0;
  border-top: 1px solid var(--color-border);
}

.summary-row:first-of-type {
  border-top: none;
  padding-top: 0;
}

.summary-row span {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.summary-row strong {
  color: var(--color-text-primary);
  font-size: 14px;
  text-align: right;
}

.booking-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-4);
}

.booking-tip {
  color: var(--color-text-secondary);
  font-size: 14px;
}

@media (max-width: 960px) {
  .overview,
  .booking-head,
  .availability-head,
  .booking-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .booking-layout {
    grid-template-columns: 1fr;
  }

  .price-panel {
    width: 100%;
  }

  .booking-summary {
    position: static;
  }

  .summary-row {
    flex-direction: column;
    gap: var(--space-1);
  }

  .summary-row strong {
    text-align: left;
  }
}
</style>
