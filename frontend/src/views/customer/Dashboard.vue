<template>
  <section class="page-card">
    <p class="page-tag">Customer</p>
    <h1>Dashboard</h1>
    <UpcomingAppointments />
  </section>
</template>

<script setup lang="ts">
import UpcomingAppointments from './dashboard/UpcomingAppointments.vue'

defineOptions({ name: 'CustomerDashboard' })
</script>

<style scoped>
.page-card { padding: 24px; }
.page-tag { color: var(--color-primary); font-weight: 600; }
</style>
