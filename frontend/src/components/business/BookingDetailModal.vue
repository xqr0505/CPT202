<template>
  <el-dialog
    v-model="visible"
    title="Booking Details"
    :width="dialogWidth"
    @close="handleClose"
    :destroy-on-close="true"
    class="custom-booking-dialog"
    append-to-body
  >
    <div v-loading="loading" class="booking-detail-modal">
      <template v-if="error">
        <el-empty :description="error"></el-empty>
      </template>
      <template v-else-if="bookingDetail">
        <div class="header-section">
          <el-avatar :size="60" :src="bookingDetail.specialistAvatar">
            {{ bookingDetail.specialistName ? bookingDetail.specialistName.charAt(0) : 'E' }}
          </el-avatar>
          <div class="header-text">
            <span class="specialist-name">{{ bookingDetail.specialistName }}</span>
            <span class="booking-id">Booking ID: {{ bookingDetail.bookingId }}</span>
            <BookingStatusTag :status="bookingDetail.status" />
          </div>
        </div>

        <div class="body-section">
          <div class="info-block">
            <div class="info-title">
              <el-icon><Calendar /></el-icon>
              Appointment Time
            </div>
            <div class="info-content">
              Date: {{ formatDate(bookingDetail.slotDate) }}<br/>
              Time: {{ formatTime(bookingDetail.startTime) }} - {{ formatTime(bookingDetail.endTime) }}
            </div>
          </div>

          <div class="info-block">
            <div class="info-title">
              <el-icon><Money /></el-icon>
              Payment Info
            </div>
            <div class="info-content">
              Total Fee: ${{ formatPrice(bookingDetail.price) }}
            </div>
          </div>

          <div class="info-block">
            <div class="info-title">
              <el-icon><Document /></el-icon>
              Consultation Details
            </div>
            <div class="info-content">
              Topic: {{ bookingDetail.topic }}<br/>
              Notes: {{ bookingDetail.customerNotes || 'No notes provided.' }}
            </div>
          </div>
        </div>
      </template>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Calendar, Money, Document } from '@element-plus/icons-vue';
import { getBookingDetail } from '@/api/booking';
import type { BookingDetail, BookingListItem } from '@/api/booking';
import BookingStatusTag from '@/components/business/BookingStatusTag.vue';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'close'): void;
  (e: 'action', data: { action: string, row: BookingListItem }): void;
}>();

const route = useRoute();
const router = useRouter();

const visible = ref(props.modelValue);
const loading = ref(false);
const error = ref<string | null>(null);
const bookingDetail = ref<BookingDetail | null>(null);
const dialogWidth = ref(window.innerWidth < 768 ? '90%' : '640px');

window.addEventListener('resize', () => {
  dialogWidth.value = window.innerWidth < 768 ? '90%' : '640px';
});

watch(() => props.modelValue, (newVal) => {
  visible.value = newVal;
  if (newVal && route.query.bookingId) {
    fetchDetail(route.query.bookingId as string);
  }
});

watch(visible, (newVal) => {
  emit('update:modelValue', newVal);
});

const fetchDetail = async (id: string) => {
  loading.value = true;
  error.value = null;
  bookingDetail.value = null;
  try {
    const res = await getBookingDetail(id);
    bookingDetail.value = res as unknown as BookingDetail;
  } catch (err: unknown) {
    const errorInfo = err as { message?: string, response?: { data?: { message?: string }, statusText?: string, status?: number } };
    const errorMsg = errorInfo.message || errorInfo.response?.data?.message || errorInfo.response?.statusText;
    if (errorInfo.response?.status === 404 || errorMsg === 'Not Found' || errorMsg === 'No reservation found') {
      error.value = 'No reservation found';
    } else {
      error.value = 'Failed to load booking details';
    }
  } finally {
    loading.value = false;
  }
};

const handleClose = () => {
  visible.value = false;
  emit('close');
  if (route.query.bookingId) {
    const newQuery = { ...route.query };
    delete newQuery.bookingId;
    router.replace({ query: newQuery });
  }
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  const dateObj = new Date(dateStr);
  return new Intl.DateTimeFormat('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric'
  }).format(dateObj);
};

const formatTime = (timeStr: string | undefined) => {
  if (!timeStr) return '';
  const [hour, minute] = timeStr.split(':') || [];
  const d = new Date();
  d.setHours(parseInt(hour || '0', 10));
  d.setMinutes(parseInt(minute || '0', 10));
  return new Intl.DateTimeFormat('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  }).format(d);
};

const formatPrice = (price: number) => {
  return price !== undefined && price !== null ? Number(price).toFixed(2) : '0.00';
};
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.booking-detail-modal {
  padding: var(--space-6);
  background-color: var(color-bg-muted);

  .header-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-5);
    border-radius: var(--radius-xl);
    border: 1px solid var(--color-border);
    margin-bottom: var(--space-6);
    text-align: center;

    .header-text {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--space-2);

      .specialist-name {
        font-size: var(--font-size-xl);
        font-weight: 700;
        color: var(--color-text-primary);
        letter-spacing: -0.01em;
      }

      .booking-id {
        font-size: var(--font-size-sm);
        color: var(--color-text-secondary);
      }
    }
  }

  .body-section {
    display: grid;
    grid-template-columns: 1fr;
    gap: var(--space-4);

    .info-block {
      background-color: var(--color-bg-muted);
      padding: var(--space-5);
      border-radius: var(--radius-xl);
      border: 1px solid var(--color-border);
      transition: all var(--transition-base);

      &:hover {
        background-color: var(--color-bg-page);
        border-color: var(--color-primary-light);
      }

      .info-title {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        font-weight: 700;
        color: var(--color-text-primary);
        margin-bottom: var(--space-4);
        font-size: var(--font-size-md);
        text-transform: uppercase;
        letter-spacing: 0.05em;

        .el-icon {
          color: var(--color-primary);
          font-size: 1.2em;
        }
      }

      .info-content {
        padding-left: 0;
        color: var(--color-text-regular);
        line-height: 1.6;
        font-size: var(--font-size-md);

        strong {
          color: var(--color-text-primary);
        }
      }
    }
  }
}


:deep(.el-divider--horizontal) {
  margin: 0 0 var(--space-6) 0;
  display: none;
}

@media (max-width: 768px) {
  .booking-detail-modal {
    padding: var(--space-4);

    .header-section {
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: var(--space-4);
    }

    .body-section {
      gap: var(--space-3);

      .info-block {
        padding: var(--space-4);
      }
    }
  }
}
</style>
