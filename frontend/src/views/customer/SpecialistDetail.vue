<template>
  <section class="detail-page">
    <div class="detail-actions">
      <CustomButton @click="goBack">Back to results</CustomButton>
    </div>

    <section v-if="loading" class="detail-loading card">
      <el-skeleton animated :rows="8" />
    </section>

    <template v-else-if="specialist">
      <section class="overview card">
        <div class="overview-main">
          <el-avatar :src="specialist.avatarUrl" :size="88">
            {{ specialist.name.charAt(0).toUpperCase() }}
          </el-avatar>

          <div class="identity">
            <p class="eyebrow">Specialist detail</p>
            <h1>{{ specialist.name }}</h1>
            <div class="tag-row">
              <el-tag effect="plain">{{ specialist.categoryName || 'Unassigned category' }}</el-tag>
              <el-tag>{{ formatLevel(specialist.level) }}</el-tag>
              <el-tag :type="specialist.status === 'ACTIVE' ? 'success' : 'info'">
                {{ formatStatus(specialist.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <aside class="price-panel">
          <span class="panel-label">Consultation fee</span>
          <strong class="panel-price">&#165;{{ Number(specialist.consultationFee || 0).toFixed(2) }}</strong>
        </aside>
      </section>

      <section class="detail-grid">
        <article class="card info-card">
          <h2>Profile</h2>
          <p class="bio">{{ specialist.bio || 'No profile introduction has been provided yet.' }}</p>

          <dl class="info-list">
            <div>
              <dt>Email</dt>
              <dd>{{ specialist.email || 'Not disclosed' }}</dd>
            </div>
            <div>
              <dt>Phone</dt>
              <dd>{{ specialist.phoneNumber || 'Not disclosed' }}</dd>
            </div>
          </dl>
        </article>

        <article class="card availability-card">
          <div class="availability-head">
            <div>
              <h2>Availability</h2>
              <p>Choose a date to view bookable time slots.</p>
            </div>

            <el-date-picker
              v-model="selectedDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="Select date"
            />
          </div>

          <div v-if="availabilityLoading" class="availability-loading">
            <el-skeleton animated :rows="4" />
          </div>

          <EmptyPlaceholder
            v-else-if="!availability.length"
            description="No available slots for the selected date."
          />

          <div v-else class="slot-grid">
            <button
              v-for="slot in availability"
              :key="slot.id"
              type="button"
              class="slot-chip"
              :class="{ active: bookingForm.slotId === slot.id }"
              @click="selectSlot(slot)"
            >
              <span>{{ slot.startTime }}</span>
              <small>{{ slot.endTime }}</small>
            </button>
          </div>
        </article>

        <article class="card booking-card">
          <div class="booking-head">
            <div>
              <h2>Book This Specialist</h2>
              <p>Select a time slot, then fill in a short topic and note.</p>
            </div>
            <el-tag v-if="selectedSlot" type="success" effect="plain">
              {{ selectedSlot.startTime }} - {{ selectedSlot.endTime }}
            </el-tag>
          </div>

          <el-form label-position="top" class="booking-form">
            <el-form-item label="Topic" required>
              <el-select
                v-model="bookingForm.topic"
                placeholder="Select a topic"
                class="topic-select"
              >
                <el-option
                  v-for="topic in specialistTopics"
                  :key="topic"
                  :label="topic"
                  :value="topic"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="Notes">
              <el-input
                v-model="bookingForm.customerNotes"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="Optional details you want the specialist to know"
              />
            </el-form-item>
          </el-form>

          <div class="booking-actions">
            <span class="booking-tip">
              {{ selectedSlot ? `Selected ${selectedDate} ${selectedSlot.startTime}` : 'Please choose an available time slot first.' }}
            </span>
            <CustomButton :loading="bookingSubmitting" @click="submitBooking">
              Confirm booking
            </CustomButton>
          </div>
        </article>
      </section>
    </template>

    <EmptyPlaceholder
      v-else
      description="The requested specialist could not be found."
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createBooking, getBookingTopics } from '@/api/booking'
import { fetchSpecialistAvailability, fetchSpecialistDetail } from '@/api/specialist'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import type { SpecialistAvailabilitySlot, SpecialistDetail } from '@/types/specialist'

defineOptions({ name: 'SpecialistDetail' })

const route = useRoute()
const router = useRouter()

const specialist = ref<SpecialistDetail | null>(null)
const availability = ref<SpecialistAvailabilitySlot[]>([])
const loading = ref(false)
const availabilityLoading = ref(false)
const bookingSubmitting = ref(false)
const bookingTopics = ref<string[]>([])

const bookingForm = ref({
  slotId: null as number | null,
  topic: '',
  customerNotes: '',
})

const toLocalDateString = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = `${now.getMonth() + 1}`.padStart(2, '0')
  const day = `${now.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const selectedDate = ref(
  typeof route.query.date === 'string' && route.query.date ? route.query.date : toLocalDateString(),
)

const specialistId = computed(() => Number(route.params.id))
const selectedSlot = computed(
  () => availability.value.find((slot) => slot.id === bookingForm.value.slotId) ?? null,
)
const specialistTopics = computed(() => bookingTopics.value)

const loadDetail = async () => {
  if (!Number.isInteger(specialistId.value) || specialistId.value <= 0) {
    specialist.value = null
    return
  }

  loading.value = true
  try {
    specialist.value = await fetchSpecialistDetail(specialistId.value)
  } finally {
    loading.value = false
  }
}

const loadAvailability = async () => {
  if (!specialist.value || !selectedDate.value) {
    availability.value = []
    return
  }

  availabilityLoading.value = true
  try {
    availability.value = await fetchSpecialistAvailability(specialist.value.id, selectedDate.value)
    if (!availability.value.some((slot) => slot.id === bookingForm.value.slotId)) {
      bookingForm.value.slotId = null
    }
  } finally {
    availabilityLoading.value = false
  }
}

const loadBookingTopics = async () => {
  bookingTopics.value = await getBookingTopics()
}

const selectSlot = (slot: SpecialistAvailabilitySlot) => {
  bookingForm.value.slotId = slot.id
}

const resetBookingForm = () => {
  bookingForm.value.slotId = null
  bookingForm.value.topic = ''
  bookingForm.value.customerNotes = ''
}

const submitBooking = async () => {
  if (!specialist.value) {
    return
  }
  if (!bookingForm.value.slotId) {
    ElMessage.warning('Please choose a time slot first.')
    return
  }
  if (!bookingForm.value.topic.trim()) {
    ElMessage.warning('Please choose a booking topic.')
    return
  }

  bookingSubmitting.value = true
  try {
    await createBooking({
      specialistId: specialist.value.id,
      slotId: bookingForm.value.slotId,
      topic: bookingForm.value.topic.trim(),
      customerNotes: bookingForm.value.customerNotes.trim(),
    })
    ElMessage.success('Booking created successfully.')
    resetBookingForm()
    await loadAvailability()
    await router.push('/customer/bookings')
  } finally {
    bookingSubmitting.value = false
  }
}

const goBack = () => {
  const from = typeof route.query.from === 'string' ? route.query.from : '/customer/specialists'
  router.push(from)
}

const formatLevel = (level: string) => level.charAt(0).toUpperCase() + level.slice(1).toLowerCase()
const formatStatus = (status: string) =>
  status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()

onMounted(async () => {
  await loadBookingTopics()
})

watch(
  () => [route.params.id, route.query.date],
  async () => {
    selectedDate.value =
      typeof route.query.date === 'string' && route.query.date ? route.query.date : toLocalDateString()
    bookingForm.value.topic = ''
    await loadDetail()
    await loadAvailability()
  },
  { immediate: true },
)

watch(selectedDate, async () => {
  if (specialist.value) {
    await loadAvailability()
  }
})
</script>

<style scoped lang="scss">
.detail-page {
  display: grid;
  gap: var(--space-6);
}

.detail-actions {
  display: flex;
  justify-content: flex-start;
}

.overview,
.detail-loading {
  padding: var(--space-8);
}

.overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
}

.overview-main {
  display: flex;
  align-items: center;
  gap: var(--space-5);
}

.identity {
  display: grid;
  gap: var(--space-3);
}

.identity h1,
.info-card h2,
.availability-card h2 {
  margin: 0;
}

.eyebrow {
  margin: 0;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.price-panel {
  min-width: 220px;
  padding: var(--space-5);
  border-radius: var(--radius-lg);
  background: linear-gradient(145deg, rgba(51, 144, 251, 0.12), rgba(51, 144, 251, 0.02));
  border: 1px solid var(--color-border);
  display: grid;
  gap: var(--space-2);
}

.panel-label {
  color: var(--color-text-secondary);
  text-transform: uppercase;
  font-size: 12px;
  letter-spacing: 0.08em;
}

.panel-price {
  font-size: 32px;
  color: var(--color-primary);
}

.detail-grid {
  display: grid;
  gap: var(--space-4);
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
}

.info-card,
.availability-card {
  padding: var(--space-6);
}

.booking-card {
  padding: var(--space-6);
}

.bio {
  margin: var(--space-4) 0 var(--space-5);
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.info-list {
  margin: 0;
  display: grid;
  gap: var(--space-4);
}

.info-list dt {
  margin-bottom: var(--space-1);
  color: var(--color-text-tertiary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.info-list dd {
  margin: 0;
  color: var(--color-text-primary);
  font-weight: 600;
}

.availability-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  align-items: flex-start;
  margin-bottom: var(--space-5);
}

.availability-head p {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
}

.slot-grid {
  display: grid;
  gap: var(--space-3);
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
}

.slot-chip {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
  display: grid;
  gap: var(--space-1);
  justify-items: center;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

.slot-chip.active {
  border-color: var(--color-primary);
  background: rgba(51, 144, 251, 0.1);
  transform: translateY(-1px);
}

.slot-chip span {
  color: var(--color-text-primary);
  font-weight: 700;
}

.slot-chip small {
  color: var(--color-text-secondary);
}

.booking-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  align-items: flex-start;
  margin-bottom: var(--space-5);
}

.booking-head h2 {
  margin: 0;
}

.booking-head p {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
}

.booking-form {
  margin-bottom: var(--space-4);
}

.topic-select {
  width: 100%;
}

.booking-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-4);
}

.booking-tip {
  color: var(--color-text-secondary);
  font-size: 14px;
}

@media (max-width: 960px) {
  .overview,
  .availability-head,
  .booking-head,
  .booking-actions,
  .detail-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .overview {
    align-items: flex-start;
  }

  .price-panel {
    width: 100%;
  }
}
</style>
