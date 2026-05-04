<template>
  <el-dialog
    v-model="visible"
    title="Reschedule Booking"
    width="560px"
    :close-on-click-modal="false"
    append-to-body
    @closed="resetState"
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
        <CustomButton @click="visible = false">Cancel</CustomButton>
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
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmBookingReschedule, getBookingRescheduleQuote } from '@/api/booking'
import type { BookingListItem, BookingRescheduleQuote } from '@/api/booking'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import { fetchSpecialistAvailability } from '@/api/specialist'
import type { SpecialistAvailabilitySlot } from '@/types/specialist'

const props = defineProps<{
  modelValue: boolean
  booking?: BookingListItem | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', bookingId: string): void
}>()

const visible = ref(props.modelValue)
const rescheduleAvailabilityLoading = ref(false)
const rescheduleQuoteLoading = ref(false)
const rescheduleConfirmLoading = ref(false)
const rescheduleDate = ref('')
const rescheduleAvailability = ref<SpecialistAvailabilitySlot[]>([])
const selectedRescheduleSlotId = ref<number | null>(null)
const rescheduleQuote = ref<BookingRescheduleQuote | null>(null)

const resolvedBookingId = computed(() => {
  const raw = props.booking?.id
  if (!raw) {
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
  () => [visible.value, props.booking?.id] as const,
  async ([isVisible, bookingId]) => {
    if (!isVisible || !bookingId || !props.booking) {
      return
    }
    rescheduleDate.value = toLocalDate(props.booking.appointmentDateTime)
    await loadRescheduleAvailability()
  },
  { immediate: true }
)

const loadRescheduleAvailability = async () => {
  if (!props.booking || !rescheduleDate.value) {
    rescheduleAvailability.value = []
    return
  }
  rescheduleAvailabilityLoading.value = true
  try {
    const slots = await fetchSpecialistAvailability(Number(props.booking.specialistId), rescheduleDate.value)
    rescheduleAvailability.value = (slots || []).filter(
      slot => slot.status === 'AVAILABLE' && slotStartsAfterTwoHours(slot),
    )
    if (!rescheduleAvailability.value.some(slot => slot.id === selectedRescheduleSlotId.value)) {
      selectedRescheduleSlotId.value = null
      rescheduleQuote.value = null
    }
  } catch {
    rescheduleAvailability.value = []
    ElMessage.error('Failed to load available slots.')
  } finally {
    rescheduleAvailabilityLoading.value = false
  }
}

const handleRescheduleSlotSelect = async (slotId: number) => {
  if (!resolvedBookingId.value) {
    return
  }
  selectedRescheduleSlotId.value = slotId
  rescheduleQuoteLoading.value = true
  try {
    rescheduleQuote.value = await getBookingRescheduleQuote(resolvedBookingId.value, slotId)
  } catch {
    rescheduleQuote.value = null
    ElMessage.error('Failed to calculate reschedule quote. Please try again.')
  } finally {
    rescheduleQuoteLoading.value = false
  }
}

const confirmReschedule = async () => {
  if (!resolvedBookingId.value || !selectedRescheduleSlotId.value || !rescheduleQuote.value?.allowed) {
    return
  }
  rescheduleConfirmLoading.value = true
  try {
    const result = await confirmBookingReschedule(resolvedBookingId.value, selectedRescheduleSlotId.value)
    ElMessage.success(result.message || 'Booking rescheduled successfully.')
    emit('success', resolvedBookingId.value)
    visible.value = false
  } catch {
    ElMessage.error('Failed to reschedule booking. Please try again.')
  } finally {
    rescheduleConfirmLoading.value = false
  }
}

const resetState = () => {
  rescheduleDate.value = ''
  rescheduleAvailability.value = []
  selectedRescheduleSlotId.value = null
  rescheduleQuote.value = null
  rescheduleAvailabilityLoading.value = false
  rescheduleQuoteLoading.value = false
  rescheduleConfirmLoading.value = false
}

const toLocalDate = (dtStr: string) => {
  if (!dtStr) return ''
  const cleanDtStr = dtStr.replace(' ', 'T')
  const dateObj = new Date(cleanDtStr)
  if (isNaN(dateObj.getTime())) return ''
  const year = dateObj.getFullYear()
  const month = `${dateObj.getMonth() + 1}`.padStart(2, '0')
  const day = `${dateObj.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatMoney = (value?: number | null) => {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '¥0.00'
}

const formatSlotTime = (time?: string) => {
  if (!time) return ''
  const parts = String(time).split(':')
  const [hour, minute] = parts
  if (hour && minute) {
    return `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`
  }
  return String(time)
}

const slotStartsAfterTwoHours = (slot: SpecialistAvailabilitySlot) => {
  const datePart = String(slot.slotDate || '').trim()
  const timePart = String(slot.startTime || '').trim()
  if (!datePart || !timePart) {
    return false
  }
  const iso = `${datePart}T${timePart.length === 5 ? `${timePart}:00` : timePart}`
  const slotTime = new Date(iso).getTime()
  if (Number.isNaN(slotTime)) {
    return false
  }
  return slotTime - Date.now() > 2 * 60 * 60 * 1000
}
</script>

<style scoped lang="scss">
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
</style>
