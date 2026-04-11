<template>
  <div class="admin-specialist-form-page">
    <h2>{{ isEditMode ? 'Edit Specialist' : 'Add Specialist' }}</h2>

    <el-card class="form-card" shadow="never" v-loading="pageLoading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="Name" prop="name">
          <el-input v-model="form.name" placeholder="Please enter specialist name" />
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

        <el-form-item label="Status" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="Active">Active</el-radio>
            <el-radio value="Inactive">Inactive</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Avatar URL">
          <el-input v-model="form.avatarUrl" placeholder="https://example.com/avatar.png" />
        </el-form-item>

        <el-form-item>
          <el-button @click="goBack">Cancel</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            Save
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="isEditMode" class="history-card" shadow="never">
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
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
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
  type SpecialistPayload,
  type SpecialistStatus
} from '@/api/adminSpecialist'

interface SpecialistFormModel {
  name: string
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

const form = reactive<SpecialistFormModel>({
  name: '',
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

function buildPayload(): SpecialistPayload {
  return {
    name: form.name.trim(),
    categoryId: Number(form.categoryId),
    level: form.level.trim(),
    consultationFee: Number(form.consultationFee),
    status: form.status,
    avatarUrl: form.avatarUrl.trim() || undefined
  }
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
      const payload = buildPayload()
      if (isEditMode.value && specialistId.value !== null) {
        await updateSpecialist(specialistId.value, payload)
        await fetchFeeChangeRecords()
        ElMessage.success('Specialist updated successfully')
      } else {
        await createSpecialist(payload)
        ElMessage.success('Specialist created successfully')
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
  padding: 20px;
}

.form-card {
  margin-top: 16px;
  max-width: 760px;
}

.history-card {
  margin-top: 16px;
}

.history-header {
  font-weight: 600;
}

.field-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}
</style>
