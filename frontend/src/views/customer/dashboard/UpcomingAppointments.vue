<template>
  <div class="upcoming-appointments">
    <div class="header-section">
      <h2 class="title">Upcoming Appointment</h2>
      <CustomButton
        v-if="hasMore"
        type="primary"
        @click="goToBookings"
        class="view-all-link"
      >
        View All
      </CustomButton>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="filteredAppointments.length > 0" class="appointment-list">
      <div
        v-for="apt in displayedAppointments"
        :key="apt.id"
        class="appointment-card"
        :class="{ 'is-today': isToday(apt.startTime) }"
      >
        <div class="card-left">
          <div class="date-box">
            <span class="month">{{ getMonth(apt.startTime) }}</span>
            <span class="day">{{ getDay(apt.startTime) }}</span>
          </div>
          <div class="info-content">
            <div class="expert-info">
              <span class="expert-name">{{ apt.specialistName }}</span>
              <el-tag
                v-if="isToday(apt.startTime)"
                size="small"
                type="success"
                effect="light"
                class="today-tag"
              >
                Today
              </el-tag>
            </div>
            <div class="service-name">{{ apt.specialistTitle || 'Service' }}</div>
          </div>
        </div>
        <div class="card-right">
          <div class="time-block">
            <el-icon><Clock /></el-icon>
            <span>{{ formatTime(apt.startTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <EmptyPlaceholder
      v-else
      description="You don't have an upcoming appointment."
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Clock } from '@element-plus/icons-vue'
import { getUpcomingBookings } from '@/api/booking'
import type { UpcomingBookingResponse } from '@/api/booking'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'
import CustomButton from '@/components/common/CustomButton.vue'

const router = useRouter()
const loading = ref(true)
const appointments = ref<UpcomingBookingResponse[]>([])

const fetchAppointments = async () => {
  loading.value = true
  try {
    const res = await getUpcomingBookings()
    appointments.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch upcoming appointments:', error)
    appointments.value = []
  } finally {
    loading.value = false
  }
}

const filteredAppointments = computed(() => {
  const now = new Date()
  return appointments.value
    .filter(apt => apt.status === 'Confirmed' && new Date(apt.startTime) > now)
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
})

const displayedAppointments = computed(() => {
  return filteredAppointments.value.slice(0, 3)
})

const hasMore = computed(() => {
  return filteredAppointments.value.length > 3
})

const goToBookings = () => {
  router.push('/customer/bookings')
}

const isToday = (dateString: string) => {
  const date = new Date(dateString)
  const today = new Date()
  return date.getDate() === today.getDate() &&
    date.getMonth() === today.getMonth() &&
    date.getFullYear() === today.getFullYear()
}

const formatTime = (dateString: string) => {
  return new Date(dateString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const getMonth = (dateString: string) => {
  return new Date(dateString).toLocaleDateString([], { month: 'short' })
}

const getDay = (dateString: string) => {
  return new Date(dateString).toLocaleDateString([], { day: '2-digit' })
}

onMounted(() => {
  fetchAppointments()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.upcoming-appointments {
  margin-bottom: var(--space-6);

  .header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-4);

    .title {
      font-size: 20px;
      font-weight: 600;
      color: var(--color-text-primary);
      margin: 0;
    }
  }

  .appointment-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .appointment-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: 8px; /* Standard border-radius is missing variable mapped directly in snapshot, using 8px */
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 12px var(--color-shadow);
      border-color: var(--color-primary-soft);
    }

    &.is-today {
      background-color: var(--color-primary-soft); /* Highlight color */
      color: var(--color-text-inverse); /* Let's assume white */
      border-color: var(--color-primary);

      .date-box {
        background-color: var(--color-bg-surface);
        color: var(--color-primary);
      }

      .expert-name,
      .service-name {
        color: var(--color-bg-surface);
      }

      .time-block {
        color: var(--color-bg-surface);
      }
    }
  }

  .card-left {
    display: flex;
    align-items: center;
    gap: var(--space-4);
  }

  .date-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 60px;
    height: 60px;
    background-color: var(--color-bg-page);
    border-radius: 8px;
    color: var(--color-primary);

    .month {
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
    }
    .day {
      font-size: 20px;
      font-weight: 700;
    }
  }

  .info-content {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);

    .expert-info {
      display: flex;
      align-items: center;
      gap: var(--space-2);
    }

    .expert-name {
      font-size: 16px;
      font-weight: 600;
      color: var(--color-text-primary);
    }

    .service-name {
      font-size: 14px;
      color: var(--color-text-secondary);
    }
  }

  .card-right {
    display: flex;
    align-items: center;

    .time-block {
      display: flex;
      align-items: center;
      gap: var(--space-1);
      font-size: 14px;
      font-weight: 500;
      color: var(--color-text-secondary);
    }
  }
}
</style>
