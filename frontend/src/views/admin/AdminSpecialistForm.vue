<template>
  <div class="admin-specialist-form-page">
    <div class="page-header">
      <div>
        <p class="eyebrow">{{ isEditMode ? 'Specialist Editing' : 'Specialist Creation' }}</p>
        <h2>{{ isEditMode ? 'Edit Specialist' : 'Add Specialist' }}</h2>
      </div>
      <p class="page-note">
        {{ isEditMode ? 'Update a specialist profile with category, level, and pricing details.' : 'Create a specialist profile with category, level, and pricing details.' }}
      </p>
    </div>

    <div class="page-grid" v-loading="pageLoading">
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h3>Profile Details</h3>
              <p>Keep the core specialist information accurate and ready for booking management.</p>
            </div>
          </div>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
          <el-form-item label="Name" prop="name">
            <el-input v-model="form.name" placeholder="Please enter specialist name" />
          </el-form-item>

          <el-form-item label="Email" prop="email">
            <el-input
              v-model="form.email"
              placeholder="Please enter specialist email"
              type="email"
            />
          </el-form-item>

          <el-form-item v-if="isEditMode" label="New Password" prop="password">
            <el-input
              v-model="form.password"
              placeholder="Leave empty to keep current password"
              type="password"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item v-else label="Initial Password">
            <div class="default-password-tip">
              New specialist accounts are created with default password: <strong>12345Expertlink</strong>
            </div>
          </el-form-item>

          <el-form-item label="Category" prop="categoryId">
            <el-select
              v-model="form.categoryId"
              placeholder="Please select category"
              clearable
              :loading="categoryStore.loading"
              style="width: 100%"
            >
              <el-option
                v-for="item in categoryStore.categories"
                :key="item.id"
                :label="item.categoryName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Level" prop="level">
            <el-select
              v-model="form.level"
              placeholder="Please select level"
              clearable
              :loading="levelLoading"
              @change="handleLevelChange"
              style="width: 100%"
            >
              <el-option
                v-for="item in levelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Consultation Fee" prop="consultationFee">
            <el-input
              :model-value="consultationFeeInput"
              inputmode="decimal"
              @update:model-value="handleConsultationFeeInput"
              @blur="handleConsultationFeeBlur"
              style="width: 100%"
            />
            <div v-if="selectedLevelOption && !isEditMode" class="field-hint">
              Allowed range: ${{ formatFee(selectedLevelOption.minFee) }} - ${{ formatFee(selectedLevelOption.maxFee) }}
            </div>
          </el-form-item>

          <el-form-item v-if="!isEditMode" label="Status" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio value="Active">Active</el-radio>
              <el-radio value="Inactive">Inactive</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="Avatar">
            <ImageUploader v-model="form.avatarUrl" />
          </el-form-item>

          <el-form-item class="action-row">
            <el-button @click="goBack">Cancel</el-button>
            <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
              Save
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <div class="side-column">
        <el-card class="summary-card" shadow="never">
          <template #header>
            <div class="card-header compact">
              <div>
                <h3>Current Summary</h3>
                <p>Preview the specialist information that will be saved.</p>
              </div>
            </div>
          </template>

          <div class="summary-block">
            <div class="summary-avatar">
              <el-avatar :size="64" :src="form.avatarUrl || undefined">
                {{ avatarFallback }}
              </el-avatar>
            </div>
            <div class="summary-name">{{ form.name.trim() || 'New Specialist' }}</div>
            <div class="summary-meta">{{ selectedCategoryName }}</div>
          </div>

          <div class="summary-grid">
            <div class="summary-item">
              <span class="summary-label">Level</span>
              <strong>{{ form.level || 'Not selected' }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-label">Status</span>
              <el-tag :type="form.status === 'Active' ? 'success' : 'info'">{{ form.status }}</el-tag>
            </div>
            <div class="summary-item wide">
              <span class="summary-label">Consultation Fee</span>
              <strong>${{ formatFee(Number(form.consultationFee || 0)) }}</strong>
            </div>
          </div>
        </el-card>
        <el-card class="summary-card accent-card" shadow="never">
          <template #header>
            <div class="card-header compact">
              <div>
                <h3>Pricing Guidance</h3>
                <p>Use the level range as the benchmark for new specialist pricing.</p>
              </div>
            </div>
          </template>

          <div v-if="selectedLevelOption" class="range-panel">
            <div class="range-value">
              ${{ formatFee(selectedLevelOption.minFee) }} - ${{ formatFee(selectedLevelOption.maxFee) }}
            </div>
            <div class="range-caption">Recommended range for {{ selectedLevelOption.label }}</div>
          </div>
          <div v-else class="empty-tip">
            Select a level to see the recommended consultation fee range.
          </div>

          <ul class="tips-list">
            <li>New specialists are clamped to the selected level range when pricing is entered manually.</li>
            <li>Inactive specialists are hidden from booking discovery on the frontend.</li>
            <li>Edit mode allows out-of-range fees, but records the change in history.</li>
          </ul>
        </el-card>
      </div>
    </div>

    <div v-if="isEditMode" class="edit-bottom-grid">
      <el-card class="history-card" shadow="never">
        <template #header>
          <div class="history-header">Fee Change Record</div>
        </template>

        <el-table v-loading="historyLoading" :data="feeChangeRecords" empty-text="No fee change records yet.">
          <el-table-column prop="createdAt" label="Changed At" min-width="180" />
          <el-table-column prop="changedByName" label="Changed By" min-width="140">
            <template #default="{ row }">
              {{ row.changedByName || 'System' }}
            </template>
          </el-table-column>
          <el-table-column prop="oldFee" label="Old Fee" min-width="110">
            <template #default="{ row }">
              ${{ formatFee(Number(row.oldFee || 0)) }}
            </template>
          </el-table-column>
          <el-table-column prop="newFee" label="New Fee" min-width="110">
            <template #default="{ row }">
              ${{ formatFee(Number(row.newFee || 0)) }}
            </template>
          </el-table-column>
          <el-table-column prop="level" label="Level" min-width="110" />
          <el-table-column label="Range Snapshot" min-width="180">
            <template #default="{ row }">
              ${{ formatFee(Number(row.rangeMin || 0)) }} - ${{ formatFee(Number(row.rangeMax || 0)) }}
            </template>
          </el-table-column>
          <el-table-column prop="outOfRange" label="Abnormal" min-width="100">
            <template #default="{ row }">
              <el-tag :type="row.outOfRange ? 'danger' : 'success'">
                {{ row.outOfRange ? 'Yes' : 'No' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import ImageUploader from '@/components/common/ImageUploader.vue'
import { useCategoryStore } from '@/stores/category'
import {
  getSpecialistFeeChangeRecords,
  type SpecialistFeeChangeRecord
} from '@/api/adminSpecialistFeeRecord'
import {
  createSpecialist,
  getSpecialistDetail,
  getSpecialistLevels,
  updateSpecialist,
  type SpecialistLevelOption,
  type CreateSpecialistPayload,
  type UpdateSpecialistPayload,
  type SpecialistStatus
} from '@/api/adminSpecialist'

interface SpecialistFormModel {
  name: string
  email: string
  password: string
  categoryId?: number
  level: string
  consultationFee: number
  status: SpecialistStatus
  avatarUrl: string
}

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()
const formRef = ref<FormInstance>()

const pageLoading = ref(false)
const levelLoading = ref(false)
const submitLoading = ref(false)
const historyLoading = ref(false)
const levelOptions = ref<SpecialistLevelOption[]>([])
const consultationFeeInput = ref('0.00')
const feeChangeRecords = ref<SpecialistFeeChangeRecord[]>([])

const specialistId = computed<number | null>(() => {
  const raw = route.params.id
  if (!raw) return null
  const id = Number(raw)
  return Number.isNaN(id) ? null : id
})

const isEditMode = computed(() => specialistId.value !== null)
const selectedLevelOption = computed(() =>
  levelOptions.value.find(item => item.value === form.level)
)
const selectedCategoryName = computed(() => {
  const category = categoryStore.categories.find(item => item.id === form.categoryId)
  return category?.categoryName || 'Category not selected'
})
const avatarFallback = computed(() => (form.name.trim().slice(0, 1).toUpperCase() || 'S'))

const form = reactive<SpecialistFormModel>({
  name: '',
  email: '',
  password: '',
  categoryId: undefined,
  level: '',
  consultationFee: 0,
  status: 'Active',
  avatarUrl: ''
})

function formatFee(fee: number) {
  return fee.toFixed(2)
}

function normalizeConsultationFee(value: string) {
  const sanitized = value.replace(/[^\d.]/g, '')
  const segments = sanitized.split('.')
  if (segments.length <= 1) {
    return sanitized
  }

  return `${segments[0]}.${segments.slice(1).join('')}`
}

function clampConsultationFeeToLevelRange(value: number) {
  if (isEditMode.value || !selectedLevelOption.value) {
    return value
  }

  if (value < selectedLevelOption.value.minFee) {
    return selectedLevelOption.value.minFee
  }

  if (value > selectedLevelOption.value.maxFee) {
    return selectedLevelOption.value.maxFee
  }

  return value
}

function showFeeAdjustedMessage(originalValue: number, adjustedValue: number) {
  if (isEditMode.value || originalValue === adjustedValue || !selectedLevelOption.value) {
    return
  }

  ElMessage.warning(
    `Fee has been adjusted to ${formatFee(adjustedValue)} to match the allowed ${form.level} range (${formatFee(selectedLevelOption.value.minFee)} - ${formatFee(selectedLevelOption.value.maxFee)}).`
  )
}

const rules: FormRules<SpecialistFormModel> = {
  name: [{ required: true, message: 'Please enter name', trigger: 'blur' }],
  email: [
    { required: true, message: 'Please enter email', trigger: 'blur' },
    { type: 'email', message: 'Please enter a valid email', trigger: ['blur', 'change'] }
  ],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (!isEditMode.value) {
          callback()
          return
        }
        if (value && value.length < 8) {
          callback(new Error('Password must be at least 8 characters'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  categoryId: [{ required: true, message: 'Please select category', trigger: 'change' }],
  level: [{ required: true, message: 'Please select level', trigger: 'change' }],
  consultationFee: [
    { required: true, message: 'Please enter consultation fee', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (value === undefined || value === null) {
          callback(new Error('Please enter consultation fee'))
          return
        }

        callback()
      },
      trigger: 'change'
    }
  ],
  status: [{ required: true, message: 'Please select status', trigger: 'change' }]
}

async function fetchCategories() {
  try {
    await categoryStore.fetchCategories()
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  }
}

async function fetchLevels() {
  levelLoading.value = true
  try {
    levelOptions.value = await getSpecialistLevels()
  } catch (error) {
    console.error('Failed to fetch specialist levels:', error)
    levelOptions.value = []
  } finally {
    levelLoading.value = false
  }
}

async function fetchSpecialistDetail() {
  if (!isEditMode.value || specialistId.value === null) return

  try {
    const detail = await getSpecialistDetail(specialistId.value)
    form.name = detail.name ?? ''
    form.email = detail.email ?? ''
    form.password = ''
    form.categoryId = detail.categoryId
    form.level = detail.level ?? ''
    form.consultationFee = detail.consultationFee ?? 0
    consultationFeeInput.value = Number(form.consultationFee).toFixed(2)
    form.status = detail.status ?? 'Active'
    form.avatarUrl = detail.avatarUrl ?? ''
  } catch (error) {
    console.error('Failed to fetch specialist detail:', error)
    ElMessage.error('Failed to load specialist detail')
    goBack()
  }
}

async function fetchFeeChangeRecords() {
  if (!isEditMode.value || specialistId.value === null) return

  historyLoading.value = true
  try {
    feeChangeRecords.value = await getSpecialistFeeChangeRecords(specialistId.value)
  } catch (error) {
    console.error('Failed to fetch fee change records:', error)
    feeChangeRecords.value = []
  } finally {
    historyLoading.value = false
  }
}

function handleLevelChange(level: string) {
  if (!level || isEditMode.value) {
    return
  }

  const option = levelOptions.value.find(item => item.value === level)
  if (!option) {
    return
  }

  form.consultationFee = option.minFee
  consultationFeeInput.value = option.minFee.toFixed(2)
  void formRef.value?.validateField('consultationFee')
}

function handleConsultationFeeInput(value: string) {
  consultationFeeInput.value = value
  const normalized = normalizeConsultationFee(value)
  if (!normalized) {
    form.consultationFee = 0
    return
  }

  const parsed = Number(normalized)
  if (!Number.isNaN(parsed)) {
    form.consultationFee = parsed
  }
}

function handleConsultationFeeBlur(event: FocusEvent) {
  const target = event.target as HTMLInputElement | null
  if (!target) {
    return
  }

  const normalized = normalizeConsultationFee(target.value)
  if (!normalized) {
    form.consultationFee = 0
    target.value = '0.00'
    void formRef.value?.validateField('consultationFee')
    return
  }

  const parsed = Number(normalized)
  if (Number.isNaN(parsed)) {
    return
  }

  const finalValue = clampConsultationFeeToLevelRange(parsed)
  form.consultationFee = finalValue
  consultationFeeInput.value = finalValue.toFixed(2)
  target.value = consultationFeeInput.value
  showFeeAdjustedMessage(parsed, finalValue)
  void formRef.value?.validateField('consultationFee')
}

function isConsultationFeeOutOfRange() {
  if (!selectedLevelOption.value) {
    return false
  }

  return form.consultationFee < selectedLevelOption.value.minFee
    || form.consultationFee > selectedLevelOption.value.maxFee
}

async function confirmOutOfRangeFeeIfNeeded() {
  if (!selectedLevelOption.value || !isConsultationFeeOutOfRange()) {
    return true
  }

  try {
    await ElMessageBox.confirm(
      `The current consultation fee is ${formatFee(form.consultationFee)}, which is outside the ${form.level} range (${formatFee(selectedLevelOption.value.minFee)} - ${formatFee(selectedLevelOption.value.maxFee)}). Do you want to keep this price and continue saving?`,
      'Confirm Consultation Fee',
      {
        confirmButtonText: 'Confirm and Save',
        cancelButtonText: 'Back to Edit',
        type: 'warning'
      }
    )
    return true
  } catch {
    return false
  }
}

function buildCreatePayload(): CreateSpecialistPayload {
  return {
    name: form.name.trim(),
    email: form.email.trim().toLowerCase(),
    categoryId: Number(form.categoryId),
    level: form.level.trim(),
    consultationFee: Number(form.consultationFee),
    status: form.status,
    avatarUrl: form.avatarUrl.trim() || undefined
  }
}

function buildUpdatePayload(): UpdateSpecialistPayload {
  const payload: UpdateSpecialistPayload = {
    name: form.name.trim(),
    email: form.email.trim().toLowerCase(),
    categoryId: Number(form.categoryId),
    level: form.level.trim(),
    consultationFee: Number(form.consultationFee),
    status: form.status,
    avatarUrl: form.avatarUrl.trim() || undefined
  }
  if (form.password.trim()) {
    payload.password = form.password.trim()
  }
  return payload
}

async function handleSubmit() {
  if (!formRef.value) return

  const originalFee = form.consultationFee
  form.consultationFee = clampConsultationFeeToLevelRange(form.consultationFee)
  consultationFeeInput.value = form.consultationFee.toFixed(2)
  showFeeAdjustedMessage(originalFee, form.consultationFee)

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    const confirmed = await confirmOutOfRangeFeeIfNeeded()
    if (!confirmed) return

    submitLoading.value = true
    try {
      if (isEditMode.value && specialistId.value !== null) {
        await updateSpecialist(specialistId.value, buildUpdatePayload())
        await fetchFeeChangeRecords()
        ElMessage.success('Specialist updated successfully')
      } else {
        await createSpecialist(buildCreatePayload())
        ElMessage.success('Specialist created successfully (default password: 12345Expertlink)')
      }
      goBack()
    } catch (error) {
      console.error('Failed to save specialist:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

function goBack() {
  router.push('/admin/specialists')
}

onMounted(async () => {
  pageLoading.value = true
  try {
    await Promise.all([
      fetchCategories(),
      fetchLevels()
    ])
    await fetchSpecialistDetail()
    await fetchFeeChangeRecords()
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
.admin-specialist-form-page {
  padding: 28px 24px 40px;
  max-width: 1240px;
  margin: 0 auto;
  --el-color-primary: var(--color-primary);
  --el-color-primary-light-3: color-mix(in srgb, var(--color-primary) 70%, white);
  --el-color-primary-light-5: color-mix(in srgb, var(--color-primary) 50%, white);
  --el-color-primary-light-7: color-mix(in srgb, var(--color-primary) 30%, white);
  --el-color-primary-dark-2: color-mix(in srgb, var(--color-primary) 80%, black);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-primary);
}

.page-header h2 {
  margin: 0;
  font-size: 42px;
  line-height: 1.05;
  color: var(--color-text-primary);
}

.page-note {
  max-width: 380px;
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.page-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.9fr);
  gap: 24px;
  align-items: start;
}

.form-card,
.summary-card,
.history-card {
  border-radius: 20px;
  border: 1px solid var(--color-border);
  box-shadow: 0 18px 40px rgba(var(--color-shadow-rgb), 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--color-text-primary);
}

.card-header p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.card-header.compact h3 {
  font-size: 18px;
}

.side-column {
  display: grid;
  gap: 20px;
}

.summary-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 6px 0 20px;
  border-bottom: 1px solid var(--color-border);
}

.summary-avatar {
  margin-bottom: 14px;
}

.summary-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.summary-meta {
  margin-top: 6px;
  color: var(--color-text-secondary);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  background: linear-gradient(180deg, var(--color-bg-surface) 0%, var(--color-bg-muted) 100%);
  border: 1px solid var(--color-border);
  border-radius: 14px;
}

.summary-item.wide {
  grid-column: 1 / -1;
}

.summary-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.accent-card {
  background: linear-gradient(180deg, var(--color-bg-surface) 0%, var(--color-bg-muted) 100%);
}

.range-panel {
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(
    135deg,
    rgba(var(--color-primary-rgb), 0.9) 0%,
    rgba(var(--color-primary-rgb), 0.65) 100%
  );
  color: #fff;
}

.range-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
}

.range-caption {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.86);
}

.empty-tip {
  padding: 16px;
  border-radius: 14px;
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
}

.tips-list {
  margin: 18px 0 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.tips-list li + li {
  margin-top: 8px;
}

.action-row :deep(.el-form-item__content) {
  justify-content: flex-end;
  gap: 10px;
}

.history-card {
  margin-top: 24px;
}

.history-header {
  font-weight: 600;
}

.edit-bottom-grid {
  margin-top: 24px;
}

.field-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.default-password-tip {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(var(--color-primary-rgb), 0.12);
  color: var(--color-text-primary);
  line-height: 1.6;
}

@media (max-width: 980px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .page-header h2 {
    font-size: 34px;
  }

  .page-grid {
    grid-template-columns: 1fr;
  }

  .edit-bottom-grid {
    margin-top: 24px;
  }
}

@media (max-width: 640px) {
  .admin-specialist-form-page {
    padding: 20px 16px 32px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .summary-item.wide {
    grid-column: auto;
  }

  .page-header h2 {
    font-size: 30px;
  }
}
</style>
