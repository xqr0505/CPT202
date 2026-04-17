<template>
  <PaginationTable
    :columns="tableColumns"
    :fetch-data="fetchBookingHistory"
    ref="paginationTableRef"
  >
    <template #appointmentDateTime="{ row }">
      <div class="appointment-time">
        <div class="time-zone">{{ getTimeZone() }}</div>
        <div class="date-time">{{ formatDateTime(row.appointmentDateTime) }}</div>
      </div>
    </template>

    <template #specialist="{ row }">
      <div class="specialist-info">
        <el-avatar
          :src="row.specialistAvatar"
          :size="40"
          class="specialist-avatar"
        >
          {{ getInitials(row.specialistName) }}
        </el-avatar>
        <div class="specialist-details">
          <div class="specialist-name">{{ row.specialistName }}</div>
          <div class="specialist-title">{{ row.specialistTitle }}</div>
        </div>
      </div>
    </template>

    <template #serviceName="{ row }">
      <div class="service-name">{{ row.serviceName }}</div>
    </template>

    <template #status="{ row }">
      <BookingStatusTag :status="row.status" />
    </template>

    <template #action="{ row }">
      <div class="action-buttons">
        <CustomButton type="primary" size="small" @click="viewDetails(row)">
          View Details
        </CustomButton>

        <template v-if="isFutureAppointment(row.appointmentDateTime)">
          <CustomButton type="warning" size="small" @click="reschedule(row)">
            Reschedule
          </CustomButton>
          <CustomButton type="danger" size="small" @click="cancel(row)">
            Cancel
          </CustomButton>
        </template>

        <template v-else>
          <CustomButton type="success" size="small" @click="bookAgain(row)">
            Book Again
          </CustomButton>
        </template>
      </div>
    </template>
  </PaginationTable>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import PaginationTable from './PaginationTable.vue'
import BookingStatusTag from './BookingStatusTag.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import type { BookingItem } from '@/api/booking'
import type { TableColumn, FetchDataResult, FetchDataParams } from './PaginationTable.vue'

interface Props {
  fetchData: (params: FetchDataParams) => Promise<FetchDataResult<BookingItem>>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'view-details', item: BookingItem): void
  (e: 'reschedule', item: BookingItem): void
  (e: 'cancel', item: BookingItem): void
  (e: 'book-again', item: BookingItem): void
}>()

const paginationTableRef = ref()

const tableColumns: TableColumn[] = [
  {
    label: 'Date & Time',
    prop: 'appointmentDateTime',
    minWidth: '180px'
  },
  {
    label: 'Expert',
    prop: 'specialist',
    minWidth: '200px'
  },
  {
    label: 'Service',
    prop: 'serviceName',
    minWidth: '150px'
  },
  {
    label: 'Status',
    prop: 'status',
    width: '120px'
  },
  {
    label: 'Action',
    prop: 'action',
    minWidth: '300px'
  }
]

const fetchBookingHistory = async (params: FetchDataParams): Promise<FetchDataResult<BookingItem>> => {
  return await props.fetchData(params)
}

const formatDateTime = (dateTime: string): string => {
  const date = new Date(dateTime)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  })
}

const getTimeZone = (): string => {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

const getInitials = (name: string): string => {
  return name
    .split(' ')
    .map(word => word[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

const isFutureAppointment = (dateTime: string): boolean => {
  return new Date(dateTime) > new Date()
}

const viewDetails = (item: BookingItem) => {
  emit('view-details', item)
}

const reschedule = (item: BookingItem) => {
  emit('reschedule', item)
}

const cancel = (item: BookingItem) => {
  emit('cancel', item)
}

const bookAgain = (item: BookingItem) => {
  emit('book-again', item)
}

defineExpose({
  refresh: () => paginationTableRef.value?.refresh()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss';

.appointment-time {
  .time-zone {
    font-size: var(--font-size-sm, 12px);
    color: var(--color-text-tertiary);
    margin-bottom: var(--space-1);
  }

  .date-time {
    font-weight: 600;
    color: var(--color-text-primary);
  }
}

.specialist-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);

  .specialist-avatar {
    flex-shrink: 0;
  }

  .specialist-details {
    flex: 1;
    min-width: 0;

    .specialist-name {
      font-weight: 600;
      color: var(--color-text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .specialist-title {
      font-size: var(--font-size-sm, 12px);
      color: var(--color-text-secondary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

.service-name {
  color: var(--color-text-primary);
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;

  :deep(.custom-button) {
    padding: var(--space-1) var(--space-3);
    font-size: var(--font-size-sm, 12px);
    height: auto;
    min-height: 28px;
  }
}

// Responsive design
@media (max-width: 768px) {
  .action-buttons {
    flex-direction: column;
    gap: var(--space-1);

    :deep(.custom-button) {
      width: 100%;
      justify-content: center;
    }
  }

  .specialist-info {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }
}
</style>
