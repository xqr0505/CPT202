<template>
  <div class="upcoming-appointments">
    <div class="header-section">
      <div class="title-wrapper">
        <h1 class="main-title">Your Upcoming Appointments</h1>
        <p class="subtitle">Manage and track your scheduled consultations effortlessly</p>
      </div>
      <ViewAllLink class="view-all-link" />
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else>
      <div v-if="appointments.length > 0" class="appointment-list">
        <div
          v-for="apt in displayedAppointments"
          :key="apt.id"
          class="appointment-card"
          :class="{ 'is-today': apt.today }"
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
                  v-if="apt.today"
                  size="small"
                  type="success"
                  effect="light"
                  class="today-tag"
                >
                  Today
                </el-tag>
              </div>
              <div class="service-name">{{ apt.serviceName }}</div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Clock } from '@element-plus/icons-vue'
import { getUpcomingBookings } from '@/api/booking'
import type { UpcomingBookingResponse } from '@/api/booking'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'
import ViewAllLink from '@/components/common/ViewAllLink.vue'

const router = useRouter()
const loading = ref(true)
const appointments = ref<UpcomingBookingResponse[]>([])

const fetchAppointments = async () => {
  loading.value = true
  try {
    const res = await getUpcomingBookings()
    let data: any[]
    if (Array.isArray(res)) data = res
    else if (res && Array.isArray((res as any).data)) data = (res as any).data
    else if (res && (res as any).data && Array.isArray((res as any).data.data)) data = (res as any).data.data
    else data = []
    appointments.value = data
  } catch (error) {
    appointments.value = []
  } finally {
    loading.value = false
  }
}

const displayedAppointments = computed(() => appointments.value.slice(0, 3))
const hasMore = computed(() => appointments.value.length > 3)


const toDate = (dateString?: string) => {
  if (!dateString) return new Date('')
  let s = dateString.trim()
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(s)) {
    s = s.replace(' ', 'T')
  }
  return new Date(s)
}


const formatTime = (dateString: string) => {
  const d = toDate(dateString)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const getMonth = (dateString: string) => {
  const d = toDate(dateString)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleDateString('en-US', { month: 'short' })
}

const getDay = (dateString: string) => {
  const d = toDate(dateString)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleDateString('en-US', { day: '2-digit' })
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

    .title-wrapper {
      display: flex;
      flex-direction: column;
      gap: var(--space-1);

      .main-title {
        font-size: 24px;
        font-weight: 700;
        color: var(--color-text-primary);
        margin: 0;
      }

      .subtitle {
        font-size: 14px;
        color: var(--color-text-secondary);
        margin: 0;
      }
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
    color: var(--color-text-primary);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 12px var(--color-shadow);
      border-color: var(--color-primary-soft);
    }

    &.is-today {
      background-color: var(--color-primary-soft);
      color: var(--color-text-inverse);
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
    border-radius: var(--radius-md);
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

  .footer-section {
    margin-top: var(--space-4);
    text-align: center;

    .view-all-link {
      font-size: 14px;
      color: var(--color-primary);
      text-decoration: underline;
      cursor: pointer;

      &:hover {
        color: var(--color-primary-dark);
      }
    }
  }
}
</style>
