<template>
  <div class="bookings-page">
    <h1 class="page-title">My Bookings</h1>

    <div class="tabs-wrapper">
      <el-tabs v-model="activeTab" class="booking-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="Upcoming" name="UPCOMING"></el-tab-pane>
        <el-tab-pane label="History" name="HISTORY"></el-tab-pane>
      </el-tabs>
    </div>

    <div class="filter-wrapper">
      <div class="status-filter">
        <CustomButton
          v-for="option in statusOptions"
          :key="option.value"
          :type="activeStatus === option.value ? 'primary' : 'default'"
          :class="['status-btn', { 'is-active': activeStatus === option.value }]"
          @click="activeStatus = option.value; handleStatusChange()"
        >
          {{ option.label }}
        </CustomButton>
      </div>
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
              <CustomButton size="small" type="warning" plain class="action-btn" :disabled="!canRescheduleBooking(row)" @click="handleAction('reschedule', row)">
                Reschedule
              </CustomButton>
              <CustomButton size="small" type="danger" plain class="action-btn" :disabled="!canCancelBooking(row)" @click="handleAction('cancel', row)">
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

        <template #mobile-item="{ row }">
          <div class="booking-card">
            <div class="card-header">
              <div class="datetime-cell">
                <el-icon class="calendar-icon"><Calendar /></el-icon>
                <span>{{ formatDateTime(row.appointmentDateTime) }}</span>
              </div>
              <BookingStatusTag :status="row.status" />
            </div>
            <div class="card-body">
              <div class="info-row">
                <span class="info-label">Expert:</span>
                <span class="info-value">
                  <el-avatar :size="24" :src="row.specialistAvatar" class="expert-avatar-small">
                    {{ row.specialistName ? row.specialistName.charAt(0) : 'E' }}
                  </el-avatar>
                  {{ row.specialistName }}
                </span>
              </div>
              <div class="info-row">
                <span class="info-label">Service:</span>
                <span class="info-value">{{ row.serviceName }}</span>
              </div>
            </div>
            <div class="card-footer">
              <CustomButton class="mobile-action-btn" type="primary" plain @click="handleAction('view', row)">
                View
              </CustomButton>

              <template v-if="activeTab === 'UPCOMING'">
                <CustomButton class="mobile-action-btn" type="warning" plain :disabled="!canRescheduleBooking(row)" @click="handleAction('reschedule', row)">
                  Reschedule
                </CustomButton>
                <CustomButton class="mobile-action-btn" type="danger" plain :disabled="!canCancelBooking(row)" @click="handleAction('cancel', row)">
                  Cancel
                </CustomButton>
              </template>

              <template v-else>
                <CustomButton class="mobile-action-btn" type="success" plain @click="handleAction('bookAgain', row)">
                  Book Again
                </CustomButton>
              </template>
            </div>
          </div>
        </template>
      </PaginationTable>
    </div>
  </div>
  <BookingDetailModal v-model="showDetailModal" />
  <el-dialog
    v-model="showCancelDialog"
    title="Cancel Booking"
    width="500px"
    :close-on-click-modal="false"
    @closed="resetCancelDialog"
  >
    <div v-loading="cancelQuoteLoading" class="cancel-dialog-content">
      <p class="cancel-dialog-tip">
        Please confirm your cancellation. Refund and penalty will be calculated automatically according to the cancellation policy.
      </p>

      <div v-if="cancelQuote" class="cancel-finance-card">
        <div class="finance-row">
          <span>Refund Amount：</span>
          <strong class="refund-amount">{{ formatMoney(cancelQuote.refundAmount) }}</strong>
        </div>
        <div class="finance-row">
          <span>Penalty Amount：</span>
          <strong class="penalty-amount">{{ formatMoney(cancelQuote.penaltyAmount) }}</strong>
        </div>
      </div>

      <el-alert
        v-if="cancelQuote?.message"
        :title="cancelQuote.message"
        :type="cancelQuote?.allowed ? 'info' : 'warning'"
        :closable="false"
        show-icon
      />
    </div>

    <template #footer>
      <span class="dialog-footer">
        <CustomButton @click="showCancelDialog = false">Keep Booking</CustomButton>
        <CustomButton
          type="danger"
          :loading="cancelConfirmLoading"
          :disabled="cancelQuoteLoading || !cancelQuote?.allowed"
          @click="confirmCancel"
        >
          Confirm Cancel
        </CustomButton>
      </span>
    </template>
  </el-dialog>
  <el-dialog
    v-model="showRescheduleDialog"
    title="Reschedule Booking"
    width="560px"
    :close-on-click-modal="false"
    @closed="resetRescheduleDialog"
  >
    <div class="reschedule-dialog-content">
      <p class="cancel-dialog-tip">
        Choose a new time slot first. We will calculate the final amount before you confirm.
      </p>

      <div class="reschedule-picker-row">
        <el-date-picker
          v-model="rescheduleDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="Select date"
          :disabled="rescheduleQuoteLoading || rescheduleConfirmLoading"
          @change="loadRescheduleAvailability"
        />
      </div>

      <div v-loading="rescheduleAvailabilityLoading" class="reschedule-slot-grid">
        <EmptyPlaceholder
          v-if="!rescheduleAvailability.length"
          description="No available slots on this date."
        />
        <button
          v-for="slot in rescheduleAvailability"
          v-else
          :key="slot.id"
          type="button"
          class="slot-chip"
          :class="{ active: selectedRescheduleSlotId === slot.id }"
          @click="handleRescheduleSlotSelect(slot.id)"
        >
          <span class="slot-time">{{ formatSlotTime(slot.startTime) }} - {{ formatSlotTime(slot.endTime) }}</span>
        </button>
      </div>

      <div v-if="rescheduleQuote" v-loading="rescheduleQuoteLoading" class="cancel-finance-card">
        <div class="finance-row">
          <span>Price Difference：</span>
          <strong>{{ formatMoney(rescheduleQuote.priceDifference) }}</strong>
        </div>
        <div class="finance-row">
          <span>Penalty Amount：</span>
          <strong class="penalty-amount">{{ formatMoney(rescheduleQuote.penaltyAmount) }}</strong>
        </div>
        <div class="finance-row">
          <span>Refund Amount：</span>
          <strong class="refund-amount">{{ formatMoney(rescheduleQuote.refundAmount) }}</strong>
        </div>
        <div class="finance-row">
          <span>Payable Amount：</span>
          <strong>{{ formatMoney(rescheduleQuote.payableAmount) }}</strong>
        </div>
      </div>

      <el-alert
        v-if="rescheduleQuote?.message"
        :title="rescheduleQuote.message"
        :type="rescheduleQuote?.allowed ? 'info' : 'warning'"
        :closable="false"
        show-icon
      />
    </div>

    <template #footer>
      <span class="dialog-footer">
        <CustomButton @click="showRescheduleDialog = false">Cancel</CustomButton>
        <CustomButton
          type="warning"
          :loading="rescheduleConfirmLoading"
          :disabled="!selectedRescheduleSlotId || rescheduleQuoteLoading || !rescheduleQuote?.allowed"
          @click="confirmReschedule"
        >
          Confirm Reschedule
        </CustomButton>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Calendar } from '@element-plus/icons-vue'
import {confirmBookingCancel, confirmBookingReschedule, getBookingCancelQuote, getBookingList, getBookingRescheduleQuote} from '@/api/booking'
import type { BookingListItem } from '@/api/booking'
import type { FetchDataParams, FetchDataResult, TableColumn } from '@/components/business/PaginationTable.vue'
import PaginationTable from '@/components/business/PaginationTable.vue'
import BookingStatusTag from '@/components/business/BookingStatusTag.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import BookingDetailModal from '@/components/business/BookingDetailModal.vue'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'
import { fetchSpecialistAvailability } from '@/api/specialist'
import type { SpecialistAvailabilitySlot } from '@/types/specialist'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router';
import { BOOKING_STATUS } from '@/constants/booking';

defineOptions({ name: 'CustomerBookings' })

const activeTab = ref<'UPCOMING' | 'HISTORY'>('UPCOMING')
const activeStatus = ref<string>('null')
const tableRef = ref<InstanceType<typeof PaginationTable> | null>(null)
const router = useRouter();
const route = useRoute();

const showDetailModal = ref(false);
const showCancelDialog = ref(false);
const cancelQuoteLoading = ref(false);
const cancelConfirmLoading = ref(false);
const selectedBooking = ref<BookingListItem | null>(null);
const cancelQuote = ref<Awaited<ReturnType<typeof getBookingCancelQuote>> | null>(null);
const showRescheduleDialog = ref(false);
const rescheduleAvailabilityLoading = ref(false);
const rescheduleQuoteLoading = ref(false);
const rescheduleConfirmLoading = ref(false);
const rescheduleDate = ref('');
const rescheduleAvailability = ref<SpecialistAvailabilitySlot[]>([]);
const selectedRescheduleSlotId = ref<number | null>(null);
const rescheduleQuote = ref<Awaited<ReturnType<typeof getBookingRescheduleQuote>> | null>(null);

const checkQueryAndOpenModal = () => {
  if (route.query.bookingId) {
    showDetailModal.value = true;
  }
};

onMounted(() => {
  checkQueryAndOpenModal();
});

const statusOptions = [
  { label: 'All', value: 'null' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Confirmed', value: 'CONFIRMED' },
  { label: 'Cancelled', value: 'CANCELLED' },
  { label: 'Completed', value: 'COMPLETED' }
]

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
      status: activeStatus.value === 'null' ? undefined : activeStatus.value,
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

const handleStatusChange = () => {
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

const normalizeStatus = (status: string) => String(status || '').toUpperCase();

const canCancelBooking = (row: BookingListItem) => {
  const normalizedStatus = normalizeStatus(row.status);
  const isCancelableStatus = normalizedStatus === 'PENDING' || normalizedStatus === 'CONFIRMED';
  if (!isCancelableStatus) {
    return false;
  }
  const bookingTime = new Date(row.appointmentDateTime.replace(' ', 'T')).getTime();
  if (Number.isNaN(bookingTime)) {
    return true;
  }
  const diffMs = bookingTime - Date.now();
  return diffMs > 2 * 60 * 60 * 1000;
};

const canRescheduleBooking = (row: BookingListItem) => canCancelBooking(row);

const formatMoney = (value?: number) => {
  const amount = Number(value ?? 0);
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '¥0.00';
};

const formatSlotTime = (time?: string) => {
  if (!time) return '';
  const parts = String(time).split(':');
  if (parts.length >= 2) {
    return `${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}`;
  }
  return String(time);
};

const openCancelDialog = async (row: BookingListItem) => {
  if (!canCancelBooking(row)) {
    ElMessage.warning('Cancellation is only available for bookings more than 2 hours away.');
    return;
  }

  selectedBooking.value = row;
  showCancelDialog.value = true;
  cancelQuoteLoading.value = true;

  try {
    cancelQuote.value = await getBookingCancelQuote(row.id);
  } catch {
    cancelQuote.value = null;
    ElMessage.error('Failed to calculate refund quote. Please try again.');
  } finally {
    cancelQuoteLoading.value = false;
  }
};

const resetCancelDialog = () => {
  selectedBooking.value = null;
  cancelQuote.value = null;
  cancelQuoteLoading.value = false;
  cancelConfirmLoading.value = false;
};

const toLocalDate = (dtStr: string) => {
  if (!dtStr) return '';
  const cleanDtStr = dtStr.replace(' ', 'T');
  const dateObj = new Date(cleanDtStr);
  if (isNaN(dateObj.getTime())) return '';
  const year = dateObj.getFullYear();
  const month = `${dateObj.getMonth() + 1}`.padStart(2, '0');
  const day = `${dateObj.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const loadRescheduleAvailability = async () => {
  if (!selectedBooking.value || !rescheduleDate.value) {
    rescheduleAvailability.value = [];
    return;
  }
  rescheduleAvailabilityLoading.value = true;
  try {
    const slots = await fetchSpecialistAvailability(Number(selectedBooking.value.specialistId), rescheduleDate.value);
    rescheduleAvailability.value = (slots || []).filter((slot) => slot.status === 'AVAILABLE');
    if (!rescheduleAvailability.value.some((slot) => slot.id === selectedRescheduleSlotId.value)) {
      selectedRescheduleSlotId.value = null;
      rescheduleQuote.value = null;
    }
  } catch {
    rescheduleAvailability.value = [];
    ElMessage.error('Failed to load available slots.');
  } finally {
    rescheduleAvailabilityLoading.value = false;
  }
};

const openRescheduleDialog = async (row: BookingListItem) => {
  if (!canRescheduleBooking(row)) {
    ElMessage.warning('Reschedule is only available for bookings more than 2 hours away.');
    return;
  }
  selectedBooking.value = row;
  rescheduleDate.value = toLocalDate(row.appointmentDateTime);
  showRescheduleDialog.value = true;
  await loadRescheduleAvailability();
};

const handleRescheduleSlotSelect = async (slotId: number) => {
  if (!selectedBooking.value) {
    return;
  }
  selectedRescheduleSlotId.value = slotId;
  rescheduleQuoteLoading.value = true;
  try {
    rescheduleQuote.value = await getBookingRescheduleQuote(selectedBooking.value.id, slotId);
  } catch {
    rescheduleQuote.value = null;
    ElMessage.error('Failed to calculate reschedule quote. Please try again.');
  } finally {
    rescheduleQuoteLoading.value = false;
  }
};

const resetRescheduleDialog = () => {
  selectedBooking.value = null;
  rescheduleDate.value = '';
  rescheduleAvailability.value = [];
  selectedRescheduleSlotId.value = null;
  rescheduleQuote.value = null;
  rescheduleAvailabilityLoading.value = false;
  rescheduleQuoteLoading.value = false;
  rescheduleConfirmLoading.value = false;
};

const confirmReschedule = async () => {
  if (!selectedBooking.value || !selectedRescheduleSlotId.value || !rescheduleQuote.value?.allowed) {
    return;
  }
  rescheduleConfirmLoading.value = true;
  try {
    const result = await confirmBookingReschedule(selectedBooking.value.id, selectedRescheduleSlotId.value);
    ElMessage.success(result.message || 'Booking rescheduled successfully.');
    showRescheduleDialog.value = false;
    tableRef.value?.refresh();
  } catch {
    ElMessage.error('Failed to reschedule booking. Please try again.');
  } finally {
    rescheduleConfirmLoading.value = false;
  }
};

const confirmCancel = async () => {
  if (!selectedBooking.value || !cancelQuote.value?.allowed) {
    return;
  }
  cancelConfirmLoading.value = true;
  try {
    const result = await confirmBookingCancel(selectedBooking.value.id);
    ElMessage.success(result.message || 'Booking cancelled successfully.');
    showCancelDialog.value = false;
    tableRef.value?.refresh();
  } catch {
    ElMessage.error('Failed to cancel booking. Please try again.');
  } finally {
    cancelConfirmLoading.value = false;
  }
};

const handleAction = (action: string, row: BookingListItem) => {
  switch (action) {
    case 'view':
      router.push({ query: { ...route.query, bookingId: row.id } }).then(() => {
        showDetailModal.value = true;
      });
      break;
    case 'reschedule':
      void openRescheduleDialog(row);
      break;
    case 'cancel':
      void openCancelDialog(row);
      break;
    case 'bookAgain':
      router.push(`/customer/specialists/${row.specialistId}?from=/customer/specialists`);
      break;
    default:
      ElMessage.warning('Action not implemented');
  }
};
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

  .filter-wrapper {
    background-color: var(--color-bg-surface);
    padding: var(--space-4) var(--space-6);
    border-radius: 0;
    box-shadow: 0 4px 12px var(--color-shadow);

    .status-filter {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-4);

      :deep(.status-btn) {
        color: var(--color-text-primary);
        background: var(--color-btn-bg-default);
        border: 1px solid var(--color-btn-border-default);
        font-weight: 500;
        border-radius: var(--radius-md);
        transition: all var(--transition-base);
      }
      :deep(.status-btn.is-active) {
        color: var(--color-btn-text-primary);
        background: var(--color-btn-bg-primary);
        border-color: var(--color-btn-border-primary);
      }
      :deep(.status-btn):hover {
        background: var(--color-btn-bg-default-hover);
        color: var(--color-text-primary);
        border-color: var(--color-btn-border-default-hover);
        filter: brightness(1.03);
        box-shadow: 0 2px 6px var(--color-shadow);
      }
      :deep(.status-btn.is-active):hover {
        background: var(--color-btn-bg-primary-hover);
        color: var(--color-btn-text-primary-hover);
        border-color: var(--color-btn-border-primary-hover);
        box-shadow: 0 2px 8px var(--color-btn-shadow-primary);
        filter: brightness(1.03);
      }
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

  .booking-card {
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    padding: var(--space-4);
    margin-bottom: var(--space-4);
    display: flex;
    flex-direction: column;
    gap: var(--space-3);

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--color-border-light, #ebeef5);
      padding-bottom: var(--space-2);
    }

    .card-body {
      display: flex;
      flex-direction: column;
      gap: var(--space-2);

      .info-row {
        display: flex;
        align-items: center;
        gap: var(--space-2);

        .info-label {
          color: var(--color-text-secondary);
          width: 60px;
          font-size: var(--font-size-sm);
        }

        .info-value {
          display: flex;
          align-items: center;
          gap: var(--space-2);
          color: var(--color-text-primary);
          font-weight: 500;
          font-size: var(--font-size-sm);

          .expert-avatar-small {
            background-color: var(--color-primary-light);
            color: var(--color-primary);
          }
        }
      }
    }

    .card-footer {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-2);
      padding-top: var(--space-2);
      border-top: 1px solid var(--color-border-light, #ebeef5);

      .mobile-action-btn {
        flex: 1;
        min-height: 44px;
        margin: 0;
      }
    }
  }

  .cancel-dialog-content {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .reschedule-dialog-content {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .reschedule-picker-row {
    display: flex;
    justify-content: flex-start;
  }

  .reschedule-slot-grid {
    display: grid;
    gap: var(--space-3);
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  }

  .slot-chip {
    min-height: 68px;
    padding: var(--space-5);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background: var(--color-bg-page);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
  }

  .slot-chip:hover {
    transform: translateY(-1px);
  }

  .slot-chip.active {
    border-color: var(--color-primary);
    background: rgba(51, 144, 251, 0.1);
  }

  .slot-chip span {
    color: var(--color-text-primary);
    font-weight: 700;
  }

  .slot-chip .slot-time {
    color: var(--color-text-primary);
    font-weight: 700;
    font-size: 18px;
    line-height: 1.3;
  }

  .cancel-dialog-tip {
    margin: 0;
    color: var(--color-text-secondary);
  }

  .cancel-finance-card {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    padding: var(--space-4);
    background: var(--color-bg-page);
  }

  .finance-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: var(--color-text-primary);
    margin-bottom: var(--space-2);
  }

  .finance-row:last-child {
    margin-bottom: 0;
  }

  .refund-amount {
    color: var(--color-success);
  }

  .penalty-amount {
    color: var(--color-danger);
  }
}
</style>
