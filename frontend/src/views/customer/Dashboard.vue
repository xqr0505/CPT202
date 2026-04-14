<template>
  <section class="page-card">
    <h1 class="page-title">{{ DASHBOARD_PAGE_TITLE }}</h1>
    <el-card class="dashboard-card">
      <UpcomingAppointments />
      <DashboardSummarySection />
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { DASHBOARD_PAGE_TITLE } from '@/constants/dashboard'
import UpcomingAppointments from './dashboard/UpcomingAppointments.vue'
import DashboardSummarySection from './dashboard/DashboardSummarySection.vue'

defineOptions({ name: 'CustomerDashboard' })
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.page-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-6);
}

.page-title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: calc(var(--space-6) + var(--space-2));
  font-weight: 700;
}

.dashboard-card {
  border-radius: var(--radius-lg);
}
</style>
