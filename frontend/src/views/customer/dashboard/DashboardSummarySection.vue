<template>
  <section class="dashboard-summary">
    <div class="dashboard-summary__header">
      <div class="dashboard-summary__copy">
        <h2 class="dashboard-summary__title">{{ DASHBOARD_SECTION_TITLE }}</h2>
        <p class="dashboard-summary__description">
          {{ DASHBOARD_SECTION_DESCRIPTION }}
        </p>
      </div>

      <div class="dashboard-summary__filters">
        <el-date-picker
          v-model="dateRange"
          class="dashboard-summary__date-picker"
          :type="DASHBOARD_DATE_PICKER_TYPE"
          :value-format="DASHBOARD_DATE_VALUE_FORMAT"
          :range-separator="DASHBOARD_DATE_RANGE_SEPARATOR"
          :start-placeholder="DASHBOARD_DATE_START_PLACEHOLDER"
          :end-placeholder="DASHBOARD_DATE_END_PLACEHOLDER"
          :clearable="true"
          unlink-panels
          @change="handleDateRangeChange"
        />
      </div>
    </div>

    <div v-if="viewState === DASHBOARD_VIEW_STATES.error" class="dashboard-summary__state-card">
      <div>
        <h3 class="dashboard-summary__state-title">{{ DASHBOARD_ERROR_TITLE }}</h3>
        <p class="dashboard-summary__state-text">{{ DASHBOARD_ERROR_MESSAGE }}</p>
      </div>
      <CustomButton type="primary" @click="loadDashboardStatistics">
        {{ DASHBOARD_RETRY_LABEL }}
      </CustomButton>
    </div>

    <div
      v-if="viewState === DASHBOARD_VIEW_STATES.loading"
      class="dashboard-summary__grid"
      aria-live="polite"
    >
      <article
        v-for="index in DASHBOARD_INITIAL_SKELETON_CARD_COUNT"
        :key="index"
        class="dashboard-summary__card dashboard-summary__card--skeleton"
      >
        <el-skeleton animated>
          <template #template>
            <div class="dashboard-summary__skeleton-shell">
              <el-skeleton-item variant="text" class="dashboard-summary__skeleton-label" />
              <el-skeleton-item variant="h1" class="dashboard-summary__skeleton-value" />
              <el-skeleton-item variant="text" class="dashboard-summary__skeleton-text" />
            </div>
          </template>
        </el-skeleton>
      </article>
    </div>

    <div v-else class="dashboard-summary__grid">
      <article
        v-for="card in statisticCards"
        :key="card.label"
        class="dashboard-summary__card"
      >
        <div class="dashboard-summary__card-header">
          <div class="dashboard-summary__icon">
            <el-icon>
              <component :is="card.icon" />
            </el-icon>
          </div>
          <span class="dashboard-summary__card-label">{{ card.label }}</span>
        </div>

        <el-statistic
          v-if="card.precision !== undefined"
          :value="card.value"
          :precision="card.precision"
          :prefix="card.prefix"
        />
        <el-statistic v-else :value="card.value" />
      </article>

      <article class="dashboard-summary__card dashboard-summary__card--experts">
        <div class="dashboard-summary__card-header">
          <div class="dashboard-summary__icon">
            <el-icon>
              <User />
            </el-icon>
          </div>
          <span class="dashboard-summary__card-label">
            {{ DASHBOARD_STATISTIC_LABELS.consultedExperts }}
          </span>
        </div>

        <div v-if="statistics.consultedExperts.length === 0" class="dashboard-summary__empty-text">
          {{ DASHBOARD_EMPTY_EXPERTS_TEXT }}
        </div>

        <div v-else class="dashboard-summary__experts">
          <div
            v-for="expert in statistics.consultedExperts"
            :key="expert.specialistId"
            class="dashboard-summary__expert"
          >
            <el-avatar
              :size="DASHBOARD_EXPERT_AVATAR_SIZE"
              :src="expert.specialistAvatar"
              class="dashboard-summary__expert-avatar"
            >
              {{ getExpertInitials(expert.specialistName) }}
            </el-avatar>
            <span class="dashboard-summary__expert-name">
              {{ expert.specialistName }}
            </span>
          </div>
        </div>
      </article>
    </div>

    <DashboardTrendChart
      :trend-data="statistics.trendData"
      :view-state="viewState"
      :date-range="dateRange"
      @retry="loadDashboardStatistics"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type Component } from 'vue'
import { CircleCheck, Clock, Money, User } from '@element-plus/icons-vue'
import {
  getCustomerDashboardStatistics,
  type DashboardStatistics,
  type DashboardStatisticsQuery
} from '@/api/booking'
import CustomButton from '@/components/common/CustomButton.vue'
import DashboardTrendChart from './DashboardTrendChart.vue'
import {
  createEmptyDashboardStatistics,
  DASHBOARD_AMOUNT_PRECISION,
  DASHBOARD_CURRENCY_PREFIX,
  DASHBOARD_DATE_END_PLACEHOLDER,
  DASHBOARD_DATE_PICKER_TYPE,
  DASHBOARD_DATE_RANGE_SEPARATOR,
  DASHBOARD_DATE_START_PLACEHOLDER,
  DASHBOARD_DATE_VALUE_FORMAT,
  DASHBOARD_EMPTY_EXPERTS_TEXT,
  DASHBOARD_ERROR_MESSAGE,
  DASHBOARD_ERROR_TITLE,
  DASHBOARD_EXPERT_AVATAR_SIZE,
  DASHBOARD_EXPERT_INITIAL_LIMIT,
  DASHBOARD_INITIAL_SKELETON_CARD_COUNT,
  DASHBOARD_RETRY_LABEL,
  DASHBOARD_SECTION_DESCRIPTION,
  DASHBOARD_SECTION_TITLE,
  DASHBOARD_STATISTIC_LABELS,
  DASHBOARD_VIEW_STATES,
  type DashboardDateRangeNullable,
  type DashboardViewState
} from '@/constants/dashboard'

defineOptions({ name: 'DashboardSummarySection' })

interface StatisticCardDefinition {
  label: string
  value: number
  icon: Component
  precision?: number
  prefix?: string
}

const dateRange = ref<DashboardDateRangeNullable>(null)
const statistics = ref<DashboardStatistics>(createEmptyDashboardStatistics())
const viewState = ref<DashboardViewState>(DASHBOARD_VIEW_STATES.loading)

const statisticCards = computed<StatisticCardDefinition[]>(() => {
  return [
    {
      label: DASHBOARD_STATISTIC_LABELS.totalCompletedAppointments,
      value: statistics.value.totalCompletedAppointments,
      icon: CircleCheck
    },
    {
      label: DASHBOARD_STATISTIC_LABELS.totalAmountSpent,
      value: statistics.value.totalAmountSpent,
      icon: Money,
      precision: DASHBOARD_AMOUNT_PRECISION,
      prefix: DASHBOARD_CURRENCY_PREFIX
    },
    {
      label: DASHBOARD_STATISTIC_LABELS.totalConsultationHours,
      value: statistics.value.totalConsultationHours,
      icon: Clock
    }
  ]
})

const loadDashboardStatistics = async (): Promise<void> => {
  viewState.value = DASHBOARD_VIEW_STATES.loading

  try {
    const query: DashboardStatisticsQuery = {}

    if (dateRange.value) {
      const [startDate, endDate] = dateRange.value
      query.startDate = startDate
      query.endDate = endDate
    }

    const response = await getCustomerDashboardStatistics(query)

    statistics.value = response
    viewState.value = DASHBOARD_VIEW_STATES.ready
  } catch {
    statistics.value = createEmptyDashboardStatistics()
    viewState.value = DASHBOARD_VIEW_STATES.error
  }
}

const handleDateRangeChange = (value: DashboardDateRangeNullable): void => {
  if (!value) {
    dateRange.value = null
    void loadDashboardStatistics()
    return
  }

  const [startDate, endDate] = value

  if (!startDate || !endDate) {
    return
  }

  dateRange.value = [startDate, endDate]
  void loadDashboardStatistics()
}

const getExpertInitials = (specialistName: string): string => {
  return specialistName
    .split(' ')
    .filter(Boolean)
    .slice(0, DASHBOARD_EXPERT_INITIAL_LIMIT)
    .map((word) => word.charAt(0))
    .join('')
    .toUpperCase()
}

onMounted(() => {
  void loadDashboardStatistics()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.dashboard-summary {
  --dashboard-summary-card-min-width: calc(var(--space-12) * 4 + var(--space-6));

  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding-top: var(--space-2);

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-4);
    flex-wrap: wrap;
  }

  &__copy {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    min-width: 0;
  }

  &__title {
    margin: 0;
    color: var(--color-text-primary);
    font-size: calc(var(--space-4) + var(--space-2));
    font-weight: 700;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__description {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: calc(var(--space-3) + var(--space-1));
    line-height: calc(var(--space-4) + var(--space-3));
  }

  &__date-picker {
    width: min(100%, calc(var(--space-12) * 7));
  }

  &__filters {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: var(--space-1);
  }

  &__filter-hint {
    margin: 0;
    color: var(--color-text-tertiary);
    font-size: calc(var(--space-3) + var(--space-1));
    line-height: calc(var(--space-4) + var(--space-2));
  }

  &__state-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-4);
    flex-wrap: wrap;
    padding: var(--space-4);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background: var(--color-bg-page);
  }

  &__state-title {
    margin: 0 0 var(--space-1);
    color: var(--color-text-primary);
    font-size: calc(var(--space-4) + var(--space-1));
    font-weight: 600;
  }

  &__state-text {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: calc(var(--space-3) + var(--space-1));
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(var(--dashboard-summary-card-min-width), 1fr));
    gap: var(--space-4);
  }

  &__card {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    min-height: calc(var(--space-12) * 4);
    padding: var(--space-5);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: 0 var(--space-1) var(--space-4) var(--color-shadow);
    transition: transform var(--transition-base), box-shadow var(--transition-base);
  }

  &__card:hover {
    transform: translateY(calc(var(--space-1) * -1));
    box-shadow: 0 var(--space-2) var(--space-5) var(--color-shadow);
  }

  &__card--skeleton {
    justify-content: center;
  }

  &__card--experts {
    justify-content: flex-start;
  }

  &__card-header {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: calc(var(--space-6) + var(--space-2));
    height: calc(var(--space-6) + var(--space-2));
    border-radius: var(--radius-md);
    background: var(--color-primary-soft);
    color: var(--color-text-inverse);
    flex-shrink: 0;
  }

  &__card-label {
    color: var(--color-text-secondary);
    font-size: calc(var(--space-3) + var(--space-1));
    font-weight: 600;
    line-height: calc(var(--space-4) + var(--space-3));
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__experts {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  &__expert {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  &__expert-avatar {
    background: var(--color-bg-page);
    color: var(--color-primary);
    border: 1px solid var(--color-border);
    flex-shrink: 0;
  }

  &__expert-name {
    color: var(--color-text-primary);
    font-size: calc(var(--space-3) + var(--space-1));
    font-weight: 600;
    line-height: calc(var(--space-4) + var(--space-3));
  }

  &__empty-text {
    color: var(--color-text-secondary);
    font-size: calc(var(--space-3) + var(--space-1));
    line-height: calc(var(--space-4) + var(--space-3));
  }

  &__skeleton-shell {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  &__skeleton-label {
    width: calc(var(--space-12) * 2);
  }

  &__skeleton-value {
    width: calc(var(--space-12) * 2 + var(--space-6));
  }

  &__skeleton-text {
    width: calc(var(--space-12) * 3);
  }

  :deep(.el-statistic__content) {
    color: var(--color-text-primary);
    font-weight: 700;
  }

  :deep(.el-statistic__number) {
    font-size: calc(var(--space-6) + var(--space-2));
  }

  @media (max-width: 768px) {
    &__filters {
      width: 100%;
      align-items: stretch;
    }

    &__date-picker {
      width: 100%;
    }

    &__filter-hint {
      text-align: left;
    }
  }
}
</style>
