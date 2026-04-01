<template>
  <el-tag :type="tagType" class="booking-status-tag">
    {{ status }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { BOOKING_STATUS } from '@/constants/booking.ts'
import type { BookingStatus } from '@/constants/booking.ts'

interface Props {
  status: BookingStatus | string
}

const props = defineProps<Props>()

const tagType = computed(() => {
  switch (props.status) {
    case BOOKING_STATUS.PENDING:
      return 'warning'
    case BOOKING_STATUS.CONFIRMED:
      return 'success'
    case BOOKING_STATUS.CANCELLED:
      return 'danger'
    case BOOKING_STATUS.COMPLETED:
      return 'info'
    default:
      return 'info'
  }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables';

.booking-status-tag {
  font-weight: 600;
  text-transform: capitalize;
  border-width: var(--border-width-bold);
  border-style: solid;
}
</style>
