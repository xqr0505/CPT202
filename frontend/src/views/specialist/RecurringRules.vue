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
        Recurring rules automatically generate time slots on a weekly basis.
        For example, set your availability for every Monday and Wednesday.
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
              <span class="time-range">{{ rule.startTime }} - {{ rule.endTime }}</span>
            </div>
          </div>
          <div class="rule-meta">
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>Until {{ formatDate(rule.effectiveEndDate) }}</span>
            </div>
            <div class="meta-item">
              <el-tag :type="rule.isActive === 1 ? 'success' : 'info'" size="small">
                {{ rule.statusDesc }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="rule-actions">
          <el-button type="danger" size="small" @click="handleDelete(rule.id)">
            Delete
          </el-button>
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
          <el-time-select
            v-model="form.startTime"
            start="08:00"
            step="00:30"
            end="18:00"
            placeholder="Select start time"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="End Time" prop="endTime">
          <el-time-select
            v-model="form.endTime"
            start="08:00"
            step="00:30"
            end="18:00"
            placeholder="Select end time"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="Effective Until" prop="effectiveEndDate">
          <el-date-picker
            v-model="form.effectiveEndDate"
            type="date"
            placeholder="Select end date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledPastDates"
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
  type RecurringRuleVO
} from '@/api/schedule'

// ============== State ==============
const loading = ref(false)
const submitting = ref(false)
const rules = ref<RecurringRuleVO[]>([])
const showCreateDialog = ref(false)
const formRef = ref<FormInstance>()

interface RecurringRuleForm {
  dayOfWeek: number | null
  startTime: string
  endTime: string
  effectiveEndDate: string
}

const form = ref<RecurringRuleForm>({
  dayOfWeek: 1,
  startTime: '',
  endTime: '',
  effectiveEndDate: ''
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

const rulesForm: FormRules = {
  dayOfWeek: [
    { required: true, message: 'Please select a day', trigger: 'change' }
  ],
  startTime: [
    { required: true, message: 'Please select start time', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: 'Please select end time', trigger: 'change' },
    { validator: validateTimeRange, trigger: 'change' }
  ],
  effectiveEndDate: [
    { required: true, message: 'Please select effective end date', trigger: 'change' }
  ]
}

// ============== Methods ==============
function disabledPastDates(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  const [year, month, day] = dateStr.split('-').map(Number)
  if (!year || !month || !day) return dateStr
  const date = new Date(year, month - 1, day)
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
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
    dayOfWeek: null,
    startTime: '',
    endTime: '',
    effectiveEndDate: ''
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
          dayOfWeek: form.value.dayOfWeek as number,
          startTime: form.value.startTime,
          endTime: form.value.endTime,
          effectiveEndDate: form.value.effectiveEndDate
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
      'Are you sure you want to delete this recurring rule? This will also delete all generated time slots.',
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    
    await deleteRecurringRule(ruleId)
    ElMessage.success('Recurring rule deleted successfully')
    await fetchRules()
  } catch (error: any) {
    if (error !== 'cancel') {
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

  .empty-state {
    padding: var(--space-8);
    text-align: center;
  }
}
</style>
