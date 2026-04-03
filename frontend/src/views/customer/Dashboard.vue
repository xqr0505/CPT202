<template>
  <section class="page-card">
    <h1>Dashboard</h1>
    <el-card>
      <UpcomingAppointments />
    </el-card>
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
