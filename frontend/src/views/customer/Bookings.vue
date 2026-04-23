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
      <div class="status-filter-scroll">
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
    </div>

    <div class="table-wrapper">
      <PaginationTable
        ref="tableRef"
        :columns="tableColumns"
        :fetchData="fetchData"
        :rowClassName="resolveRowClassName"
      >
        <template #datetime="{ row }">
          <div class="datetime-cell">
            <el-icon class="calendar-icon"><Calendar /></el-icon>
            <span>{{ formatDateTime(bookingRow(row).appointmentDateTime) }}</span>
          </div>
        </template>

        <template #expert="{ row }">
          <div class="expert-cell">
            <el-avatar
              :size="36"
              :src="bookingRow(row).specialistAvatar"
              class="expert-avatar"
            >
              {{ bookingRow(row).specialistName ? bookingRow(row).specialistName.charAt(0) : 'E' }}
            </el-avatar>
            <span class="expert-name">{{ bookingRow(row).specialistName }}</span>
          </div>
        </template>

        <template #status="{ row }">
          <BookingStatusTag :status="bookingRow(row).status" />
        </template>

        <template #action="{ row }">
          <div class="action-buttons">
            <el-tooltip content="View Details" placement="top">
              <CustomButton size="small" type="primary" circle class="action-btn-circle" @click="handleAction('view', bookingRow(row))">
                <el-icon><View /></el-icon>
              </CustomButton>
            </el-tooltip>

            <template v-if="activeTab === 'UPCOMING'">
              <el-tooltip content="Reschedule" placement="top">
                <CustomButton size="small" type="warning" circle class="action-btn-circle" :disabled="!canRescheduleBooking(bookingRow(row))" @click="handleAction('reschedule', bookingRow(row))">
                  <el-icon><Edit /></el-icon>
                </CustomButton>
              </el-tooltip>
              <el-tooltip content="Cancel" placement="top">
                <CustomButton size="small" type="danger" circle class="action-btn-circle" :disabled="!canCancelBooking(bookingRow(row))" @click="handleAction('cancel', bookingRow(row))">
                  <el-icon><Close /></el-icon>
                </CustomButton>
              </el-tooltip>
            </template>

            <template v-else>
              <el-tooltip content="Book Again" placement="top">
                <CustomButton size="small" type="success" circle class="action-btn-circle" @click="handleAction('bookAgain', bookingRow(row))">
                  <el-icon><RefreshRight /></el-icon>
                </CustomButton>
              </el-tooltip>
            </template>
          </div>
        </template>

        <template #mobile-item="{ row }">
          <div class="booking-card" :class="{ 'booking-card-highlight': isHighlightedBooking(bookingRow(row)) }">
            <div class="card-header">
              <div class="datetime-cell">
                <el-icon class="calendar-icon"><Calendar /></el-icon>
                <span>{{ formatDateTime(bookingRow(row).appointmentDateTime) }}</span>
              </div>
              <BookingStatusTag :status="bookingRow(row).status" />
            </div>
            <div class="card-body">
              <div class="info-row">
                <span class="info-label">Expert:</span>
                <span class="info-value">
                  <el-avatar :size="24" :src="bookingRow(row).specialistAvatar" class="expert-avatar-small">
                    {{ bookingRow(row).specialistName ? bookingRow(row).specialistName.charAt(0) : 'E' }}
                  </el-avatar>
                  {{ bookingRow(row).specialistName }}
                </span>
              </div>
              <div class="info-row">
                <span class="info-label">Service:</span>
                <span class="info-value">{{ bookingRow(row).serviceName }}</span>
              </div>
            </div>
            <div class="card-footer">
              <el-tooltip content="View Details" placement="top">
                <CustomButton class="mobile-action-btn-circle" size="small" type="primary" circle @click="handleAction('view', bookingRow(row))">
                  <el-icon><View /></el-icon>
                </CustomButton>
              </el-tooltip>

              <template v-if="activeTab === 'UPCOMING'">
                <el-tooltip content="Reschedule" placement="top">
                  <CustomButton class="mobile-action-btn-circle" size="small" type="warning" circle :disabled="!canRescheduleBooking(bookingRow(row))" @click="handleAction('reschedule', bookingRow(row))">
                    <el-icon><Edit /></el-icon>
                  </CustomButton>
                </el-tooltip>
                <el-tooltip content="Cancel" placement="top">
                  <CustomButton class="mobile-action-btn-circle" size="small" type="danger" circle :disabled="!canCancelBooking(bookingRow(row))" @click="handleAction('cancel', bookingRow(row))">
                    <el-icon><Close /></el-icon>
                  </CustomButton>
                </el-tooltip>
              </template>

              <template v-else>
                <el-tooltip content="Book Again" placement="top">
                  <CustomButton class="mobile-action-btn-circle" size="small" type="success" circle @click="handleAction('bookAgain', bookingRow(row))">
                    <el-icon><RefreshRight /></el-icon>
                  </CustomButton>
                </el-tooltip>
              </template>
            </div>
          </div>
        </template>
      </PaginationTable>
    </div>
  </div>
  <BookingDetailModal v-model="showDetailModal" @action="(data) => handleAction(data.action, data.row)" />
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
          <span>Refund Amount:</span>
          <strong class="refund-amount">{{ formatMoney(cancelQuote.refundAmount) }}</strong>
        </div>
        <div class="finance-row">
          <span>Penalty Amount:</span>
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
          <span>Price Difference:</span>
          <strong>{{ formatMoney(rescheduleQuote.priceDifference) }}</strong>
        </div>
        <div class="finance-row">
          <span>Penalty Amount:</span>
          <strong class="penalty-amount">{{ formatMoney(rescheduleQuote.penaltyAmount) }}</strong>
        </div>
        <div class="finance-row">
          <span>Refund Amount:</span>
          <strong class="refund-amount">{{ formatMoney(rescheduleQuote.refundAmount) }}</strong>
        </div>
        <div class="finance-row">
          <span>Payable Amount:</span>
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
import { computed, ref, onMounted, watch } from 'vue'
import { Calendar, View, Edit, Close, RefreshRight } from '@element-plus/icons-vue'
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
const highlightedBookingId = computed(() => {
  const raw = typeof route.query.highlightBookingId === 'string'
    ? route.query.highlightBookingId.trim()
    : '';
  return raw || '';
});

const checkQueryAndOpenModal = () => {
  if (route.query.bookingId) {
    showDetailModal.value = true;
  }
};

onMounted(() => {
  checkQueryAndOpenModal();
});

watch(
  () => route.query.highlightBookingId,
  (value, previousValue) => {
    const highlightId = typeof value === 'string' ? value.trim() : '';
    if (!highlightId) {
      return;
    }

    activeTab.value = 'UPCOMING';
    activeStatus.value = 'null';
    tableRef.value?.refresh();

    const previousHighlightId = typeof previousValue === 'string' ? previousValue.trim() : '';
    if (highlightId !== previousHighlightId) {
      ElMessage.success(`Booking #${highlightId} was created. Highlighted in list.`);
    }
  },
  { immediate: true }
);

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

    const payload = res as unknown as { list?: BookingListItem[]; total?: number | string }
    const rawList: BookingListItem[] = payload.list ?? []
    const sortedList =
      activeTab.value === 'UPCOMING'
        ? [...rawList].sort((a, b) => {
            const aTime = Date.parse(String(a.appointmentDateTime || '').replace(' ', 'T'))
            const bTime = Date.parse(String(b.appointmentDateTime || '').replace(' ', 'T'))
            if (Number.isNaN(aTime) && Number.isNaN(bTime)) return 0
            if (Number.isNaN(aTime)) return 1
            if (Number.isNaN(bTime)) return -1
            return aTime - bTime
          })
        : rawList

    return {
      list: sortedList,
      total: Number(payload.total) || 0
    }
  } catch {
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

const bookingRow = (row: unknown) => row as BookingListItem;

const isHighlightedBooking = (row: BookingListItem) =>
  Boolean(highlightedBookingId.value) && String(row.id) === highlightedBookingId.value;

const resolveRowClassName = ({ row }: { row: unknown; rowIndex: number }) =>
  isHighlightedBooking(bookingRow(row)) ? 'booking-row-highlight' : '';

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
  const [hour, minute] = parts;
  if (hour && minute) {
    return `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`;
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
    font-size: 28px;
    font-weight: 800;
    color: var(--color-text-primary);
    margin-bottom: var(--space-8);
    letter-spacing: -0.02em;
  }

  .tabs-wrapper {
    margin-bottom: var(--space-4);
  }

  :deep(.booking-tabs) {
    .el-tabs__header {
      margin: 0;
      border: none;
    }

    .el-tabs__item {
      font-size: 18px;
      font-weight: 700;
      color: var(--color-text-secondary);
      padding: 0 var(--space-6);
      height: 56px;
      line-height: 56px;
      transition: all var(--transition-base);

      &.is-active {
        color: var(--color-primary);
      }
    }

    .el-tabs__active-bar {
      height: 4px;
      border-radius: 2px;
      background-color: var(--color-primary);
    }

    .el-tabs__nav-wrap::after {
      display: none;
    }
  }

  .filter-wrapper {
    margin-bottom: var(--space-6);

    .status-filter-scroll {
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      padding-bottom: var(--space-2);

      &::-webkit-scrollbar {
        display: none;
      }
    }

    .status-filter {
      display: flex;
      flex-wrap: nowrap; // Change to nowrap for horizontal scrolling
      gap: var(--space-3);
      padding: var(--space-1) 0;

      :deep(.status-btn) {
        flex-shrink: 0; // Prevent buttons from shrinking
        color: var(--color-text-regular);
        background: var(--color-bg-surface);
        border: 1px solid var(--color-border);
        font-weight: 600;
        border-radius: 100px; // Pill shape
        padding: 8px 20px;
        transition: all var(--transition-base);

        &.is-active {
          background: var(--color-primary);
          color: white;
          border-color: var(--color-primary);
          box-shadow: 0 4px 12px rgba(var(--color-primary-rgb), 0.3);
        }
      }
    }
  }

  .table-wrapper {
    background: transparent;
    padding: 0;
  }

  :deep(.booking-row-highlight > td) {
    background: rgba(var(--color-primary-rgb), 0.14) !important;
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
    gap: 12px;
  }

  .action-btn-circle {
    transition: all var(--transition-base);

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      filter: brightness(1.1);
    }
  }

  .booking-card {
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
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
      justify-content: flex-end;
      gap: var(--space-3);
      padding-top: var(--space-3);
      border-top: 1px solid var(--color-border-light, #ebeef5);

      .mobile-action-btn-circle {
        margin: 0;
      }
    }
  }

  .booking-card-highlight {
    border-color: rgba(var(--color-primary-rgb), 0.75);
    box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb), 0.2);
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
    background: rgba(var(--color-primary-rgb), 0.1);
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
