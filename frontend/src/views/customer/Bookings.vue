<template>
  <div class="bookings-page">
    <h1 class="page-title">My Bookings</h1>

    <div class="tabs-wrapper">
      <el-tabs v-model="activeTab" class="booking-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="Upcoming" name="UPCOMING"></el-tab-pane>
        <el-tab-pane label="History" name="HISTORY"></el-tab-pane>
      </el-tabs>
    </div>

    <div class="table-wrapper">
      <PaginationTable
        ref="tableRef"
        :columns="tableColumns"
        :fetchData="fetchData"
      >
        <template #datetime="{ row }">
          <div class="datetime-cell">
            <el-icon class="calendar-icon"><Calendar /></el-icon>
            <span>{{ formatDateTime(row.appointmentDateTime) }}</span>
          </div>
        </template>

        <template #expert="{ row }">
          <div class="expert-cell">
            <el-avatar
              :size="36"
              :src="row.specialistAvatar"
              class="expert-avatar"
            >
              {{ row.specialistName ? row.specialistName.charAt(0) : 'E' }}
            </el-avatar>
            <span class="expert-name">{{ row.specialistName }}</span>
          </div>
        </template>

        <template #status="{ row }">
          <BookingStatusTag :status="row.status" />
        </template>

        <template #action="{ row }">
          <div class="action-buttons">
            <CustomButton size="small" type="primary" plain class="action-btn" @click="handleAction('view', row)">
              View Details
            </CustomButton>

            <template v-if="activeTab === 'UPCOMING'">
              <CustomButton size="small" type="warning" plain class="action-btn" @click="handleAction('reschedule', row)">
                Reschedule
              </CustomButton>
              <CustomButton size="small" type="danger" plain class="action-btn" @click="handleAction('cancel', row)">
                Cancel
              </CustomButton>
            </template>

            <template v-else>
              <CustomButton size="small" type="success" plain class="action-btn" @click="handleAction('bookAgain', row)">
                Book Again
              </CustomButton>
            </template>
          </div>
        </template>
      </PaginationTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Calendar } from '@element-plus/icons-vue'
import { getBookingList } from '@/api/booking'
import type { BookingListItem } from '@/api/booking'
import type { FetchDataParams, FetchDataResult, TableColumn } from '@/components/business/PaginationTable.vue'
import PaginationTable from '@/components/business/PaginationTable.vue'
import BookingStatusTag from '@/components/business/BookingStatusTag.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'CustomerBookings' })

const activeTab = ref<'UPCOMING' | 'HISTORY'>('UPCOMING')
const tableRef = ref<InstanceType<typeof PaginationTable> | null>(null)

const tableColumns: TableColumn[] = [
  { prop: 'appointmentDateTime', label: 'Date & Time', minWidth: '200', slotName: 'datetime' },
  { prop: 'expert', label: 'Expert', minWidth: '220', slotName: 'expert' },
  { prop: 'serviceName', label: 'Service', minWidth: '200' },
  { prop: 'status', label: 'Status', minWidth: '150', slotName: 'status' },
  { prop: 'action', label: 'Action', width: '350', slotName: 'action' }
]

const fetchData = async (params: FetchDataParams): Promise<FetchDataResult<BookingListItem>> => {
  try {
    const res = await getBookingList({
      tab: activeTab.value,
      pageNo: params.page,
      pageSize: params.limit
    })

    return {
      list: (res as any).list || [],
      total: Number((res as any).total) || 0
    }
  } catch (error) {
    ElMessage.error('Failed to load bookings')
    return { list: [], total: 0 }
  }
}

const handleTabChange = () => {
  if (tableRef.value) {
    tableRef.value.refresh()
  }
}

const formatDateTime = (dtStr: string) => {
  if (!dtStr) return ''
  try {
    const cleanDtStr = dtStr.replace(' ', 'T')
    const dateObj = new Date(cleanDtStr)
    if (isNaN(dateObj.getTime())) return dtStr

    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
      day: '2-digit',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    }).format(dateObj)
  } catch {
    return dtStr
  }
}

const handleAction = (action: string, row: BookingListItem) => {
  ElMessage.info(`${action.toUpperCase()} action clicked for ${row.specialistName}`)
  // TODO: Implement actual action flows
}
</script>

<style scoped lang="scss">
@use '@/styles/variables';

.bookings-page {
  padding: var(--space-6);
  background-color: var(--color-bg-page);
  min-height: 100vh;

  .page-title {
    font-size: var(--font-size-xxl);
    font-weight: 700;
    color: var(--color-text-primary);
    margin-bottom: var(--space-6);
  }

  .tabs-wrapper {
    background-color: var(--color-bg-surface);
    padding: var(--space-4) var(--space-6) 0;
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    box-shadow: 0 2px 4px var(--color-shadow);
  }

  :deep(.booking-tabs) {
    .el-tabs__item {
      font-size: var(--font-size-md);
      font-weight: 600;
      color: var(--color-text-regular);
      padding: 0 var(--space-6);
      height: 48px;
      line-height: 48px;

      &.is-active {
        color: var(--color-primary);
      }

      &:hover {
        color: var(--color-primary-hover);
      }
    }

    .el-tabs__active-bar {
      height: 3px;
      background-color: var(--color-primary);
      border-radius: var(--radius-sm);
    }

    .el-tabs__nav-wrap::after {
      height: 1px;
      background-color: var(--color-border);
    }
  }

  .table-wrapper {
    background-color: var(--color-bg-surface);
    padding: var(--space-6);
    border-radius: 0 0 var(--radius-lg) var(--radius-lg);
    box-shadow: 0 4px 12px var(--color-shadow);
  }

  .datetime-cell {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    color: var(--color-text-regular);
    font-weight: 500;

    .calendar-icon {
      color: var(--color-primary);
      font-size: var(--font-size-lg);
    }
  }

  .expert-cell {
    display: flex;
    align-items: center;
    gap: var(--space-3);

    .expert-avatar {
      background-color: var(--color-primary-light);
      color: var(--color-primary);
      border: 1px solid var(--color-border);
    }

    .expert-name {
      font-weight: 600;
      color: var(--color-text-primary);
    }
  }

  .action-buttons {
    display: flex;
    flex-wrap: nowrap;
    justify-content: flex-start;
    gap: 8px;

    @media (max-width: 600px) {
      flex-wrap: wrap;
    }
  }

  .action-btn {
    margin: 0;

    @media (max-width: 600px) {
      flex: 1 1 100%;
    }
  }
}
</style>
