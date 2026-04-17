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
          <p class="panel-copy">Review the profile here, then continue to the separate booking page when you're ready.</p>
          <CustomButton type="primary" class="panel-action" @click="goToBooking">
            Book now
          </CustomButton>
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
      </section>
    </template>

    <EmptyPlaceholder
      v-else
      description="The requested specialist could not be found."
    />
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchSpecialistDetail } from '@/api/specialist'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'
import CustomButton from '@/components/common/CustomButton.vue'
import type { SpecialistDetail } from '@/types/specialist'

defineOptions({ name: 'SpecialistDetail' })

const route = useRoute()
const router = useRouter()

const specialist = ref<SpecialistDetail | null>(null)
const loading = ref(false)

const specialistId = computed(() => Number(route.params.id))

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

const goBack = () => {
  const from = typeof route.query.from === 'string' ? route.query.from : '/customer/specialists'
  router.push(from)
}

const goToBooking = () => {
  if (!specialist.value) {
    return
  }

  router.push({
    name: 'CustomerSpecialistBooking',
    params: { id: specialist.value.id },
    query: {
      from: route.fullPath,
      ...(typeof route.query.date === 'string' && route.query.date ? { date: route.query.date } : {}),
    },
  })
}

const formatLevel = (level: string) => level.charAt(0).toUpperCase() + level.slice(1).toLowerCase()
const formatStatus = (status: string) =>
  status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()

watch(
  () => route.params.id,
  async () => {
    await loadDetail()
  },
  { immediate: true },
)
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
.detail-loading,
.info-card {
  padding: var(--space-6);
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
.info-card h2 {
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
  min-width: 260px;
  padding: var(--space-5);
  border-radius: var(--radius-lg);
  background: linear-gradient(
    145deg,
    rgba(var(--color-primary-rgb), 0.12),
    rgba(var(--color-primary-rgb), 0.02)
  );
  border: 1px solid var(--color-border);
  display: grid;
  gap: var(--space-3);
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

.panel-copy {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.panel-action {
  width: 100%;
}

.detail-grid {
  display: grid;
  gap: var(--space-4);
  grid-template-columns: minmax(0, 1fr);
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

@media (max-width: 960px) {
  .overview {
    flex-direction: column;
    align-items: flex-start;
  }

  .price-panel {
    width: 100%;
  }

  .info-list {
    grid-template-columns: 1fr;
  }
}
</style>
