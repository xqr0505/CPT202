<template>
  <section class="upcoming-appointments">
    <div class="header">
      <h2 class="title">Upcoming Appointment</h2>
      <el-link
        v-if="hasMoreAppointments"
        type="primary"
        class="view-all-link"
        @click="goToMyBookings"
      >
        View All
      </el-link>
    </div>

    <div v-if="loading" v-loading="loading" class="loading-area"></div>

    <template v-else>
      <div v-if="displayList.length > 0" class="appointment-list">
        <el-card
          v-for="appointment in displayList"
          :key="appointment.id"
          class="appointment-card"
          shadow="hover"
          :class="{ 'is-today': isToday(appointment.startTime) }"
        >
          <div class="card-header">
            <div class="expert-info">
              <span class="expert-name">{{ appointment.specialistName }}</span>
              <el-tag v-if="isToday(appointment.startTime)" type="warning" size="small" effect="dark" class="today-tag">
                Today
              </el-tag>
            </div>
            <!-- BookingStatusTag component not available yet, using text instead -->
            <span class="status-text">{{ appointment.status }}</span>
          </div>

          <div class="card-body">
            <div class="info-row">
              <el-icon><Service /></el-icon>
              <span class="service-name">{{ appointment.specialistTitle }}</span>
            </div>
            <div class="info-row time-info">
              <el-icon><Calendar /></el-icon>
              <span class="date">{{ formatDate(appointment.startTime) }}</span>
              <el-icon class="time-icon"><Clock /></el-icon>
              <span class="time">{{ formatTime(appointment.startTime) }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <empty-placeholder
        v-else
        description="You don't have an upcoming appointment."
        :image-size="120"
      />
    </template>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Clock, Service } from '@element-plus/icons-vue'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'
import { BOOKING_STATUS } from '@/constants/booking'
import type { UpcomingBookingResponse } from '@/api/booking'
import { getUpcomingBookings } from '@/api/booking'

const router = useRouter()
const appointments = ref<UpcomingBookingResponse[]>([])
const loading = ref<boolean>(false)

const DISPLAY_LIMIT = 3

const validAppointments = computed(() => {
  const now = new Date()

  return appointments.value
    .filter(app => app.status === BOOKING_STATUS.CONFIRMED)
    .filter(app => {
      const appointmentTime = new Date(app.startTime)
      return appointmentTime > now
    })
    .sort((a, b) => {
      const timeA = new Date(a.startTime)
      const timeB = new Date(b.startTime)
      return timeA.getTime() - timeB.getTime()
    })
})

const displayList = computed(() => {
  return validAppointments.value.slice(0, DISPLAY_LIMIT)
})

const hasMoreAppointments = computed(() => {
  return validAppointments.value.length > DISPLAY_LIMIT
})

const isToday = (dateTimeStr: string): boolean => {
  const appointmentDate = new Date(dateTimeStr)
  const today = new Date()
  return appointmentDate.toDateString() === today.toDateString()
}

const goToMyBookings = (): void => {
  // TODO: Implement navigation to customer bookings
  router.push({ name: 'CustomerBookings' })
}

const fetchAppointments = async () => {
  loading.value = true
  try {
    const res = await getUpcomingBookings()
    appointments.value = res
  } catch (error) {
    console.error('Failed to fetch upcoming appointments:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateTimeStr: string): string => {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'short', day: 'numeric' }
  return new Intl.DateTimeFormat('en-US', options).format(new Date(dateTimeStr))
}

const formatTime = (dateTimeStr: string): string => {
  const options: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit', hour12: false }
  return new Intl.DateTimeFormat('en-US', options).format(new Date(dateTimeStr))
}

onMounted(() => {
  fetchAppointments()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables';

.upcoming-appointments {
  background-color: var(--color-bg-surface, #ffffff);
  border-radius: var(--radius-lg, 8px);
  padding: var(--space-16, 16px);
  box-shadow: 0 2px 8px var(--color-shadow, rgba(0, 0, 0, 0.05));

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-16, 16px);

    .title {
      font-size: var(--font-size-lg, 18px);
      font-weight: 600;
      color: var(--color-text-primary, #303133);
      margin: 0;
    }

    .view-all-link {
      font-size: var(--font-size-sm, 14px);
    }
  }

  .loading-area {
    min-height: 200px;
  }

  .appointment-list {
    display: flex;
    flex-direction: column;
    gap: var(--space-12, 12px);

    .appointment-card {
      border-radius: var(--radius-md, 6px);
      border: 1px solid var(--color-border, #dcdfe6);
      transition: all var(--transition-base, 0.3s);

      &.is-today {
        background-color: var(--color-warning-light, #fdf6ec);
        border-color: var(--color-warning, #e6a23c);
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid var(--color-border-light, #ebeef5);
        padding-bottom: var(--space-8, 8px);
        margin-bottom: var(--space-8, 8px);

        .expert-info {
          display: flex;
          align-items: center;
          gap: var(--space-8, 8px);

          .expert-name {
            font-size: var(--font-size-md, 16px);
            font-weight: 600;
            color: var(--color-text-primary, #303133);
          }

          .today-tag {
            font-weight: bold;
          }
        }

        .status-text {
          font-size: var(--font-size-sm, 14px);
          color: var(--color-text-regular, #606266);
        }
      }

      .card-body {
        display: flex;
        flex-direction: column;
        gap: var(--space-8, 8px);

        .info-row {
          display: flex;
          align-items: center;
          gap: var(--space-8, 8px);
          font-size: var(--font-size-sm, 14px);
          color: var(--color-text-regular, #606266);

          .service-name {
            font-weight: 500;
          }

          .time-icon {
            margin-left: var(--space-8, 8px);
          }
        }
      }
    }
  }
}
</style>

