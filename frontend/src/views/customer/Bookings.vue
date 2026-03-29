<template>
  <section class="page-card">
    <p class="page-tag">Customer</p>
    <h1>My Bookings</h1>

    <div class="statuses-demo">
      <h2>Status Tag</h2>
      <div v-for="s in statuses" :key="s" class="status-row">
        <span class="status-label">{{ s }}</span>
        <BookingStatusTag :status="s" />
      </div>
    </div>

    <div class="empty-demo">
      <h2>Empty State Demo</h2>
      <EmptyPlaceholder description="No reservation records found for this period.">
        <CustomButton type="primary" size="large">Book a Consultation Now</CustomButton>
      </EmptyPlaceholder>
    </div>

    <div class="button-demo">
      <h2>Styled Buttons Demo</h2>
      <div style="display: flex; gap: 12px; align-items: center;">
        <CustomButton type="primary">Primary Button</CustomButton>
        <CustomButton>Default Button</CustomButton>
        <CustomButton type="success">Success Button</CustomButton>
        <CustomButton type="warning">Warning Button</CustomButton>
        <CustomButton type="danger">Danger Button</CustomButton>
      </div>
    </div>

    <div class="table-demo">
      <h2>Pagination Table Demo</h2>
      <PaginationTable
        :columns="tableColumns"
        :fetch-data="fetchMockBookings"
      >
        <template #status="{ row }">
          <BookingStatusTag :status="row.status" />
        </template>
      </PaginationTable>
    </div>
  </section>
</template>

<script setup lang="ts">
import { BookingStatus } from '@/constants/booking'
import BookingStatusTag from '@/components/common/BookingStatusTag.vue'
import EmptyPlaceholder from '@/components/common/EmptyPlaceholder.vue'
import PaginationTable from '@/components/common/PaginationTable.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import type { TableColumn, FetchDataParams, FetchDataResult } from '@/components/common/PaginationTable.vue'

defineOptions({ name: 'CustomerBookings' })

const statuses = Object.values(BookingStatus)

const tableColumns: TableColumn[] = [
  { label: 'Booking ID', prop: 'id', width: 120 },
  { label: 'Date', prop: 'date', width: 180 },
  { label: 'Time', prop: 'time', width: 120 },
  { label: 'Specialist', prop: 'specialist', minWidth: 150 },
  { label: 'Topic', prop: 'topic', minWidth: 200 },
  { label: 'Status', prop: 'status', slotName: 'status', width: 150 }
]

const fetchMockBookings = async (params: FetchDataParams): Promise<FetchDataResult> => {
  // Simulate API delay
  await new Promise(resolve => setTimeout(resolve, 800))

  const mockData = Array.from({ length: 45 }).map((_, idx) => ({
    id: `BK-${2026}${String(idx + 1).padStart(4, '0')}`,
    date: `2026-04-${String((idx % 28) + 1).padStart(2, '0')}`,
    time: `${String(9 + (idx % 8)).padStart(2, '0')}:00`,
    specialist: `Dr. Expert ${idx % 5 + 1}`,
    topic: `Career Consulting Session ${idx + 1}`,
    status: statuses[idx % statuses.length]
  }))

  const start = (params.page - 1) * params.limit
  const end = start + params.limit

  return {
    list: mockData.slice(start, end),
    total: mockData.length
  }
}
</script>

<style scoped>
.page-card { padding: 24px; }
.page-tag { color: var(--color-primary); font-weight: 600; }

.statuses-demo { margin-top: 16px; display: flex; flex-direction: column; gap: 12px; }
.empty-demo { margin-top: 40px; }
.button-demo { margin-top: 40px; }
.table-demo { margin-top: 40px; padding-bottom: 40px; }
.status-row { display: flex; align-items: center; gap: 12px; }
.status-label { width: 160px; font-weight: 600; color: var(--color-text-secondary); text-transform: uppercase; }
</style>
