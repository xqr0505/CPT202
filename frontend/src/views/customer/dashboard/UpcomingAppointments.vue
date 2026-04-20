<template>
  <div class="upcoming-appointments">
    <div class="header-section">
      <div class="title-wrapper">
        <h1 class="main-title">Your Upcoming Appointments</h1>
      </div>
      <router-link to="/customer/bookings" class="view-all-link">
        View All
        <el-icon class="arrow-icon"><ArrowRight /></el-icon>
      </router-link>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else>
      <div v-if="appointments.length > 0" class="appointment-list slider-capable">
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
import { Clock, ArrowRight } from '@element-plus/icons-vue'
import { getUpcomingBookings } from '@/api/booking'
import type { UpcomingBookingResponse } from '@/api/booking'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'

const loading = ref(true)
const appointments = ref<UpcomingBookingResponse[]>([])

const fetchAppointments = async () => {
  loading.value = true
  try {
    const res = await getUpcomingBookings()
    let data: UpcomingBookingResponse[]
    if (Array.isArray(res)) data = res
    else if (res && Array.isArray(res.data)) data = res.data
    else if (res && res.data && Array.isArray(res.data.data)) data = res.data.data
    else data = []
    appointments.value = data
  } catch {
    appointments.value = []
  } finally {
    loading.value = false
  }
}

const displayedAppointments = computed(() => appointments.value.slice(0, 3))


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
    margin-bottom: var(--space-5);

    .title-wrapper {
      display: flex;
      flex-direction: column;
      gap: var(--space-1);

      .main-title {
        font-size: 20px;
        font-weight: 700;
        color: var(--color-text-primary);
        margin: 0;
      }
    }

    .view-all-link {
      display: inline-flex;
      align-items: center;
      gap: var(--space-1);
      font-size: 14px;
      font-weight: 600;
      color: var(--color-primary);
      text-decoration: none;
      padding: var(--space-1) var(--space-3);
      border-radius: var(--radius-full);
      background-color: transparent;
      transition: all 0.3s ease;

      .arrow-icon {
        font-size: 14px;
        transition: transform 0.3s ease;
      }

      &:hover {
        background-color: var(--color-primary-soft);

        .arrow-icon {
          transform: translateX(4px);
        }
      }
    }
  }

  .appointment-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  /* Desktop formatting: vertical stacked */
  .appointment-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-4);
    background-color: var(--color-primary-soft);
    color: var(--color-text-primary);
    border: none;
    border-radius: var(--radius-xl);
    box-shadow: none;
    transition: all 0.3s;
    flex-shrink: 0;

    &:hover {
      box-shadow: none;
      filter: brightness(0.95);
    }

    &.is-today {
      background-color: var(--color-primary);
      color: var(--color-text-inverse);
      border-color: transparent;

      .date-box {
        background-color: var(--color-bg-surface);
        color: var(--color-primary);
      }

      .expert-name,
      .service-name {
        color: var(--color-text-inverse);
      }

      .time-block {
        color: var(--color-text-inverse);
      }
    }
  }

  .card-left {
    display: flex;
    align-items: center;
    gap: var(--space-4);
    min-width: 0;
  }

  .date-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 60px;
    height: 60px;
    flex-shrink: 0;
    background-color: var(--color-bg-surface);
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
    min-width: 0;

    .expert-info {
      display: flex;
      align-items: center;
      gap: var(--space-2);
    }

    .expert-name {
      font-size: 13px;
      font-weight: 600;
      color: var(--color-text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

    }

    .service-name {
      font-size: 12px;
      color: var(--color-text-secondary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .card-right {
    flex-shrink: 0;
  }

  /* Mobile Swiper style overrides */
  @media (max-width: 900px) {
    .header-section {
      padding: 0 var(--space-4);
    }

    .slider-capable {
      flex-direction: row;
      overflow-x: auto;
      scroll-snap-type: x mandatory;
      padding: var(--space-2) var(--space-4);
      gap: var(--space-4);

      -ms-overflow-style: none; /* IE and Edge */
      scrollbar-width: none; /* Firefox */
      &::-webkit-scrollbar {
        display: none;
      }
    }

    .appointment-card {
      width: 85%;
      min-width: 250px;
      max-width: 320px;
      scroll-snap-align: center;
      flex-direction: column;
      align-items: flex-start;
      gap: var(--space-4);

      .card-right {
        align-self: flex-end;
      }
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
