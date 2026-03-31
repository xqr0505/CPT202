<template>
  <article class="specialist-card card">
    <div class="card-top">
      <el-avatar :src="specialist.avatarUrl" :size="68">
        {{ specialist.name.charAt(0).toUpperCase() }}
      </el-avatar>

      <div class="identity">
        <div class="identity-top">
          <h3>{{ specialist.name }}</h3>
          <el-tag :type="statusTagType" effect="light">{{ statusLabel }}</el-tag>
        </div>
        <p class="category">{{ specialist.categoryName || 'Unassigned category' }}</p>
        <div class="meta-tags">
          <el-tag effect="plain">{{ levelLabel }}</el-tag>
          <el-tag v-if="selectedDate" :type="specialist.hasAvailabilityOnSelectedDate ? 'success' : 'info'">
            {{ specialist.hasAvailabilityOnSelectedDate ? 'Available on selected date' : 'No slots on selected date' }}
          </el-tag>
        </div>
      </div>
    </div>

    <p class="bio">
      {{ specialist.bio || 'No profile introduction has been provided yet.' }}
    </p>

    <div class="card-bottom">
      <div class="price-block">
        <span class="price-label">Consultation fee</span>
        <strong class="price">¥{{ formatFee(specialist.consultationFee) }}</strong>
      </div>

      <CustomButton type="primary" @click="$emit('view', specialist.id)">View details</CustomButton>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import CustomButton from '@/components/common/CustomButton.vue'
import type { SpecialistSummary } from '@/types/specialist'

const props = defineProps<{
  specialist: SpecialistSummary
  selectedDate?: string
}>()

defineEmits<{
  view: [id: number]
}>()

const levelLabel = computed(() => {
  const level = props.specialist.level || ''
  return level.charAt(0).toUpperCase() + level.slice(1).toLowerCase()
})

const statusLabel = computed(() => {
  const status = props.specialist.status || 'UNKNOWN'
  return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()
})

const statusTagType = computed(() => {
  return props.specialist.status === 'ACTIVE' ? 'success' : 'info'
})

const formatFee = (value: number) => Number(value || 0).toFixed(2)
</script>

<style scoped lang="scss">
.specialist-card {
  height: 100%;
  padding: var(--space-6);
  display: grid;
  gap: var(--space-5);
}

.card-top {
  display: flex;
  gap: var(--space-4);
  align-items: flex-start;
}

.identity {
  min-width: 0;
  display: grid;
  gap: var(--space-2);
}

.identity-top {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  justify-content: space-between;
}

.identity-top h3 {
  margin: 0;
  font-size: 20px;
  color: var(--color-text-primary);
}

.category {
  margin: 0;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.meta-tags {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.bio {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
}

.price-block {
  display: grid;
  gap: var(--space-1);
}

.price-label {
  color: var(--color-text-tertiary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.price {
  color: var(--color-primary);
  font-size: 26px;
}

@media (max-width: 640px) {
  .card-bottom {
    align-items: stretch;
    flex-direction: column;
  }

  .card-bottom :deep(.el-button) {
    width: 100%;
  }
}
</style>
