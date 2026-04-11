<template>
  <div class="admin-specialist-page">
    <h2>Specialist Management</h2>

    <div class="toolbar">
      <el-input
        v-model="filters.keyword"
        placeholder="Search specialist name"
        clearable
        class="toolbar-item keyword-input"
      />

      <el-select
        v-model="filters.categoryId"
        placeholder="Category"
        clearable
        class="toolbar-item"
      >
        <el-option
          v-for="item in categoryStore.categoryOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>

      <el-select
        v-model="filters.status"
        placeholder="Status"
        clearable
        class="toolbar-item"
      >
        <el-option label="Active" value="Active" />
        <el-option label="Inactive" value="Inactive" />
      </el-select>

      <el-button type="primary" @click="goCreatePage">Add Specialist</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" style="width: 100%">
      <el-table-column label="Avatar" width="100">
        <template #default="{ row }">
          <el-avatar :size="36" :src="row.avatarUrl">
            {{ getAvatarFallback(row.name) }}
          </el-avatar>
        </template>
      </el-table-column>

      <el-table-column prop="name" label="Name" min-width="140" />
      <el-table-column prop="categoryName" label="Category" min-width="120" />
      <el-table-column prop="level" label="Level" min-width="100" />
      <el-table-column prop="consultationFee" label="Fee" min-width="100">
        <template #default="{ row }">
          {{ formatFee(row.consultationFee) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="Status" min-width="100" />

      <el-table-column label="Actions" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="goEditPage(row.id)">Edit</el-button>
          <el-button
            size="small"
            :type="row.status === 'Active' ? 'warning' : 'success'"
            plain
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 'Active' ? 'Deactivate' : 'Activate' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCategoryStore } from '@/stores/category'
import {
  getSpecialistList,
  type SpecialistListParams,
  updateSpecialistStatus,
  type SpecialistStatus,
  type SpecialistItem
} from '@/api/adminSpecialist'

const router = useRouter()
const categoryStore = useCategoryStore()
const loading = ref(false)
const tableData = ref<SpecialistItem[]>([])

const filters = reactive({
  keyword: '',
  categoryId: undefined as number | string | undefined,
  status: undefined as 'Active' | 'Inactive' | undefined
})

function getAvatarFallback(name: string) {
  return (name || '').slice(0, 1).toUpperCase() || 'S'
}

function formatFee(fee: number) {
  return `$${fee.toFixed(2)}`
}

function goCreatePage() {
  router.push('/admin/specialists/create')
}

function goEditPage(id: number) {
  router.push(`/admin/specialists/${id}/edit`)
}

async function handleToggleStatus(row: SpecialistItem) {
  const nextStatus: SpecialistStatus = row.status === 'Active' ? 'Inactive' : 'Active'
  const actionText = nextStatus === 'Active' ? 'activate' : 'deactivate'

  try {
    await ElMessageBox.confirm(
      `Are you sure you want to ${actionText} specialist "${row.name}"?`,
      'Confirm Status Change',
      {
        type: 'warning',
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel'
      }
    )
  } catch {
    return
  }

  try {
    await updateSpecialistStatus(row.id, nextStatus)
    ElMessage.success('Status updated successfully')
    await fetchSpecialistList()
  } catch (error) {
    console.error('Failed to update specialist status:', error)
  }
}

async function fetchSpecialistList() {
  loading.value = true
  try {
    const params: SpecialistListParams = {}
    const keyword = filters.keyword.trim()
    const categoryId =
      filters.categoryId === undefined || filters.categoryId === null || filters.categoryId === ''
        ? undefined
        : Number(filters.categoryId)

    if (keyword) {
      params.keyword = keyword
    }
    if (categoryId !== undefined && !Number.isNaN(categoryId)) {
      params.categoryId = categoryId
    }
    if (filters.status) {
      params.status = filters.status
    }

    const data = await getSpecialistList(params)
    const rows = Array.isArray(data) ? data : data.list
    tableData.value = rows
  } catch (error) {
    console.error('Failed to fetch specialist list:', error)
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function initializePage() {
  await Promise.all([
    categoryStore.fetchCategories(),
    fetchSpecialistList()
  ])
}

onMounted(() => {
  void initializePage()
})

watch(
  () => [filters.keyword, filters.categoryId, filters.status],
  () => {
    void fetchSpecialistList()
  }
)
</script>

<style scoped>
.admin-specialist-page {
  padding: 20px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin: 16px 0 20px;
  flex-wrap: wrap;
}

.toolbar-item {
  width: 180px;
}

.keyword-input {
  width: 260px;
}
</style>
