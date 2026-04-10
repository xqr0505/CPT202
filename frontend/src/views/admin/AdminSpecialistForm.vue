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
          <el-input-number
            v-model="form.consultationFee"
            :min="selectedLevelOption?.minFee ?? 0"
            :max="selectedLevelOption?.maxFee"
            :precision="2"
            :step="10"
            style="width: 100%"
          />
          <div v-if="selectedLevelOption" class="field-hint">
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useCategoryStore } from '@/stores/category'
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
const levelOptions = ref<SpecialistLevelOption[]>([])
const suppressFeeAutoSync = ref(false)

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

        if (!selectedLevelOption.value) {
          callback()
          return
        }

        if (value < selectedLevelOption.value.minFee || value > selectedLevelOption.value.maxFee) {
          callback(new Error(
            `Consultation fee for ${selectedLevelOption.value.value} must be between ${formatFee(selectedLevelOption.value.minFee)} and ${formatFee(selectedLevelOption.value.maxFee)}`
          ))
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
    suppressFeeAutoSync.value = true
    form.name = detail.name ?? ''
    form.categoryId = detail.categoryId
    form.level = detail.level ?? ''
    form.consultationFee = detail.consultationFee ?? 0
    form.status = detail.status ?? 'Active'
    form.avatarUrl = detail.avatarUrl ?? ''
    suppressFeeAutoSync.value = false
  } catch (error) {
    console.error('Failed to fetch specialist detail:', error)
    ElMessage.error('Failed to load specialist detail')
    goBack()
  }
}

watch(
  () => form.level,
  level => {
    if (!level || suppressFeeAutoSync.value) {
      return
    }

    const option = levelOptions.value.find(item => item.value === level)
    if (!option) {
      return
    }

    form.consultationFee = option.minFee
    void formRef.value?.validateField('consultationFee')
  }
)

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

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const payload = buildPayload()
      if (isEditMode.value && specialistId.value !== null) {
        await updateSpecialist(specialistId.value, payload)
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

.field-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}
</style>
