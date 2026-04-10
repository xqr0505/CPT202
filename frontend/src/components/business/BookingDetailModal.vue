<template>
  <el-dialog
    v-model="visible"
    title="Booking Details"
    width="640px"
    @close="handleClose"
    :destroy-on-close="true"
    class="custom-booking-dialog"
  >
    <div v-loading="loading" class="booking-detail-modal">
      <template v-if="error">
        <el-empty :description="error"></el-empty>
      </template>
      <template v-else-if="bookingDetail">
        <div class="header-section">
          <div class="specialist-info">
            <el-avatar :size="50" :src="bookingDetail.specialistAvatar">
              {{ bookingDetail.specialistName ? bookingDetail.specialistName.charAt(0) : 'E' }}
            </el-avatar>
            <span class="specialist-name">{{ bookingDetail.specialistName }}</span>
          </div>
          <div class="status-badge">
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

    <template #footer>
      <span class="dialog-footer">
        <CustomButton @click="handleClose">Close</CustomButton>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Calendar, Money, Document } from '@element-plus/icons-vue';
import { getBookingDetail } from '@/api/booking';
import type { BookingDetail } from '@/api/booking';
import BookingStatusTag from '@/components/business/BookingStatusTag.vue';
import CustomButton from '@/components/common/CustomButton.vue';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'close'): void;
}>();

const route = useRoute();
const router = useRouter();

const visible = ref(props.modelValue);
const loading = ref(false);
const error = ref<string | null>(null);
const bookingDetail = ref<BookingDetail | null>(null);

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
  } catch (err: any) {
    const errorMsg = err.message || err.response?.data?.message || err.response?.statusText;
    if (err.response?.status === 404 || errorMsg === 'Not Found' || errorMsg === 'No reservation found') {
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

:deep(.custom-booking-dialog) {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 12px 32px var(--color-shadow);
}

.booking-detail-modal {
  padding: var(--space-6);
  background-color: var(--color-bg-page);

  .header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: var(--color-bg-surface);
    padding: var(--space-5);
    border-radius: var(--radius-md);
    border: 1px solid var(--color-border);
    margin-bottom: var(--space-6);
    box-shadow: 0 4px 12px var(--color-shadow);

    .specialist-info {
      display: flex;
      align-items: center;
      gap: var(--space-4);

      .specialist-name {
        font-size: var(--font-size-xl);
        font-weight: 700;
        color: var(--color-text-primary);
        letter-spacing: -0.01em;
      }
    }
  }

  .body-section {
    display: grid;
    grid-template-columns: 1fr;
    gap: var(--space-4);

    .info-block {
      background-color: var(--color-bg-surface);
      padding: var(--space-5);
      border-radius: var(--radius-md);
      border: 1px solid var(--color-border);
      transition: transform var(--transition-base), box-shadow var(--transition-base);

      &:hover {
        box-shadow: 0 6px 16px var(--color-shadow);
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
  display: none; // Hiding the divider as we use card-based layout
}
</style>
