<template>
  <section class="page-card">
    <p class="page-tag">Customer</p>
    <h1>My Bookings</h1>
    <p class="placeholder-text">This page is under construction. It will display the user's bookings history using the PaginationTable component.</p>
  </section>
</template>

<script setup lang="ts">
defineOptions({ name: 'CustomerBookings' })
</script>

<style scoped>
.page-card { padding: 24px; }
.page-tag { color: var(--color-primary); font-weight: 600; }
.placeholder-text { margin-top: 16px; color: var(--color-text-secondary); }
</style>
