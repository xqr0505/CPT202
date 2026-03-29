<template>
  <div class="dev-demo-page">
    <h1>Development Component Demos</h1>
    <p class="desc">A sandbox for testing global components.</p>

    <div class="section-card">
      <h2>BookingStatusTag</h2>
      <div class="demo-flex">
        <BookingStatusTag v-for="s in statuses" :key="s" :status="s" />
      </div>
    </div>

    <div class="section-card">
      <h2>CustomButton</h2>
      <div class="demo-flex">
        <CustomButton type="primary">Primary Button</CustomButton>
        <CustomButton>Default Button</CustomButton>
        <CustomButton type="success">Success Button</CustomButton>
        <CustomButton type="warning">Warning Button</CustomButton>
        <CustomButton type="danger">Danger Button</CustomButton>
      </div>
    </div>

    <div class="section-card">
      <h2>EmptyPlaceholder</h2>
      <EmptyPlaceholder description="No reservation records found for this period.">
        <CustomButton type="primary" size="large">Book a Consultation Now</CustomButton>
      </EmptyPlaceholder>
    </div>

    <div class="section-card">
      <h2>PaginationTable</h2>
      <PaginationTable
        :columns="tableColumns"
        :fetch-data="fetchMockBookings"
      >
        <template #status="{ row }">
          <BookingStatusTag :status="row.status" />
        </template>
      </PaginationTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { BookingStatus } from '@/constants/booking.ts'
import BookingStatusTag from '@/components/common/BookingStatusTag.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import EmptyPlaceholder from '@/components/common/EmptyPlaceholder.vue'
import PaginationTable from '@/components/common/PaginationTable.vue'
import type { TableColumn, FetchDataParams, FetchDataResult } from '@/components/common/PaginationTable.vue'

defineOptions({ name: 'DevDemo' })

const statuses = Object.values(BookingStatus)

const tableColumns: TableColumn[] = [
  { label: 'ID', prop: 'id', width: 100 },
  { label: 'Date', prop: 'date', width: 150 },
  { label: 'Time', prop: 'time', width: 100 },
  { label: 'Specialist', prop: 'specialist', minWidth: 150 },
  { label: 'Topic', prop: 'topic', minWidth: 200 },
  { label: 'Status', prop: 'status', slotName: 'status', width: 150 }
]

const fetchMockBookings = async (params: FetchDataParams): Promise<FetchDataResult> => {
  await new Promise(resolve => setTimeout(resolve, 800))

  const mockData = Array.from({ length: 45 }).map((_, idx) => ({
    id: `BK-2026${String(idx + 1).padStart(4, '0')}`,
    date: `2026-04-${String((idx % 28) + 1).padStart(2, '0')}`,
    time: `${String(9 + (idx % 8)).padStart(2, '0')}:00`,
    specialist: `Dr. Expert ${idx % 5 + 1}`,
    topic: `Career Consulting ${idx + 1}`,
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

<style scoped lang="scss">
@use '@/styles/variables';

.dev-demo-page {
  padding: var(--space-6);
  max-width: var(--content-max-width);
  margin: 0 auto;

  h1 {
    color: var(--color-text-primary);
    margin-bottom: var(--space-1);
  }

  .desc {
    color: var(--color-text-secondary);
    margin-bottom: var(--space-6);
  }
}

.section-card {
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  margin-bottom: var(--space-6);
  box-shadow: 0 4px 12px var(--color-shadow);

  h2 {
    margin-top: 0;
    margin-bottom: var(--space-4);
    font-size: 1.25rem;
    color: var(--color-primary);
    border-bottom: 1px solid var(--color-border);
    padding-bottom: var(--space-2);
  }
}

.demo-flex {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  align-items: center;
}
</style>

