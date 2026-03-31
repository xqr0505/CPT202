<template>
  <div class="pagination-table">
    <el-table
      v-loading="loading"
      :data="tableData"
      class="table-content"
      border
      stripe
    >
      <el-table-column
        v-for="col in columns"
        :key="col.prop || col.label"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
      >
        <template #default="scope" v-if="col.slotName || $slots[col.prop || '']">
          <slot :name="col.slotName || col.prop" :row="scope.row" :index="scope.$index">
            {{ scope.row[col.prop!] }}
          </slot>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

export interface TableColumn {
  prop?: string
  label: string
  width?: number | string
  minWidth?: number | string
  slotName?: string
}

export interface FetchDataParams {
  page: number
  limit: number
}

export interface FetchDataResult<T = any> {
  list: T[]
  total: number
}

interface Props {
  columns: TableColumn[]
  fetchData: (params: FetchDataParams) => Promise<FetchDataResult>
}

const props = defineProps<Props>()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const loadData = async () => {
  loading.value = true
  try {
    const res = await props.fetchData({ page: currentPage.value, limit: pageSize.value })
    tableData.value = res.list
    total.value = res.total
  } catch (error) {
    console.error('Failed to load table data:', error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

// Expose refresh method to parent
defineExpose({
  refresh: () => {
    currentPage.value = 1
    loadData()
  }
})

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables';

.pagination-table {
  background-color: var(--color-bg-surface);
  border-radius: var(--radius-md);
  box-shadow: 0 2px 12px var(--color-shadow);
  padding: var(--space-4);

  .table-content {
    width: 100%;
    margin-bottom: var(--space-4);
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: var(--space-4);
  }
}
</style>

