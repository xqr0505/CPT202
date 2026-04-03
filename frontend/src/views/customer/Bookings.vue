<template>
  <section class="bookings-page">
    <section class="page-card">
      <p class="page-tag">Customer</p>
      <h1>My Bookings</h1>
      <p class="page-desc">Your booking history and upcoming appointments.</p>
    </section>

    <section class="page-card filters-card">
      <el-radio-group v-model="timeScope" @change="refreshTable">
        <el-radio-button label="UPCOMING">Upcoming</el-radio-button>
        <el-radio-button label="HISTORY">History</el-radio-button>
      </el-radio-group>
    </section>

    <PaginationTable
      ref="tableRef"
      :columns="columns"
      :fetch-data="fetchData"
    >
      <template #specialistName="{ row }">
        <div class="specialist-cell">
          <el-avatar :src="row.specialistAvatar" :size="36">
            {{ row.specialistName?.charAt?.(0) || 'S' }}
          </el-avatar>
          <div>
            <div class="specialist-name">{{ row.specialistName }}</div>
            <div class="specialist-title">{{ row.specialistTitle }}</div>
          </div>
        </div>
      </template>

      <template #startTime="{ row }">
        <div>{{ formatDateTime(row.startTime) }}</div>
      </template>

      <template #status="{ row }">
        <BookingStatusTag :status="row.status" />
      </template>

      <template #amount="{ row }">
        <span>{{ formatAmount(row.amount) }}</span>
      </template>
    </PaginationTable>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FetchDataParams, FetchDataResult, TableColumn } from '@/components/business/PaginationTable.vue'
import PaginationTable from '@/components/business/PaginationTable.vue'
import BookingStatusTag from '@/components/business/BookingStatusTag.vue'
import { getBookingHistory, type BookingHistoryItem } from '@/api/booking'

defineOptions({ name: 'CustomerBookings' })

const timeScope = ref<'UPCOMING' | 'HISTORY'>('UPCOMING')
const tableRef = ref<InstanceType<typeof PaginationTable> | null>(null)

const columns: TableColumn[] = [
  { label: 'Specialist', prop: 'specialistName', minWidth: 220 },
  { label: 'Start Time', prop: 'startTime', minWidth: 180 },
  { label: 'Duration', prop: 'duration', minWidth: 100 },
  { label: 'Status', prop: 'status', minWidth: 120 },
  { label: 'Fee', prop: 'amount', minWidth: 100 },
]

const fetchData = async (
  params: FetchDataParams,
): Promise<FetchDataResult<BookingHistoryItem>> => {
  const res = await getBookingHistory({
    pageNo: params.page,
    pageSize: params.limit,
    timeScope: timeScope.value,
  }) as unknown as { list: BookingHistoryItem[]; total: number }

  return {
    list: res.list || [],
    total: res.total || 0,
  }
}

const refreshTable = () => {
  tableRef.value?.refresh()
}

const formatDateTime = (value: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const formatAmount = (value: number) => `¥${Number(value || 0).toFixed(2)}`
</script>

<style scoped lang="scss">
.bookings-page {
  display: grid;
  gap: var(--space-4);
}

.page-card {
  padding: 24px;
}

.page-tag {
  color: var(--color-primary);
  font-weight: 600;
}

.page-desc {
  margin-top: 16px;
  color: var(--color-text-secondary);
}

.filters-card {
  display: flex;
  justify-content: flex-start;
}

.specialist-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.specialist-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

.specialist-title {
  color: var(--color-text-secondary);
  font-size: 13px;
}
</style>
