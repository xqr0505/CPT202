<template>
  <section class="bookings-page">
    <div class="tabs-bar">
      <div
        class="tab-item"
        :class="{ active: currentTab === 'upcoming' }"
        @click="switchTab('upcoming')"
      >
        Upcoming
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'history' }"
        @click="switchTab('history')"
      >
        History
      </div>
    </div>

    <div class="table-container">
      <PaginationTable
        ref="tableRef"
        :columns="tableColumns"
        :fetchData="fetchBookings"
      >
        <!-- Date & Time Slot -->
        <template #dateTime="{ row }">
          <div class="date-time-cell">
            <span class="date">{{ formatDate(row.startTime) }}</span>
            <span class="time">{{ formatTime(row.startTime) }}</span>
          </div>
        </template>

        <!-- Expert Slot -->
        <template #expert="{ row }">
          <div class="expert-cell">
            <el-avatar :src="row.specialistAvatar" :size="36" class="expert-avatar">
              {{ row.specialistName?.charAt(0) || 'E' }}
            </el-avatar>
            <div class="expert-info">
              <span class="expert-name">{{ row.specialistName }}</span>
              <span class="expert-title" v-if="row.specialistTitle">{{ row.specialistTitle }}</span>
            </div>
          </div>
        </template>

        <!-- Service Slot -->
        <template #service="{ row }">
          <span class="service-name">{{ row.serviceName }}</span>
        </template>

        <!-- Status Slot -->
        <template #status="{ row }">
          <BookingStatusTag :status="row.status" />
        </template>

        <!-- Action Slot -->
        <template #action="{ row }">
          <div class="action-cell">
            <template v-if="currentTab === 'upcoming'">
              <CustomButton size="small" @click="handleViewDetails(row)">View Details</CustomButton>
              <CustomButton size="small" type="primary" plain @click="handleReschedule(row)">Reschedule</CustomButton>
              <CustomButton size="small" type="danger" plain @click="handleCancel(row)">Cancel</CustomButton>
            </template>
            <template v-else>
              <CustomButton size="small" @click="handleViewDetails(row)">View Details</CustomButton>
              <CustomButton size="small" type="primary" @click="handleBookAgain(row)">Book Again</CustomButton>
            </template>
          </div>
        </template>
      </PaginationTable>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { getUnifiedBookings } from '@/api/booking'
import PaginationTable from '@/components/business/PaginationTable.vue'
import BookingStatusTag from '@/components/business/BookingStatusTag.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import dayjs from 'dayjs'
import type { FetchDataParams } from '@/components/business/PaginationTable.vue'
import type { UnifiedBookingItem } from '@/api/booking'

defineOptions({ name: 'CustomerBookings' })

const currentTab = ref<'upcoming' | 'history'>('upcoming')
const tableRef = ref<InstanceType<typeof PaginationTable> | null>(null)

const tableColumns = computed(() => [
  { prop: 'dateTime', label: 'Date & Time', minWidth: 150, slotName: 'dateTime' },
  { prop: 'expert', label: 'Expert', minWidth: 200, slotName: 'expert' },
  { prop: 'service', label: 'Service', minWidth: 150, slotName: 'service' },
  { prop: 'status', label: 'Status', minWidth: 120, slotName: 'status' },
  { prop: 'action', label: 'Action', minWidth: 280, slotName: 'action' }
])

const switchTab = (tab: 'upcoming' | 'history') => {
  if (currentTab.value === tab) return
  currentTab.value = tab
  nextTick(() => {
    tableRef.value?.refresh()
  })
}

const fetchBookings = async (params: FetchDataParams) => {
  const { data } = await getUnifiedBookings({
    pageNo: params.page,
    pageSize: params.limit,
    type: currentTab.value
  })
  return {
    list: data.list,
    total: data.total
  }
}

const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('MMM DD, YYYY')
}

const formatTime = (dateStr: string) => {
  return dayjs(dateStr).format('HH:mm A')
}

// Action Handlers
const handleViewDetails = (row: UnifiedBookingItem) => {
  console.log('View details', row.id)
}
const handleReschedule = (row: UnifiedBookingItem) => {
  console.log('Reschedule', row.id)
}
const handleCancel = (row: UnifiedBookingItem) => {
  console.log('Cancel', row.id)
}
const handleBookAgain = (row: UnifiedBookingItem) => {
  console.log('Book again', row.originalBookingId || row.id)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.bookings-page {
  padding: 24px;
  background-color: var(--color-background-soft);
  min-height: 100%;
}

.tabs-bar {
  display: flex;
  gap: 32px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 8px;
}

.tab-item {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text-regular);
  cursor: pointer;
  position: relative;
  transition: color 0.3s;

  &:hover {
    color: var(--color-primary);
  }

  &.active {
    color: var(--color-primary);
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      bottom: -9px;
      left: 0;
      width: 100%;
      height: 2px;
      background-color: var(--color-primary);
    }
  }
}

.table-container {
  background: var(--color-background);
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  padding: 20px;
}

.date-time-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .date {
    font-weight: 500;
    color: var(--color-text-primary);
  }

  .time {
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}

.expert-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .expert-avatar {
    flex-shrink: 0;
  }

  .expert-info {
    display: flex;
    flex-direction: column;

    .expert-name {
      font-weight: 500;
      color: var(--color-text-primary);
    }

    .expert-title {
      font-size: 13px;
      color: var(--color-text-secondary);
    }
  }
}

.service-name {
  color: var(--color-text-regular);
  font-weight: 500;
}

.action-cell {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
