<template>
  <div class="schedule-dashboard">
    <div class="page-header">
      <h1 class="page-title">Schedule Management</h1>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          Add Time Slot
        </el-button>
      </div>
    </div>

    <!-- Week Navigation -->
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

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>Loading schedule...</span>
    </div>

    <!-- Schedule Grid -->
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
            :class="getSlotStatusClass(slot.status)"
            @click="openSlotDetail(slot)"
          >
            <div class="slot-time">
              {{ slot.startTime }} - {{ slot.endTime }}
            </div>
            <div class="slot-status">
              <el-tag :type="getStatusTagType(slot.status)" size="small">
                {{ slot.status }}
              </el-tag>
            </div>
            <div v-if="slot.recurringRuleId" class="slot-badge">
              <el-icon><Refresh /></el-icon>
            </div>
          </div>

          <div v-if="day.slots.length === 0" class="no-slots">
            <span>No slots</span>
            <el-button size="small" text @click="openCreateForDay(day.date)">
              Add slot
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Slot Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      title="Add Time Slot"
      width="500px"
      @close="resetCreateForm"
    >
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="120px">
        <el-form-item label="Date" prop="slotDate">
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
        <el-form-item label="Start Time" prop="startTime">
          <el-time-select
            v-model="createForm.startTime"
            start="08:00"
            step="00:30"
            end="18:00"
            placeholder="Select start time"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="End Time" prop="endTime">
          <el-time-select
            v-model="createForm.endTime"
            start="08:00"
            step="00:30"
            end="18:00"
            placeholder="Select end time"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          Create
        </el-button>
      </template>
    </el-dialog>

    <!-- Slot Detail Dialog -->
    <el-dialog
      v-model="showDetailDialog"
      title="Time Slot Details"
      width="500px"
    >
      <div v-if="selectedSlot" class="slot-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Date">
            {{ selectedSlot.slotDate }}
          </el-descriptions-item>
          <el-descriptions-item label="Time">
            {{ selectedSlot.startTime }} - {{ selectedSlot.endTime }}
          </el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="getStatusTagType(selectedSlot.status)">
              {{ selectedSlot.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedSlot.recurringRuleId" label="Recurring">
            <el-tag type="info">From Recurring Rule</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <div v-if="selectedSlot && selectedSlot.status !== 'BOOKED'">
          <el-button @click="showDetailDialog = false">Close</el-button>
          <el-button type="primary" @click="openUpdateDialog">
            Update
          </el-button>
          <el-button type="danger" @click="handleDelete">
            Delete
          </el-button>
        </div>
        <div v-else>
          <el-button @click="showDetailDialog = false">Close</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Update Slot Dialog -->
    <el-dialog
      v-model="showUpdateDialog"
      title="Update Time Slot"
      width="500px"
      @close="resetUpdateForm"
    >
      <el-form :model="updateForm" :rules="updateRules" ref="updateFormRef" label-width="120px">
        <el-form-item label="Start Time" prop="startTime">
          <el-time-select
            v-model="updateForm.startTime"
            start="08:00"
            step="00:30"
            end="18:00"
            placeholder="Select start time"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="End Time" prop="endTime">
          <el-time-select
            v-model="updateForm.endTime"
            start="08:00"
            step="00:30"
            end="18:00"
            placeholder="Select end time"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="Status" prop="status">
          <el-select v-model="updateForm.status" placeholder="Select status" style="width: 100%">
            <el-option label="Available" value="AVAILABLE" />
            <el-option label="Blocked" value="BLOCKED" />
            <el-option label="Unavailable" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpdateDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdate">
          Update
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, ArrowLeft, ArrowRight, Refresh, Loading } from '@element-plus/icons-vue'
import {
  getWeeklySchedule,
  createSlot,
  updateSlot,
  deleteSlot,
  type TimeSlotVO,
  type CreateSlotRequest,
  type UpdateSlotRequest
} from '@/api/schedule'

// ============== State ==============
const loading = ref(false)
const submitting = ref(false)
const slots = ref<TimeSlotVO[]>([])

const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const showUpdateDialog = ref(false)

const currentWeekStart = ref(getWeekStart(new Date()))
const selectedSlot = ref<TimeSlotVO | null>(null)

const createFormRef = ref<FormInstance>()
const updateFormRef = ref<FormInstance>()

const createForm = ref<CreateSlotRequest>({
  slotDate: '',
  startTime: '',
  endTime: ''
})

const updateForm = ref<UpdateSlotRequest>({
  startTime: '',
  endTime: '',
  status: ''
})

// ============== Validation Rules ==============
const validateTimeRange = (rule: any, value: any, callback: any) => {
  if (createForm.value.startTime && createForm.value.endTime) {
    if (createForm.value.startTime >= createForm.value.endTime) {
      callback(new Error('End time must be after start time'))
    } else {
      callback()
    }
  } else {
    callback()
  }
}

const createRules: FormRules = {
  slotDate: [
    { required: true, message: 'Please select a date', trigger: 'change' }
  ],
  startTime: [
    { required: true, message: 'Please select start time', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: 'Please select end time', trigger: 'change' },
    { validator: validateTimeRange, trigger: 'change' }
  ]
}

const updateRules: FormRules = {
  status: [
    { required: true, message: 'Please select status', trigger: 'change' }
  ]
}

// ============== Computed ==============
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
  for (let i = 0; i < 7; i++) {
    const date = new Date(currentWeekStart.value)
    date.setDate(date.getDate() + i)
    const dateStr = formatDate(date)
    const daySlots = slots.value.filter(s => s.slotDate === dateStr)
    
    days.push({
      date: dateStr,
      dayName: date.toLocaleDateString('en-US', { weekday: 'short' }),
      dateLabel: date.getDate().toString(),
      isToday: isToday(date),
      slots: daySlots.sort((a, b) => a.startTime.localeCompare(b.startTime))
    })
  }
  return days
})

// ============== Methods ==============
function getWeekStart(date: Date): Date {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  d.setDate(diff)
  d.setHours(0, 0, 0, 0)
  return d
}

function formatDate(date: Date): string {
  return date.toISOString().split('T')[0] as string
}

function isToday(date: Date): boolean {
  const today = new Date()
  return date.toDateString() === today.toDateString()
}

function disabledPastDates(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

async function fetchSchedule() {
  loading.value = true
  try {
    const weekStartStr = formatDate(currentWeekStart.value)
    slots.value = await getWeeklySchedule(weekStartStr)
  } catch (error) {
    console.error('Failed to fetch schedule:', error)
  } finally {
    loading.value = false
  }
}

function prevWeek() {
  const newStart = new Date(currentWeekStart.value)
  newStart.setDate(newStart.getDate() - 7)
  currentWeekStart.value = newStart
}

function nextWeek() {
  const newStart = new Date(currentWeekStart.value)
  newStart.setDate(newStart.getDate() + 7)
  currentWeekStart.value = newStart
}

function goToToday() {
  currentWeekStart.value = getWeekStart(new Date())
}

function openSlotDetail(slot: TimeSlotVO) {
  selectedSlot.value = slot
  showDetailDialog.value = true
}

function openCreateForDay(date: string) {
  createForm.value.slotDate = date
  showCreateDialog.value = true
}

function openUpdateDialog() {
  if (selectedSlot.value) {
    updateForm.value = {
      startTime: selectedSlot.value.startTime,
      endTime: selectedSlot.value.endTime,
      status: selectedSlot.value.status
    }
  }
  showDetailDialog.value = false
  showUpdateDialog.value = true
}

function resetCreateForm() {
  createForm.value = { slotDate: '', startTime: '', endTime: '' }
  createFormRef.value?.resetFields()
}

function resetUpdateForm() {
  updateForm.value = { startTime: '', endTime: '', status: '' }
  updateFormRef.value?.resetFields()
}

async function handleCreate() {
  if (!createFormRef.value) return
  
  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await createSlot(createForm.value)
        ElMessage.success('Time slot created successfully')
        showCreateDialog.value = false
        resetCreateForm()
        await fetchSchedule()
      } catch (error) {
        console.error('Failed to create slot:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

async function handleUpdate() {
  if (!selectedSlot.value) return
  
  submitting.value = true
  try {
    await updateSlot(selectedSlot.value.id, updateForm.value)
    ElMessage.success('Time slot updated successfully')
    showUpdateDialog.value = false
    resetUpdateForm()
    await fetchSchedule()
  } catch (error) {
    console.error('Failed to update slot:', error)
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  if (!selectedSlot.value) return
  
  try {
    await ElMessageBox.confirm(
      'Are you sure you want to delete this time slot?',
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    
    await deleteSlot(selectedSlot.value.id)
    ElMessage.success('Time slot deleted successfully')
    showDetailDialog.value = false
    await fetchSchedule()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to delete slot:', error)
    }
  }
}

function getSlotStatusClass(status: string): string {
  const statusMap: Record<string, string> = {
    AVAILABLE: 'status-available',
    BOOKED: 'status-booked',
    BLOCKED: 'status-blocked',
    UNAVAILABLE: 'status-unavailable'
  }
  return statusMap[status] || ''
}

function getStatusTagType(status: string): any {
  const typeMap: Record<string, string> = {
    AVAILABLE: 'success',
    BOOKED: 'warning',
    BLOCKED: 'danger',
    UNAVAILABLE: 'info'
  }
  return typeMap[status] || 'info'
}

// ============== Lifecycle ==============
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
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
    min-width: 200px;
    text-align: center;
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
    background: var(--color-bg-overlay);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    overflow: hidden;
    min-height: 400px;

    &.is-today .day-header {
      background: var(--color-primary-soft);
      color: var(--color-text-inverse);
    }
  }

  .day-header {
    padding: var(--space-3);
    background: var(--color-bg-muted);
    text-align: center;
    border-bottom: 1px solid var(--color-border);
  }

  .day-name {
    display: block;
    font-weight: 600;
    font-size: 14px;
  }

  .day-date {
    display: block;
    font-size: 12px;
    opacity: 0.8;
    margin-top: 2px;
  }

  .day-slots {
    padding: var(--space-2);
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    min-height: 350px;
  }

  .time-slot-card {
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--transition-fast);
    position: relative;
    font-size: 12px;

    &.status-available {
      background: rgba(103, 194, 58, 0.15);
      border: 1px solid rgba(103, 194, 58, 0.3);
      color: var(--color-text-primary);
    }

    &.status-booked {
      background: rgba(230, 162, 60, 0.15);
      border: 1px solid rgba(230, 162, 60, 0.3);
      color: var(--color-text-primary);
    }

    &.status-blocked {
      background: rgba(245, 108, 108, 0.15);
      border: 1px solid rgba(245, 108, 108, 0.3);
      color: var(--color-text-primary);
    }

    &.status-unavailable {
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
    font-weight: 600;
    margin-bottom: 4px;
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
    padding: var(--space-4);
    color: var(--color-text-secondary);
    font-size: 13px;
    gap: var(--space-2);
  }

  .slot-detail {
    padding: var(--space-2);
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
