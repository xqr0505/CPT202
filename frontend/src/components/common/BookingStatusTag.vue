<template>
  <el-tag :type="tagType" class="booking-status-tag">
    {{ status }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { BookingStatus } from '@/constants/booking'

interface Props {
  status: BookingStatus | string
}

const props = defineProps<Props>()

const tagType = computed(() => {
  switch (props.status) {
    case BookingStatus.PENDING:
      return 'warning'
    case BookingStatus.CONFIRMED:
      return 'success'
    case BookingStatus.CANCELLED:
      return 'danger'
    case BookingStatus.COMPLETED:
      return 'info'
    default:
      return 'info'
  }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss';

.booking-status-tag {
  font-weight: 600;
  text-transform: capitalize;
  border-width: var(--border-width-bold);
  border-style: solid;
}
</style>
