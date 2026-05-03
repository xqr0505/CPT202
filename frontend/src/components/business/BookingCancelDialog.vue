<template>
  <el-dialog
    v-model="visible"
    title="Cancel Booking"
    width="500px"
    :close-on-click-modal="false"
    append-to-body
    @closed="resetState"
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
        <CustomButton @click="visible = false">Keep Booking</CustomButton>
        <CustomButton
          type="danger"
          :loading="cancelConfirmLoading"
          :disabled="cancelQuoteLoading || !cancelQuote?.allowed || !resolvedBookingId"
          @click="confirmCancel"
        >
          Confirm Cancel
        </CustomButton>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmBookingCancel, getBookingCancelQuote } from '@/api/booking'
import type { BookingCancelQuote, BookingListItem } from '@/api/booking'
import CustomButton from '@/components/common/CustomButton.vue'

const props = defineProps<{
  modelValue: boolean
  booking?: BookingListItem | null
  bookingId?: number | string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', bookingId: string): void
}>()

const visible = ref(props.modelValue)
const cancelQuoteLoading = ref(false)
const cancelConfirmLoading = ref(false)
const cancelQuote = ref<BookingCancelQuote | null>(null)

const resolvedBookingId = computed(() => {
  const raw = props.bookingId ?? props.booking?.id ?? null
  if (raw === null || raw === undefined || raw === '') {
    return ''
  }
  return String(raw).trim()
})

watch(() => props.modelValue, value => {
  visible.value = value
})

watch(visible, value => {
  emit('update:modelValue', value)
})

watch(
  () => [visible.value, resolvedBookingId.value] as const,
  async ([isVisible, bookingId]) => {
    if (!isVisible || !bookingId) {
      return
    }
    await loadQuote(bookingId)
  },
  { immediate: true }
)

const loadQuote = async (bookingId: string) => {
  cancelQuoteLoading.value = true
  try {
    cancelQuote.value = await getBookingCancelQuote(bookingId)
  } catch {
    cancelQuote.value = null
    ElMessage.error('Failed to calculate refund quote. Please try again.')
    visible.value = false
  } finally {
    cancelQuoteLoading.value = false
  }
}

const confirmCancel = async () => {
  if (!resolvedBookingId.value || !cancelQuote.value?.allowed) {
    return
  }
  cancelConfirmLoading.value = true
  try {
    const result = await confirmBookingCancel(resolvedBookingId.value)
    ElMessage.success(result.message || 'Booking cancelled successfully.')
    emit('success', resolvedBookingId.value)
    visible.value = false
  } catch {
    ElMessage.error('Failed to cancel booking. Please try again.')
  } finally {
    cancelConfirmLoading.value = false
  }
}

const resetState = () => {
  cancelQuote.value = null
  cancelQuoteLoading.value = false
  cancelConfirmLoading.value = false
}

const formatMoney = (value?: number | null) => {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '¥0.00'
}
</script>

<style scoped lang="scss">
.cancel-dialog-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
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
</style>
