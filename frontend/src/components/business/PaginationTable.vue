<template>
  <div class="pagination-table">
    <div class="table-container">
      <template v-if="!isMobile">
        <el-table
          v-loading="loading"
          :data="tableData"
          class="table-content"
          border
          stripe
          :row-class-name="props.rowClassName"
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
      </template>

      <template v-else>
        <div class="mobile-list-content" v-loading="loading">
          <template v-if="$slots.mobile">
            <slot name="mobile" :data="tableData"></slot>
          </template>
          <template v-else>
            <div v-for="(row, index) in tableData" :key="index" class="mobile-fallback-card">
              <slot name="mobile-item" :row="row" :index="index">
                <pre>{{ row }}</pre>
              </slot>
            </div>
          </template>
          <el-empty v-if="tableData.length === 0 && !loading" description="No Data" />
        </div>
      </template>
    </div>

    <div class="pagination-container" :class="{ 'is-mobile': isMobile }">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
        :total="total"
        :small="isMobile"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useWindowSize } from '@vueuse/core'

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

export interface FetchDataResult<T = unknown> {
  list: T[]
  total: number
}

interface Props {
  columns: TableColumn[]
  fetchData: (params: FetchDataParams) => Promise<FetchDataResult>
  rowClassName?: string | ((data: { row: unknown; rowIndex: number }) => string)
}

const props = defineProps<Props>()

const { width } = useWindowSize()
const isMobile = computed(() => width.value < 768)

const loading = ref(false)
const tableData = ref<unknown[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const loadData = async () => {
  loading.value = true
  try {
    const res = await props.fetchData({ page: currentPage.value, limit: pageSize.value })
    tableData.value = res.list
    const parsedTotal = Number((res as unknown as { total?: unknown })?.total)
    total.value = Number.isFinite(parsedTotal) ? parsedTotal : 0
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
  background-color: var(---color-bg-page);
  border-radius: 0;
  box-shadow: none;
  padding: 0;

  .table-container {
    border: 0px solid var(--color-border);
    border-radius: 8px;
    overflow: hidden;

    .table-content {
      width: 100%;
      margin-bottom: var(--space-4);
    }

    .mobile-list-content {
      margin-bottom: var(--space-4);
    }
  }

  .pagination-container {
    margin-top: 16px;
    display: flex;
    justify-content: center;
  }
}
</style>
