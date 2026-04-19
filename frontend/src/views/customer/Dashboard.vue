<template>
  <section class="dashboard-page">
    <div class="dashboard-main">
      <!-- Mobile Profile Header -->
      <div class="mobile-profile-header">
        <el-avatar :src="userInfo?.avatar" :size="48">
          {{ userInitial }}
        </el-avatar>
        <div class="welcome-text">
          <span class="greeting">Hello,</span>
          <span class="name">{{ displayName }}</span>
        </div>
      </div>

      <!-- Mobile Upcoming Bookings Swiper -->
      <div class="mobile-upcoming-wrapper">
        <UpcomingAppointments />
      </div>

      <!-- Summary and Charts -->
      <DashboardSummarySection />
    </div>

    <!-- Desktop Right Panel -->
    <aside class="dashboard-side-panel">
      <!-- Desktop Profile Card -->
      <div class="desktop-profile-card">
        <el-avatar :src="userInfo?.avatar" :size="80">
          {{ userInitial }}
        </el-avatar>
        <h3 class="profile-name">{{ displayName }}</h3>
        <p class="profile-email">{{ userInfo?.email }}</p>
      </div>

      <!-- Desktop Mini Calendar -->
      <div class="desktop-mini-calendar">
        <div class="calendar-header">{{ currentMonthName }}</div>
        <div class="calendar-days">
          <div
            v-for="day in weekDays"
            :key="day.date"
            class="calendar-day"
            :class="{ 'is-today': day.isToday }"
          >
            <span class="day-name">{{ day.name }}</span>
            <span class="day-date">{{ day.date }}</span>
          </div>
        </div>
      </div>

      <!-- Desktop Upcoming Bookings -->
      <div class="desktop-upcoming-wrapper">
        <UpcomingAppointments />
      </div>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import UpcomingAppointments from './dashboard/UpcomingAppointments.vue'
import DashboardSummarySection from './dashboard/DashboardSummarySection.vue'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'CustomerDashboard' })

const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)

const displayName = computed(() => {
  const nickname = userInfo.value?.nickname?.trim()
  if (nickname) return nickname
  const username = userInfo.value?.username?.trim()
  return username || 'Guest'
})

const userInitial = computed(() => displayName.value.charAt(0).toUpperCase())

const today = new Date()
const currentMonthName = today.toLocaleDateString('en-US', { month: 'long' })
const weekDays = computed(() => {
  const days = []
  const d = new Date(today)
  const currentDay = d.getDay()
  const diff = d.getDate() - currentDay + (currentDay === 0 ? -6 : 1)
  d.setDate(diff)

  for (let i = 0; i < 7; i++) {
    days.push({
      name: d.toLocaleDateString('en-US', { weekday: 'short' }).slice(0, 2),
      date: d.getDate(),
      isToday: d.toDateString() === today.toDateString()
    })
    d.setDate(d.getDate() + 1)
  }
  return days
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.dashboard-page {
  display: flex;
  gap: var(--space-6);
  padding-right: var(--space-6);
  width: 100%;
  box-sizing: border-box;
  background: var(--color-bg-page);
  min-height: 100vh;
}

.dashboard-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-6);
  gap: var(--space-6);
}

.mobile-profile-header,
.mobile-upcoming-wrapper {
  display: none;
}


.dashboard-side-panel {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  background: var(--color-bg-surface);
  padding: var(--space-6);
  border-radius: var(--radius-xl);
}

.desktop-profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-6);
  background: var(--color-bg-surface);
  border-radius: var(--radius-xl);
  text-align: center;
  border: none;
  box-shadow: none;

  .profile-name {
    margin: var(--space-3) 0 var(--space-1);
    font-size: 20px;
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .profile-email {
    margin: 0;
    font-size: 14px;
    color: var(--color-text-secondary);
  }
}

.desktop-mini-calendar {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4) 0;

  .calendar-header {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .calendar-days {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .calendar-day {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-2);
    border-radius: var(--radius-xl);
    width: 32px;

    .day-name {
      font-size: 12px;
      color: var(--color-text-tertiary);
    }
    .day-date {
      font-size: 14px;
      font-weight: 700;
      color: var(--color-text-primary);
    }

    &.is-today {
      background-color: var(--color-primary);

      .day-name, .day-date {
        color: var(--color-text-inverse);
      }
    }
  }
}

/* Mobile responsive */
@media (max-width: 900px) {
  .dashboard-page {
    flex-direction: column;
    gap: 0;
    padding-right: 0;
    background: var(--color-bg-surface);
  }

  .dashboard-main {
    padding: var(--space-4);
    gap: var(--space-4);
  }

  .dashboard-side-panel {
    display: none;
  }

  .mobile-profile-header {
    display: flex;
    align-items: center;
    gap: var(--space-4);

    .welcome-text {
      display: flex;
      flex-direction: column;

      .greeting {
        font-size: 14px;
        color: var(--color-text-secondary);
      }
      .name {
        font-size: 20px;
        font-weight: 700;
        color: var(--color-text-primary);
      }
    }
  }

  .mobile-upcoming-wrapper {
    display: block;
    margin: 0 calc(var(--space-4) * -1);
  }

  .dashboard-main {
    gap: var(--space-4);
  }
}
</style>
