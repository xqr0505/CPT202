<template>
  <section class="page-card">
    <p class="page-tag">Customer</p>
    <h1>My Bookings</h1>

    <div class="statuses-demo">
      <h2>Status Tag</h2>
      <div v-for="s in statuses" :key="s" class="status-row">
        <span class="status-label">{{ s }}</span>
        <BookingStatusTag :status="s" />
      </div>
    </div>

    <div class="empty-demo">
      <h2>Empty State</h2>
      <EmptyPlaceholder description="No reservation records found for this period.">
      </EmptyPlaceholder>
    </div>
  </section>
</template>

<script setup lang="ts">
import { BookingStatus } from '@/constants/booking'
import BookingStatusTag from '@/components/common/BookingStatusTag.vue'
import EmptyPlaceholder from '@/components/common/EmptyPlaceholder.vue'

defineOptions({ name: 'CustomerBookings' })

const statuses = Object.values(BookingStatus)
</script>

<style scoped>
.page-card { padding: 24px; }
.page-tag { color: var(--color-primary); font-weight: 600; }

.statuses-demo { margin-top: 16px; display: flex; flex-direction: column; gap: 12px; }
.empty-demo { margin-top: 40px; }
.status-row { display: flex; align-items: center; gap: 12px; }
.status-label { width: 160px; font-weight: 600; color: var(--color-text-secondary); text-transform: uppercase; }
</style>
