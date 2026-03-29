<template>
  <section class="page-card">
    <p class="page-tag">Customer</p>
    <h1>My Bookings (demo)</h1>
    <p>This page demonstrates the BookingStatusTag component with several example statuses.</p>

    <div class="statuses-demo">
      <div v-for="s in statuses" :key="s" class="status-row">
        <span class="status-label">{{ s }}</span>
        <BookingStatusTag :status="s" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { BookingStatus } from '@/constants/booking'
import BookingStatusTag from '@/components/common/BookingStatusTag.vue'

defineOptions({ name: 'CustomerBookings' })

const statuses = Object.values(BookingStatus)
</script>

<style scoped>
.page-card { padding: 24px; }
.page-tag { color: var(--color-primary); font-weight: 600; }

.statuses-demo { margin-top: 16px; display: flex; flex-direction: column; gap: 12px; }
.status-row { display: flex; align-items: center; gap: 12px; }
.status-label { width: 160px; font-weight: 600; color: var(--color-text-secondary); text-transform: uppercase; }
</style>
