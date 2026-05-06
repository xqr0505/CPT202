<template>
  <div class="recurring-rules">
    <div class="page-header">
      <h1 class="page-title">Recurring Availability Rules</h1>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          Create Recurring Rule
        </el-button>
      </div>
    </div>

    <!-- Description -->
    <div class="info-card">
      <el-icon><InfoFilled /></el-icon>
      <span>
        Recurring rules repeat weekly from the start date you choose. If you later edit or delete
        one generated slot in the schedule view, only that occurrence changes. To remove the whole
        rule, delete it here after clearing any booked or locked generated slots.
      </span>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>Loading rules...</span>
    </div>

    <!-- Rules List -->
    <div v-else-if="rules.length > 0" class="rules-list">
      <el-card v-for="rule in rules" :key="rule.id" class="rule-card">
        <div class="rule-content">
          <div class="rule-main">
            <div class="rule-day">
              <el-tag type="primary" size="large">{{ rule.dayOfWeekDesc }}</el-tag>
            </div>
            <div class="rule-time">
              <span class="time-range">
                {{ formatDisplayTime(rule.startTime) }} - {{ formatDisplayTime(rule.endTime) }}
              </span>
            </div>
          </div>
          <div class="rule-meta">
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>Starts {{ formatDate(rule.effectiveStartDate) }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>{{
                rule.effectiveEndDate ? `Until ${formatDate(rule.effectiveEndDate)}` : 'No end date'
              }}</span>
            </div>
            <div class="meta-item">
              <el-tag :type="rule.isActive === 1 ? 'success' : 'info'" size="small">
                {{ rule.statusDesc }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="rule-actions">
          <el-button type="danger" size="small" @click="handleDelete(rule.id)"> Delete </el-button>
        </div>
      </el-card>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <el-empty description="No recurring rules yet">
        <el-button type="primary" @click="showCreateDialog = true">
          Create Your First Rule
        </el-button>
      </el-empty>
    </div>

    <!-- Create Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      title="Create Recurring Availability Rule"
      width="500px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rulesForm" ref="formRef" label-width="140px">
        <el-form-item label="Start Date" prop="effectiveStartDate">
          <el-date-picker
            v-model="form.effectiveStartDate"
            type="date"
            placeholder="Select start date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledPastDates"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="Day of Week" prop="dayOfWeek">
          <el-select v-model="form.dayOfWeek" placeholder="Select day" style="width: 100%">
            <el-option label="Monday" :value="1" />
            <el-option label="Tuesday" :value="2" />
            <el-option label="Wednesday" :value="3" />
            <el-option label="Thursday" :value="4" />
            <el-option label="Friday" :value="5" />
            <el-option label="Saturday" :value="6" />
            <el-option label="Sunday" :value="7" />
          </el-select>
        </el-form-item>

        <el-form-item label="Start Time" prop="startTime">
          <el-time-picker
            v-model="form.startTime"
            format="HH:mm"
            value-format="HH:mm:ss"
            placeholder="Select start time"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="End Time" prop="endTime">
          <el-time-picker
            v-model="form.endTime"
            format="HH:mm"
            value-format="HH:mm:ss"
            placeholder="Select end time"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="Effective Until" prop="effectiveEndDate">
          <div class="end-date-toggle">
            <el-switch
              v-model="form.noEndDate"
              active-text="No end date"
              @change="handleNoEndDateChange"
            />
          </div>
          <el-date-picker
            v-model="form.effectiveEndDate"
            type="date"
            placeholder="Select end date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledRepeatEndDates"
            :disabled="form.noEndDate"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          Create Rule
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, InfoFilled, Calendar, Loading } from '@element-plus/icons-vue'
import {
  getAllRecurringRules,
  createRecurringRule,
  deleteRecurringRule,
  type CreateRecurringRuleRequest,
  type RecurringRuleVO,
} from '@/api/schedule'

// ============== State ==============
const loading = ref(false)
const submitting = ref(false)
const rules = ref<RecurringRuleVO[]>([])
const showCreateDialog = ref(false)
const formRef = ref<FormInstance>()

interface RecurringRuleForm {
  effectiveStartDate: string
  dayOfWeek: number | null
  startTime: string
  endTime: string
  noEndDate: boolean
  effectiveEndDate: string
}

const form = ref<RecurringRuleForm>({
  effectiveStartDate: toDateInputValue(new Date()),
  dayOfWeek: 1,
  startTime: '',
  endTime: '',
  noEndDate: true,
  effectiveEndDate: '',
})

// ============== Validation Rules ==============
const validateTimeRange = (rule: any, value: any, callback: any) => {
  if (form.value.startTime && form.value.endTime) {
    if (form.value.startTime >= form.value.endTime) {
      callback(new Error('End time must be after start time'))
    } else {
      callback()
    }
  } else {
    callback()
  }
}

const resolveDateString = (date: Date): string => {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const parseDateString = (value: string): Date | null => {
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) {
    return null
  }
  return new Date(year, month - 1, day)
}

const resolveDateTime = (dateStr: string, timeStr: string): Date | null => {
  const date = parseDateString(dateStr)
  if (!date || !timeStr) {
    return null
  }

  const normalizedTime = /^\d{2}:\d{2}$/.test(timeStr) ? `${timeStr}:00` : timeStr
  const resolved = new Date(`${resolveDateString(date)}T${normalizedTime}`)
  return Number.isNaN(resolved.getTime()) ? null : resolved
}

const resolveFirstOccurrenceDate = (startDate: string, dayOfWeek: number | null): string => {
  const parsedStartDate = parseDateString(startDate)
  if (!parsedStartDate || !dayOfWeek) {
    return startDate
  }

  const targetDay = dayOfWeek % 7
  const currentDay = parsedStartDate.getDay()
  const diff = (targetDay - currentDay + 7) % 7
  const firstOccurrence = new Date(parsedStartDate)
  firstOccurrence.setDate(firstOccurrence.getDate() + diff)
  return resolveDateString(firstOccurrence)
}

const validateFirstOccurrenceStartTime = (_rule: any, _value: any, callback: any) => {
  if (!form.value.effectiveStartDate || !form.value.dayOfWeek || !form.value.startTime) {
    callback()
    return
  }

  const firstOccurrenceDate = resolveFirstOccurrenceDate(
    form.value.effectiveStartDate,
    form.value.dayOfWeek,
  )
  const firstOccurrenceStartAt = resolveDateTime(firstOccurrenceDate, form.value.startTime)
  if (firstOccurrenceStartAt && firstOccurrenceStartAt.getTime() <= Date.now()) {
    callback(new Error('First occurrence must start in the future'))
    return
  }
  callback()
}

const rulesForm: FormRules = {
  effectiveStartDate: [{ required: true, message: 'Please select start date', trigger: 'change' }],
  dayOfWeek: [{ required: true, message: 'Please select a day', trigger: 'change' }],
  startTime: [
    { required: true, message: 'Please select start time', trigger: 'change' },
    { validator: validateFirstOccurrenceStartTime, trigger: 'change' },
  ],
  endTime: [
    { required: true, message: 'Please select end time', trigger: 'change' },
    { validator: validateTimeRange, trigger: 'change' },
  ],
  effectiveEndDate: [
    {
      validator: (_rule, value, callback) => {
        if (form.value.noEndDate || value) {
          if (value && form.value.effectiveStartDate && value < form.value.effectiveStartDate) {
            callback(new Error('End date must be on or after the start date'))
            return
          }
          callback()
          return
        }
        callback(new Error('Please select effective end date or choose no end date'))
      },
      trigger: 'change',
    },
  ],
}

// ============== Methods ==============
function disabledPastDates(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

function disabledRepeatEndDates(date: Date): boolean {
  if (!form.value.effectiveStartDate) {
    return disabledPastDates(date)
  }
  const [year, month, day] = form.value.effectiveStartDate.split('-').map(Number)
  if (!year || !month || !day) {
    return disabledPastDates(date)
  }
  const startDate = new Date(year, month - 1, day)
  startDate.setHours(0, 0, 0, 0)
  return date < startDate
}

function toDateInputValue(date: Date): string {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function handleNoEndDateChange(value: string | number | boolean) {
  if (value) {
    form.value.effectiveEndDate = ''
  }
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-'
  const [year, month, day] = dateStr.split('-').map(Number)
  if (!year || !month || !day) return dateStr
  const date = new Date(year, month - 1, day)
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

function formatDisplayTime(time: string): string {
  if (!time) {
    return ''
  }
  const match = time.match(/^(\d{2}:\d{2})/)
  return match ? match[1] : time
}

async function fetchRules() {
  loading.value = true
  try {
    rules.value = await getAllRecurringRules()
  } catch (error) {
    console.error('Failed to fetch rules:', error)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = {
    effectiveStartDate: toDateInputValue(new Date()),
    dayOfWeek: null,
    startTime: '',
    endTime: '',
    noEndDate: true,
    effectiveEndDate: '',
  }
  formRef.value?.resetFields()
}

async function handleCreate() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const payload: CreateRecurringRuleRequest = {
          effectiveStartDate: form.value.effectiveStartDate,
          dayOfWeek: form.value.dayOfWeek as number,
          startTime: form.value.startTime,
          endTime: form.value.endTime,
          effectiveEndDate: form.value.noEndDate ? null : form.value.effectiveEndDate,
        }
        await createRecurringRule(payload)
        ElMessage.success('Recurring rule created successfully')
        showCreateDialog.value = false
        resetForm()
        await fetchRules()
      } catch (error) {
        console.error('Failed to create rule:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

async function handleDelete(ruleId: number) {
  try {
    await ElMessageBox.confirm(
      'Are you sure you want to delete this recurring rule? This removes the weekly pattern and its generated available time slots. Booked or locked generated slots must be cleared first.',
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' },
    )

    await deleteRecurringRule(ruleId)
    ElMessage.success('Recurring rule deleted successfully')
    await fetchRules()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('Failed to delete rule:', error)
    }
  }
}

// ============== Lifecycle ==============
onMounted(() => {
  fetchRules()
})
</script>

<style scoped lang="scss">
.recurring-rules {
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

  .info-card {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-4);
    margin-bottom: var(--space-6);
    background: var(--color-primary-soft);
    border-radius: var(--radius-lg);
    color: var(--color-text-inverse);
    font-size: 14px;
  }

  .loading-container {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-3);
    padding: var(--space-8);
    color: var(--color-text-secondary);
  }

  .rules-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: var(--space-4);
  }

  .rule-card {
    margin-bottom: var(--space-3);

    .rule-content {
      display: flex;
      flex-direction: column;
      gap: var(--space-3);
    }

    .rule-main {
      display: flex;
      align-items: center;
      gap: var(--space-4);
    }

    .rule-day {
      min-width: 120px;
    }

    .rule-time {
      .time-range {
        font-size: 18px;
        font-weight: 600;
        color: var(--color-text-primary);
      }
    }

    .rule-meta {
      display: flex;
      align-items: center;
      gap: var(--space-4);
      color: var(--color-text-secondary);
      font-size: 13px;
    }

    .meta-item {
      display: flex;
      align-items: center;
      gap: var(--space-1);
    }

    .rule-actions {
      margin-top: var(--space-3);
      padding-top: var(--space-3);
      border-top: 1px solid var(--color-border);
      display: flex;
      justify-content: flex-end;
    }
  }

  .end-date-toggle {
    margin-bottom: var(--space-3);
  }

  .empty-state {
    padding: var(--space-8);
    text-align: center;
  }
}
</style>
