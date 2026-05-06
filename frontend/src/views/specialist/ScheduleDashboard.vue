<template>
  <div class="schedule-dashboard">
    <div class="page-header">
      <h1 class="page-title">Schedule Management</h1>
      <div class="header-actions">
        <el-button @click="goToRecurringRules">Recurring Rules</el-button>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          Add Time Slot
        </el-button>
      </div>
    </div>

    <div class="week-navigator">
      <el-button circle @click="prevWeek">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="week-label">{{ weekLabel }}</span>
      <el-button circle @click="nextWeek">
        <el-icon><ArrowRight /></el-icon>
      </el-button>
      <el-button @click="goToToday">Today</el-button>
    </div>

    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>Loading schedule...</span>
    </div>

    <div v-else class="schedule-grid">
      <div
        v-for="day in weekDays"
        :key="day.date"
        class="day-column"
        :class="{ 'is-today': day.isToday }"
      >
        <div class="day-header">
          <span class="day-name">{{ day.dayName }}</span>
          <span class="day-date">{{ day.dateLabel }}</span>
        </div>

        <div class="day-slots">
          <div
            v-for="slot in day.slots"
            :key="slot.id"
            class="time-slot-card"
            :class="getSlotStatusClass(slot)"
            @click="openSlotDetail(slot)"
          >
            <div class="slot-time">
              {{ formatDisplayTime(slot.startTime) }} - {{ formatDisplayTime(slot.endTime) }}
            </div>
            <div class="slot-status">
              <el-tag :type="getStatusTagType(slot)" size="small">
                {{ getSlotDisplayLabel(slot) }}
              </el-tag>
            </div>
            <div v-if="slot.customerName" class="slot-customer">{{ slot.customerName }}</div>
            <div v-if="slot.recurringRuleId" class="slot-badge">
              <el-icon><Refresh /></el-icon>
            </div>
          </div>

          <div v-if="day.slots.length === 0" class="no-slots">
            <span>No slots</span>
            <el-button v-if="!isPastDate(day.date)" size="small" text @click="openCreateForDay(day.date)">
              Add slot
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showCreateDialog"
      title="Add Time Slot"
      width="500px"
      @close="resetCreateForm"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
        <el-form-item :label="createForm.isRecurring ? 'Start Date' : 'Date'" prop="slotDate">
          <el-date-picker
            v-model="createForm.slotDate"
            type="date"
            placeholder="Select date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledPastDates"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="Repeat Weekly">
          <el-switch
            v-model="createForm.isRecurring"
            inline-prompt
            active-text="Yes"
            inactive-text="No"
          />
        </el-form-item>
        <el-form-item label="Start Time" prop="startTime">
          <el-time-picker
            v-model="createForm.startTime"
            format="HH:mm"
            value-format="HH:mm:ss"
            placeholder="Select start time"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="End Time" prop="endTime">
          <el-time-picker
            v-model="createForm.endTime"
            format="HH:mm"
            value-format="HH:mm:ss"
            placeholder="Select end time"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="createForm.isRecurring" label="Repeat Until" prop="effectiveEndDate">
          <div class="repeat-end-controls">
            <el-switch
              v-model="createForm.noEndDate"
              active-text="No end date"
              @change="handleRecurringNoEndDateChange"
            />
          </div>
          <el-date-picker
            v-model="createForm.effectiveEndDate"
            type="date"
            placeholder="Select end date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledRepeatEndDates"
            :disabled="createForm.noEndDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="createForm.isRecurring && createForm.slotDate" label="Pattern">
          <p class="repeat-summary">
            Repeats every {{ getDayNameForDate(createForm.slotDate) }} from
            {{ createForm.slotDate }}
            <template v-if="createForm.noEndDate">with no end date.</template>
            <template v-else-if="createForm.effectiveEndDate">
              until {{ createForm.effectiveEndDate }}.
            </template>
            <template v-else>until the date you choose.</template>
          </p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate"> Create </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="Time Slot Details" width="500px">
      <div v-if="selectedSlot" class="slot-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Date">{{ selectedSlot.slotDate }}</el-descriptions-item>
          <el-descriptions-item label="Time">
            {{ formatDisplayTime(selectedSlot.startTime) }} -
            {{ formatDisplayTime(selectedSlot.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="getStatusTagType(selectedSlot)">
              {{ getSlotDisplayLabel(selectedSlot) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedSlot.customerName" label="Customer">
            {{ selectedSlot.customerName }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedSlot.customerEmail" label="Customer Email">
            {{ selectedSlot.customerEmail }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedSlot.recurringRuleId" label="Recurring">
            <el-tag type="info">Generated by recurring rule</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <p v-if="selectedSlot?.recurringRuleId && canEditSlot(selectedSlot)" class="detail-tip">
          Manual changes to this generated slot only affect this occurrence. Use Delete Recurring
          Rule to remove the weekly pattern and its generated available slots.
        </p>
        <p v-else-if="selectedSlot && !canEditSlot(selectedSlot)" class="detail-tip">
          {{ getSlotRestrictionMessage(selectedSlot) }}
        </p>
      </div>
      <template #footer>
        <div v-if="selectedSlot && canEditSlot(selectedSlot)">
          <el-button @click="showDetailDialog = false">Close</el-button>
          <el-button type="primary" @click="openUpdateDialog">Update</el-button>
          <el-button v-if="selectedSlot.recurringRuleId" plain type="danger" @click="handleDelete">
            Delete This Occurrence
          </el-button>
          <el-button
            v-if="selectedSlot.recurringRuleId"
            type="danger"
            @click="handleDeleteRecurringRule"
          >
            Delete Recurring Rule
          </el-button>
          <el-button v-else type="danger" @click="handleDelete">Delete</el-button>
        </div>
        <div v-else-if="selectedSlot && canForceCancelSlot(selectedSlot)">
          <el-button @click="showDetailDialog = false">Close</el-button>
          <el-button type="danger" @click="openForceCancelDialog">Force Cancel Booking</el-button>
        </div>
        <div v-else>
          <el-button @click="showDetailDialog = false">Close</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showUpdateDialog"
      title="Update Time Slot"
      width="500px"
      @close="resetUpdateForm"
    >
      <el-form ref="updateFormRef" :model="updateForm" :rules="updateRules" label-width="120px">
        <el-form-item label="Start Time" prop="startTime">
          <el-time-picker
            v-model="updateForm.startTime"
            format="HH:mm"
            value-format="HH:mm:ss"
            placeholder="Select start time"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="End Time" prop="endTime">
          <el-time-picker
            v-model="updateForm.endTime"
            format="HH:mm"
            value-format="HH:mm:ss"
            placeholder="Select end time"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpdateDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdate"> Update </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showForceCancelDialog" title="Force Cancel Booking" width="560px">
      <el-form label-position="top">
        <el-form-item label="Cancel Reason" required>
          <el-input
            v-model="forceCancelForm.cancelReason"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="Please provide cancellation reason."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForceCancelDialog = false">Cancel</el-button>
        <el-button type="primary" @click="goToForceCancelOptionStep"> Confirm </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showForceCancelOptionDialog" title="Slot Handling Option" width="560px">
      <el-form label-position="top">
        <el-form-item label="After cancellation" required>
          <el-radio-group v-model="forceCancelForm.releaseSlot">
            <el-radio :value="true">Release this slot for new bookings</el-radio>
            <el-radio :value="false">Keep this slot locked</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="backToForceCancelReasonStep">Back</el-button>
        <el-button type="danger" :loading="submitting" @click="handleForceCancel">
          Confirm Cancellation
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, ArrowRight, Loading, Plus, Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { specialistForceCancelBooking } from '@/api/booking'
import {
  createRecurringRule,
  createSlot,
  deleteRecurringRule,
  deleteSlot,
  getWeeklySchedule,
  updateSlot,
  type TimeSlotVO,
  type UpdateSlotRequest,
} from '@/api/schedule'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const slots = ref<TimeSlotVO[]>([])

const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const showUpdateDialog = ref(false)
const showForceCancelDialog = ref(false)
const showForceCancelOptionDialog = ref(false)

const currentWeekStart = ref(getWeekStart(new Date()))
const selectedSlot = ref<TimeSlotVO | null>(null)

const createFormRef = ref<FormInstance>()
const updateFormRef = ref<FormInstance>()

interface CreateSlotForm {
  slotDate: string
  startTime: string
  endTime: string
  isRecurring: boolean
  noEndDate: boolean
  effectiveEndDate: string
}

const createForm = ref<CreateSlotForm>({
  slotDate: '',
  startTime: '',
  endTime: '',
  isRecurring: false,
  noEndDate: true,
  effectiveEndDate: '',
})

const updateForm = ref<UpdateSlotRequest>({
  startTime: '',
  endTime: '',
})

const forceCancelForm = ref({
  cancelReason: '',
  releaseSlot: true,
})

const validateCreateTimeRange = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void,
) => {
  if (
    createForm.value.startTime &&
    createForm.value.endTime &&
    createForm.value.startTime >= createForm.value.endTime
  ) {
    callback(new Error('End time must be after start time'))
    return
  }
  callback()
}

const validateCreateStartTime = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void,
) => {
  const slotStartAt = resolveSlotStartAt(createForm.value.slotDate, createForm.value.startTime)
  if (slotStartAt && slotStartAt.getTime() <= Date.now()) {
    callback(new Error('Start time must be in the future'))
    return
  }
  callback()
}

const validateUpdateTimeRange = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void,
) => {
  if (
    updateForm.value.startTime &&
    updateForm.value.endTime &&
    updateForm.value.startTime >= updateForm.value.endTime
  ) {
    callback(new Error('End time must be after start time'))
    return
  }
  callback()
}

const validateUpdateStartTime = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void,
) => {
  const slotDate = selectedSlot.value?.slotDate || ''
  const slotStartAt = resolveSlotStartAt(slotDate, updateForm.value.startTime || '')
  if (slotStartAt && slotStartAt.getTime() <= Date.now()) {
    callback(new Error('Start time must be in the future'))
    return
  }
  callback()
}

const createRules: FormRules = {
  slotDate: [{ required: true, message: 'Please select a date', trigger: 'change' }],
  startTime: [
    { required: true, message: 'Please select start time', trigger: 'change' },
    { validator: validateCreateStartTime, trigger: 'change' },
  ],
  endTime: [
    { required: true, message: 'Please select end time', trigger: 'change' },
    { validator: validateCreateTimeRange, trigger: 'change' },
  ],
  effectiveEndDate: [
    {
      validator: (_rule, value, callback) => {
        if (!createForm.value.isRecurring || createForm.value.noEndDate || value) {
          callback()
          return
        }
        callback(new Error('Please select an end date or choose no end date'))
      },
      trigger: 'change',
    },
  ],
}

const updateRules: FormRules = {
  startTime: [
    { required: true, message: 'Please select start time', trigger: 'change' },
    { validator: validateUpdateStartTime, trigger: 'change' },
  ],
  endTime: [
    { required: true, message: 'Please select end time', trigger: 'change' },
    { validator: validateUpdateTimeRange, trigger: 'change' },
  ],
}

const weekLabel = computed(() => {
  const start = currentWeekStart.value
  const end = new Date(start)
  end.setDate(end.getDate() + 6)
  const startMonth = start.toLocaleDateString('en-US', { month: 'short' })
  const endMonth = end.toLocaleDateString('en-US', { month: 'short' })
  const year = start.getFullYear()
  if (startMonth === endMonth) {
    return `${startMonth} ${start.getDate()} - ${end.getDate()}, ${year}`
  }
  return `${startMonth} ${start.getDate()} - ${endMonth} ${end.getDate()}, ${year}`
})

const weekDays = computed(() => {
  const days = []
  for (let index = 0; index < 7; index += 1) {
    const date = new Date(currentWeekStart.value)
    date.setDate(date.getDate() + index)
    const dateStr = formatDate(date)
    const daySlots = slots.value.filter((slot) => slot.slotDate === dateStr)

    days.push({
      date: dateStr,
      dayName: date.toLocaleDateString('en-US', { weekday: 'short' }),
      dateLabel: date.getDate().toString(),
      isToday: isToday(date),
      slots: daySlots.sort((left, right) => left.startTime.localeCompare(right.startTime)),
    })
  }
  return days
})

function getWeekStart(date: Date): Date {
  const copy = new Date(date)
  const day = copy.getDay()
  const diff = copy.getDate() - day + (day === 0 ? -6 : 1)
  copy.setDate(diff)
  copy.setHours(0, 0, 0, 0)
  return copy
}

function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function parseLocalDate(dateStr: string): Date {
  const [year, month, day] = dateStr.split('-').map(Number)
  if (!year || !month || !day) {
    return new Date()
  }
  return new Date(year, month - 1, day)
}

function getDayOfWeekFromDateString(dateStr: string): number {
  const day = parseLocalDate(dateStr).getDay()
  return day === 0 ? 7 : day
}

function getDayNameForDate(dateStr: string): string {
  return parseLocalDate(dateStr).toLocaleDateString('en-US', { weekday: 'long' })
}

function normalizeTimeValue(time: string): string {
  return /^\d{2}:\d{2}$/.test(time) ? `${time}:00` : time
}

function formatDisplayTime(time: string): string {
  if (!time) {
    return ''
  }
  const match = time.match(/^(\d{2}:\d{2})/)
  return match && match[1] ? match[1] : time || ''
}

function resolveSlotStartAt(slotDate: string, startTime: string): Date | null {
  if (!slotDate || !startTime) {
    return null
  }

  const slotStartAt = new Date(`${slotDate}T${normalizeTimeValue(startTime)}`)
  return Number.isNaN(slotStartAt.getTime()) ? null : slotStartAt
}

function isToday(date: Date): boolean {
  return date.toDateString() === new Date().toDateString()
}

function isPastDate(dateStr: string): boolean {
  const targetDate = parseLocalDate(dateStr)
  targetDate.setHours(0, 0, 0, 0)

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return targetDate < today
}

function disabledPastDates(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

function disabledRepeatEndDates(date: Date): boolean {
  if (!createForm.value.slotDate) {
    return disabledPastDates(date)
  }
  const startDate = parseLocalDate(createForm.value.slotDate)
  startDate.setHours(0, 0, 0, 0)
  return date < startDate
}

function handleRecurringNoEndDateChange(value: string | number | boolean) {
  if (value) {
    createForm.value.effectiveEndDate = ''
  }
}

function getSlotStatusKey(slot: TimeSlotVO): string {
  return slot.bookingStatus || slot.status
}

function getSlotDisplayLabel(slot: TimeSlotVO): string {
  return slot.bookingStatusDesc || slot.statusDesc || slot.bookingStatus || slot.status
}

function hasSlotStarted(slot: TimeSlotVO): boolean {
  const slotStartAt = resolveSlotStartAt(slot.slotDate, slot.startTime)
  return slotStartAt ? slotStartAt.getTime() <= Date.now() : false
}

function canEditSlot(slot: TimeSlotVO): boolean {
  return slot.status === 'AVAILABLE' && !slot.bookingStatus && !hasSlotStarted(slot)
}

function canForceCancelSlot(slot: TimeSlotVO): boolean {
  if (!slot.bookingId) {
    return false
  }
  if (hasSlotStarted(slot)) {
    return false
  }
  return slot.bookingStatus === 'PENDING' || slot.bookingStatus === 'CONFIRMED'
}

function getSlotRestrictionMessage(slot: TimeSlotVO): string {
  if (hasSlotStarted(slot)) {
    return 'This time slot has already started or passed and can no longer be changed.'
  }
  if (slot.bookingStatus) {
    return 'This slot already has a booking and cannot be edited or deleted.'
  }
  return 'Only future available time slots can be changed.'
}

function getSlotStatusClass(slot: TimeSlotVO): string {
  const statusMap: Record<string, string> = {
    AVAILABLE: 'status-available',
    PENDING: 'status-pending',
    CONFIRMED: 'status-confirmed',
    COMPLETED: 'status-completed',
    BOOKED: 'status-booked',
    LOCKED: 'status-locked',
  }
  return statusMap[getSlotStatusKey(slot)] || ''
}

function getStatusTagType(slot: TimeSlotVO): string {
  const typeMap: Record<string, string> = {
    AVAILABLE: 'success',
    PENDING: 'warning',
    CONFIRMED: 'primary',
    COMPLETED: 'success',
    BOOKED: 'warning',
    LOCKED: 'info',
  }
  return typeMap[getSlotStatusKey(slot)] || 'info'
}

async function fetchSchedule() {
  loading.value = true
  try {
    slots.value = await getWeeklySchedule(formatDate(currentWeekStart.value))
  } finally {
    loading.value = false
  }
}

function prevWeek() {
  const next = new Date(currentWeekStart.value)
  next.setDate(next.getDate() - 7)
  currentWeekStart.value = next
}

function nextWeek() {
  const next = new Date(currentWeekStart.value)
  next.setDate(next.getDate() + 7)
  currentWeekStart.value = next
}

function goToToday() {
  currentWeekStart.value = getWeekStart(new Date())
}

function goToRecurringRules() {
  router.push('/specialist/rules')
}

function openSlotDetail(slot: TimeSlotVO) {
  selectedSlot.value = slot
  showDetailDialog.value = true
}

function openCreateForDay(date: string) {
  if (isPastDate(date)) {
    ElMessage.warning('Past dates cannot be modified')
    return
  }
  createForm.value.slotDate = date
  showCreateDialog.value = true
}

function openUpdateDialog() {
  if (!selectedSlot.value || !canEditSlot(selectedSlot.value)) {
    return
  }
  updateForm.value = {
    startTime: selectedSlot.value.startTime,
    endTime: selectedSlot.value.endTime,
  }
  showDetailDialog.value = false
  showUpdateDialog.value = true
}

function openForceCancelDialog() {
  if (!selectedSlot.value || !canForceCancelSlot(selectedSlot.value)) {
    return
  }
  forceCancelForm.value = {
    cancelReason: '',
    releaseSlot: true,
  }
  showForceCancelOptionDialog.value = false
  showForceCancelDialog.value = true
}

function goToForceCancelOptionStep() {
  const reason = forceCancelForm.value.cancelReason.trim()
  if (!reason) {
    ElMessage.warning('Cancel reason is required')
    return
  }
  if (reason.length > 300) {
    ElMessage.warning('Cancel reason must be within 300 characters')
    return
  }
  forceCancelForm.value.cancelReason = reason
  showForceCancelDialog.value = false
  showForceCancelOptionDialog.value = true
}

function backToForceCancelReasonStep() {
  showForceCancelOptionDialog.value = false
  showForceCancelDialog.value = true
}

function resetCreateForm() {
  createFormRef.value?.resetFields()
  createForm.value = {
    slotDate: '',
    startTime: '',
    endTime: '',
    isRecurring: false,
    noEndDate: true,
    effectiveEndDate: '',
  }
}

function resetUpdateForm() {
  updateFormRef.value?.resetFields()
  updateForm.value = { startTime: '', endTime: '' }
}

async function handleCreate() {
  if (!createFormRef.value) {
    return
  }
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const slotDate = createForm.value.slotDate
    if (createForm.value.isRecurring) {
      await createRecurringRule({
        dayOfWeek: getDayOfWeekFromDateString(slotDate),
        effectiveStartDate: slotDate,
        startTime: createForm.value.startTime,
        endTime: createForm.value.endTime,
        effectiveEndDate: createForm.value.noEndDate ? null : createForm.value.effectiveEndDate,
      })
      ElMessage.success('Recurring schedule created successfully')
    } else {
      await createSlot({
        slotDate,
        startTime: createForm.value.startTime,
        endTime: createForm.value.endTime,
      })
      ElMessage.success('Time slot created successfully')
    }
    showCreateDialog.value = false
    resetCreateForm()
    currentWeekStart.value = getWeekStart(parseLocalDate(slotDate))
  } finally {
    submitting.value = false
  }
}

async function handleUpdate() {
  if (!selectedSlot.value) {
    return
  }
  if (!canEditSlot(selectedSlot.value)) {
    ElMessage.warning(getSlotRestrictionMessage(selectedSlot.value))
    return
  }
  if (!updateFormRef.value) {
    return
  }
  const valid = await updateFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await updateSlot(selectedSlot.value.id, updateForm.value)
    ElMessage.success('Time slot updated successfully')
    showUpdateDialog.value = false
    resetUpdateForm()
    await fetchSchedule()
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  if (!selectedSlot.value) {
    return
  }
  if (!canEditSlot(selectedSlot.value)) {
    ElMessage.warning(getSlotRestrictionMessage(selectedSlot.value))
    return
  }

  try {
    const confirmMessage = selectedSlot.value.recurringRuleId
      ? 'Are you sure you want to delete only this recurring occurrence?'
      : 'Are you sure you want to delete this available time slot?'
    await ElMessageBox.confirm(confirmMessage, 'Confirm Delete', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning',
    })
    await deleteSlot(selectedSlot.value.id)
    ElMessage.success(
      selectedSlot.value.recurringRuleId
        ? 'Recurring occurrence deleted successfully'
        : 'Time slot deleted successfully',
    )
    showDetailDialog.value = false
    await fetchSchedule()
  } catch (error: unknown) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('Failed to delete slot:', error)
    }
  }
}

async function handleDeleteRecurringRule() {
  if (!selectedSlot.value?.recurringRuleId) {
    return
  }

  try {
    await ElMessageBox.confirm(
      'Are you sure you want to delete this recurring rule? This removes the weekly pattern and its generated available time slots. Booked or locked generated slots must be cleared first.',
      'Confirm Delete Recurring Rule',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' },
    )
    await deleteRecurringRule(selectedSlot.value.recurringRuleId)
    ElMessage.success('Recurring rule deleted successfully')
    showDetailDialog.value = false
    await fetchSchedule()
  } catch (error: unknown) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('Failed to delete recurring rule:', error)
    }
  }
}

async function handleForceCancel() {
  if (!selectedSlot.value || !selectedSlot.value.bookingId) {
    return
  }
  const reason = forceCancelForm.value.cancelReason.trim()
  if (!reason) {
    ElMessage.warning('Cancel reason is required')
    return
  }
  if (reason.length > 300) {
    ElMessage.warning('Cancel reason must be within 300 characters')
    return
  }

  submitting.value = true
  try {
    await specialistForceCancelBooking(selectedSlot.value.bookingId, {
      cancelReason: reason,
      releaseSlot: forceCancelForm.value.releaseSlot,
    })
    ElMessage.success('Booking cancelled successfully')
    showForceCancelOptionDialog.value = false
    showForceCancelDialog.value = false
    showDetailDialog.value = false
    await fetchSchedule()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchSchedule()
})

watch(currentWeekStart, () => {
  fetchSchedule()
})
</script>

<style scoped lang="scss">
.schedule-dashboard {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-6);
  }

  .page-title {
    font-size: 24px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
  }

  .header-actions {
    display: flex;
    gap: var(--space-3);
  }

  .week-navigator {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-4);
    margin-bottom: var(--space-6);
    padding: var(--space-4);
    background: var(--color-bg-overlay);
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
  }

  .week-label {
    min-width: 200px;
    text-align: center;
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .loading-container {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-3);
    padding: var(--space-8);
    color: var(--color-text-secondary);
  }

  .schedule-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: var(--space-3);
    min-height: 400px;
  }

  .day-column {
    min-height: 400px;
    overflow: hidden;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background: var(--color-bg-overlay);

    &.is-today .day-header {
      background: var(--color-primary-soft);
      color: var(--color-text-inverse);
    }
  }

  .day-header {
    padding: var(--space-3);
    text-align: center;
    background: var(--color-bg-muted);
    border-bottom: 1px solid var(--color-border);
  }

  .day-name {
    display: block;
    font-size: 14px;
    font-weight: 600;
  }

  .day-date {
    display: block;
    margin-top: 2px;
    font-size: 12px;
    opacity: 0.8;
  }

  .day-slots {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    min-height: 350px;
    padding: var(--space-2);
  }

  .time-slot-card {
    position: relative;
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--transition-fast);
    font-size: 12px;

    &.status-available {
      background: rgba(103, 194, 58, 0.15);
      border: 1px solid rgba(103, 194, 58, 0.3);
    }

    &.status-pending,
    &.status-booked {
      background: rgba(230, 162, 60, 0.15);
      border: 1px solid rgba(230, 162, 60, 0.3);
    }

    &.status-confirmed {
      background: rgba(64, 158, 255, 0.14);
      border: 1px solid rgba(64, 158, 255, 0.28);
    }

    &.status-completed {
      background: rgba(103, 194, 58, 0.14);
      border: 1px solid rgba(103, 194, 58, 0.28);
    }

    &.status-locked {
      background: var(--color-bg-muted);
      border: 1px solid var(--color-border);
      color: var(--color-text-secondary);
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px var(--color-shadow);
    }
  }

  .slot-time {
    margin-bottom: 4px;
    font-weight: 600;
  }

  .slot-customer {
    margin-top: 4px;
    overflow: hidden;
    color: var(--color-text-secondary);
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .slot-badge {
    position: absolute;
    top: var(--space-1);
    right: var(--space-1);
    color: var(--color-primary);
  }

  .no-slots {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: var(--space-2);
    padding: var(--space-4);
    color: var(--color-text-secondary);
    font-size: 13px;
  }

  .slot-detail {
    padding: var(--space-2);
  }

  .detail-tip {
    margin: 12px 0 0;
    color: var(--color-text-secondary);
    font-size: 13px;
  }

  .repeat-end-controls {
    margin-bottom: var(--space-3);
  }

  .repeat-summary {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: 13px;
    line-height: 1.5;
  }
}

@media (max-width: 1200px) {
  .schedule-dashboard .schedule-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .schedule-dashboard .schedule-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
