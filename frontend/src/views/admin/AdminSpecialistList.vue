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
          v-for="item in categoryOptions"
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
          >
            {{ row.status === 'Active' ? 'Deactivate' : 'Activate' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getSpecialistList,
  type SpecialistItem,
  type SpecialistListResponse
} from '@/api/adminSpecialist'

interface CategoryOption {
  label: string
  value: number
}

const router = useRouter()
const loading = ref(false)
const tableData = ref<SpecialistItem[]>([])
const categoryOptions = ref<CategoryOption[]>([])

const filters = reactive({
  keyword: '',
  categoryId: undefined as number | undefined,
  status: undefined as 'Active' | 'Inactive' | undefined
})

function getAvatarFallback(name: string) {
  return (name || '').slice(0, 1).toUpperCase() || 'S'
}

function formatFee(fee: number) {
  if (typeof fee !== 'number') return '-'
  return `$${fee.toFixed(2)}`
}

function goCreatePage() {
  router.push('/admin/specialists/create')
}

function goEditPage(id: number) {
  router.push(`/admin/specialists/${id}/edit`)
}

function buildCategoryOptions(list: SpecialistItem[]) {
  const map = new Map<number, string>()
  list.forEach(item => {
    if (item.categoryId && item.categoryName) {
      map.set(item.categoryId, item.categoryName)
    }
  })

  categoryOptions.value = Array.from(map.entries()).map(([value, label]) => ({
    value,
    label
  }))
}

async function fetchSpecialistList() {
  loading.value = true
  try {
    const data = await getSpecialistList()
    const list = (data as SpecialistListResponse)?.list
    tableData.value = Array.isArray(list) ? list : []
    buildCategoryOptions(tableData.value)
  } catch (error) {
    console.error('Failed to fetch specialist list:', error)
    tableData.value = []
    categoryOptions.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchSpecialistList()
})
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
