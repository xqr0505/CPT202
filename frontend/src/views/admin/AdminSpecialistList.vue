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

    <div class="pagination-wrapper">
      <div class="pagination-summary">
        Total {{ pagination.total }} specialists, Page {{ totalPages === 0 ? 0 : pagination.pageNo }} / {{ totalPages }}
      </div>
      <div class="pagination-actions">
        <el-select v-model="pagination.pageSize" class="page-size-select">
          <el-option :value="10" label="10 / page" />
          <el-option :value="20" label="20 / page" />
          <el-option :value="50" label="50 / page" />
          <el-option :value="100" label="100 / page" />
        </el-select>
        <el-button :disabled="pagination.pageNo <= 1" @click="pagination.pageNo -= 1">
          Previous
        </el-button>
        <el-button :disabled="pagination.pageNo >= totalPages" type="primary" @click="pagination.pageNo += 1">
          Next
        </el-button>
      </div>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
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
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})
const totalPages = computed(() => Math.ceil(pagination.total / pagination.pageSize))

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
  const hasActiveBookings = nextStatus === 'Inactive' && row.hasActiveBookings
  const activeBookingCount = row.activeBookingCount ?? 0
  const confirmMessage = hasActiveBookings
    ? `Specialist "${row.name}" still has ${activeBookingCount} pending or confirmed booking${activeBookingCount === 1 ? '' : 's'}. Are you sure you want to deactivate this specialist?`
    : `Are you sure you want to ${actionText} specialist "${row.name}"?`
  const confirmTitle = hasActiveBookings ? 'Existing Bookings Found' : 'Confirm Status Change'

  try {
    await ElMessageBox.confirm(
      confirmMessage,
      confirmTitle,
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
    const cancelledBookingCount = await updateSpecialistStatus(row.id, nextStatus)
    row.status = nextStatus
    if (nextStatus === 'Inactive') {
      row.hasActiveBookings = false
      row.activeBookingCount = 0
    }
    if (nextStatus === 'Inactive' && cancelledBookingCount > 0) {
      ElMessage.success(`${cancelledBookingCount} bookings were cancelled and affected customers have been notified.`)
    } else {
      ElMessage.success('Status updated successfully')
    }
    void fetchSpecialistList()
  } catch (error) {
    console.error('Failed to update specialist status:', error)
  }
}

async function fetchSpecialistList() {
  loading.value = true
  try {
    const params: SpecialistListParams = {
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    }
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
    tableData.value = Array.isArray(data) ? data : (data.list ?? [])
    pagination.total = Array.isArray(data) ? tableData.value.length : (data.total ?? 0)
  } catch (error) {
    console.error('Failed to fetch specialist list:', error)
    tableData.value = []
    pagination.total = 0
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
    pagination.pageNo = 1
    void fetchSpecialistList()
  }
)

watch(
  () => [pagination.pageNo, pagination.pageSize],
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

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin: 0 0 16px;
  padding: 12px 16px;
  background: #f8fbff;
  border: 1px solid #dbe7f3;
  border-radius: 12px;
}

.pagination-summary {
  color: #475467;
  font-size: 14px;
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-size-select {
  width: 120px;
}
</style>
