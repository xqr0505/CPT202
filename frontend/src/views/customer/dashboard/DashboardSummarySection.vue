<template>
  <section class="dashboard-summary">
    <div class="dashboard-summary__header">
      <div class="dashboard-summary__copy">
        <h2 class="dashboard-summary__title">{{ DASHBOARD_SECTION_TITLE }}</h2>
        <p class="dashboard-summary__description">
          {{ DASHBOARD_SECTION_DESCRIPTION }}
        </p>
      </div>

      <div class="dashboard-summary__filters desktop-only">
        <el-date-picker
          v-model="dateRange"
          class="dashboard-summary__date-picker"
          popper-class="dashboard-date-range-popper"
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

    <!-- Mobile wheel date filter (English) -->
    <div class="dashboard-summary__filters mobile-only mobile-date-range">
      <div class="mobile-wheel-group">
        <!-- Start date row -->
        <div class="mobile-wheel">
          <select class="mobile-wheel-select" v-model.number="mobileStartYear" aria-label="Start year">
            <option v-for="y in mobileYears" :key="'sy'+y" :value="y">{{ y }}</option>
          </select>
          <select class="mobile-wheel-select" v-model.number="mobileStartMonth" aria-label="Start month">
            <option v-for="(m, idx) in mobileMonths" :key="'sm'+idx" :value="idx + 1">{{ m }}</option>
          </select>
          <select class="mobile-wheel-select" v-model.number="mobileStartDay" aria-label="Start day">
            <option v-for="d in mobileStartDays" :key="'sd'+d" :value="d">{{ d }}</option>
          </select>
        </div>

        <!-- Separator -->
        <span class="mobile-wheel-sep">to</span>

        <!-- End date row -->
        <div class="mobile-wheel">
          <select class="mobile-wheel-select" v-model.number="mobileEndYear" aria-label="End year">
            <option v-for="y in mobileYears" :key="'ey'+y" :value="y">{{ y }}</option>
          </select>
          <select class="mobile-wheel-select" v-model.number="mobileEndMonth" aria-label="End month">
            <option v-for="(m, idx) in mobileMonths" :key="'em'+idx" :value="idx + 1">{{ m }}</option>
          </select>
          <select class="mobile-wheel-select" v-model.number="mobileEndDay" aria-label="End day">
            <option v-for="d in mobileEndDays" :key="'ed'+d" :value="d">{{ d }}</option>
          </select>
        </div>
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

    <el-row
      v-if="viewState === DASHBOARD_VIEW_STATES.loading"
      :gutter="16"
      class="dashboard-summary__grid"
      aria-live="polite"
    >
      <el-col
        v-for="index in DASHBOARD_INITIAL_SKELETON_CARD_COUNT"
        :key="index"
        :xs="12" :sm="12" :md="6"
      >
        <article class="dashboard-summary__card dashboard-summary__card--skeleton">
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
      </el-col>
    </el-row>

    <el-row v-else :gutter="16" class="dashboard-summary__grid">
      <el-col
        v-for="card in statisticCards"
        :key="card.label"
        :xs="12" :sm="12" :md="6"
      >
        <article class="dashboard-summary__card">
          <div class="dashboard-summary__card-header">
            <div class="dashboard-summary__icon">
              <el-icon>
                <component :is="card.icon" />
              </el-icon>
            </div>
            <span class="dashboard-summary__card-label" :title="card.label">{{ card.label }}</span>
          </div>

          <div class="dashboard-summary__card-body">
            <el-statistic
              v-if="card.precision !== undefined"
              :value="card.value"
              :precision="card.precision"
              :prefix="card.prefix"
            />
            <el-statistic v-else :value="card.value" />
          </div>
        </article>
      </el-col>

      <el-col :xs="12" :sm="12" :md="6">
        <article class="dashboard-summary__card dashboard-summary__card--experts">
          <div class="dashboard-summary__card-header">
            <div class="dashboard-summary__icon">
              <el-icon>
                <User />
              </el-icon>
            </div>
            <span class="dashboard-summary__card-label" :title="DASHBOARD_STATISTIC_LABELS.consultedExperts">
              {{ DASHBOARD_STATISTIC_LABELS.consultedExperts }}
            </span>
          </div>

          <div class="dashboard-summary__card-body">
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
          </div>
        </article>
      </el-col>
    </el-row>

    <DashboardTrendChart
      :trend-data="statistics.trendData"
      :view-state="viewState"
      :date-range="dateRange"
      @retry="loadDashboardStatistics"
    />

    <el-row :gutter="24" class="dashboard-bottom-charts">
      <el-col :xs="24" :sm="24" :md="12">
        <DashboardHabitChart
          :habit-data="statistics.habitData"
          :view-state="viewState"
          @retry="loadDashboardStatistics"
        />
      </el-col>
      <el-col :xs="24" :sm="24" :md="12">
        <DashboardDepartmentChart
          :category-data="statistics.categoryData"
          :view-state="viewState"
          @retry="loadDashboardStatistics"
        />
      </el-col>
    </el-row>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type Component, watch } from 'vue'
import { CircleCheck, Clock, Money, User } from '@element-plus/icons-vue'
import {
  getCustomerDashboardStatistics,
  type DashboardStatistics,
  type DashboardStatisticsQuery
} from '@/api/booking'
import CustomButton from '@/components/common/CustomButton.vue'
import DashboardTrendChart from './DashboardTrendChart.vue'
import DashboardDepartmentChart from './DashboardDepartmentChart.vue'
import DashboardHabitChart from './DashboardHabitChart.vue'
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
const mobileStartDate = ref('')
const mobileEndDate = ref('')

// Mobile wheel picker state (English)
const mobileMonths: string[] = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const currentYear = new Date().getFullYear()
const mobileYears = computed<number[]>(() => {
  const years: number[] = []
  for (let y = currentYear; y >= currentYear - 5; y--) years.push(y)
  return years
})

const pad = (n: number) => n.toString().padStart(2, '0')

const mobileStartYear = ref<number>(currentYear)
const mobileStartMonth = ref<number>(new Date().getMonth() + 1)
const mobileStartDay = ref<number>(new Date().getDate())
const mobileEndYear = ref<number>(currentYear)
const mobileEndMonth = ref<number>(new Date().getMonth() + 1)
const mobileEndDay = ref<number>(new Date().getDate())

const daysInMonth = (year: number, month: number): number => {
  return new Date(year, month, 0).getDate()
}

const mobileStartDays = computed<number[]>(() => {
  const count = daysInMonth(mobileStartYear.value, mobileStartMonth.value)
  return Array.from({ length: count }, (_, i) => i + 1)
})

const mobileEndDays = computed<number[]>(() => {
  const count = daysInMonth(mobileEndYear.value, mobileEndMonth.value)
  return Array.from({ length: count }, (_, i) => i + 1)
})

// keep mobileStartDate/mobileEndDate in sync with the wheel selects
watch([mobileStartYear, mobileStartMonth, mobileStartDay], () => {
  mobileStartDate.value = `${mobileStartYear.value}-${pad(mobileStartMonth.value)}-${pad(mobileStartDay.value)}`
  handleMobileDateChange()
})

watch([mobileEndYear, mobileEndMonth, mobileEndDay], () => {
  mobileEndDate.value = `${mobileEndYear.value}-${pad(mobileEndMonth.value)}-${pad(mobileEndDay.value)}`
  handleMobileDateChange()
})

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

const handleMobileDateChange = () => {
  if (mobileStartDate.value && mobileEndDate.value) {
    dateRange.value = [mobileStartDate.value, mobileEndDate.value]
    void loadDashboardStatistics()
  } else if (!mobileStartDate.value && !mobileEndDate.value) {
    dateRange.value = null
    void loadDashboardStatistics()
  }
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

    &.desktop-only { display: flex; }
    &.mobile-only { display: none; }
  }

  .mobile-date-range {
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
    gap: var(--space-2);
    margin-bottom: var(--space-4);
    max-width: 100%;

    span {
      color: var(--color-text-secondary);
    }
    .mobile-wheel-group {
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
      align-items: center;
      max-width: 100%;
    }
    .mobile-wheel {
      display: flex;
      gap: 8px;
      align-items: center;
      justify-content: center;
      width: 100%;
      flex-wrap: wrap;
    }
    .mobile-wheel-select {
      -webkit-appearance: menulist-button;
      appearance: menulist-button;
      height: 40px;
      padding: 6px 8px;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      background: var(--color-bg-surface);
      color: var(--color-text-primary);
      font-size: 13px;
      flex: 1 1 auto;
      min-width: 56px;
      max-width: 80px;
    }
    .mobile-wheel-sep {
      color: var(--color-text-secondary);
      font-size: 12px;
      font-weight: 600;
      margin: var(--space-1) 0;
    }
  }

  .mobile-native-date {
    height: 32px;
    padding: 0 var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background: var(--color-bg-surface);
    color: var(--color-text-primary);
    font-family: inherit;
    font-size: 14px;
    outline: none;

    &:focus {
      border-color: var(--color-primary);
    }
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
    margin-bottom: var(--space-4);
    align-items: stretch;
  }

  .dashboard-bottom-charts {
    margin-top: var(--space-4);
  }

  &__card {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: var(--space-4);
    min-height: 220px;
    height: 220px;
    padding: var(--space-5);
    border: none;
    border-radius: var(--radius-xl);
    background: var(--color-bg-surface);
    box-shadow: none;
    overflow: hidden;
    align-items: center;
    text-align: center;
    width: 100%;
    position: relative;
  }

  /* center numeric content in the visual center of non-expert cards */
  &__card:not(.dashboard-summary__card--experts) {
    > .dashboard-summary__card-header {
      position: absolute;
      top: var(--space-4);
      left: var(--space-5);
      right: var(--space-5);
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: flex-start;
      gap: var(--space-3);
    }

    > .dashboard-summary__card-body {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: calc(100% - var(--space-10));
      justify-content: center;
      align-items: center;
      text-align: center;
    }
  }

  &__card-body {
    display: flex;
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    flex-direction: column;
    justify-content: center; /* vertically center numbers */
    align-items: center; /* horizontally center numbers */
    text-align: center;
  }

  &__card-header {
    display: flex;
    flex-direction: row; /* icon + title on same line */
    align-items: center;
    justify-content: flex-start; /* icon sits at left */
    gap: var(--space-3);
    width: 100%;
  }

  &__card-label {
    color: var(--color-text-secondary);
    font-size: calc(var(--space-3) + var(--space-1));
    font-weight: 600;
    line-height: calc(var(--space-4) + var(--space-3));
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    flex: 1 1 auto;
    text-align: left; /* title in single line next to icon */
  }

  &__card:hover {
    /* No shadow on hover as per prompt */
  }

  &__card--skeleton {
    justify-content: center;
  }

  &__card--experts {
    justify-content: flex-start;
    align-items: stretch;
    text-align: left;
  }

  /* Expert-card header should remain left aligned */
  .dashboard-summary__card--experts .dashboard-summary__card-header {
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
  }

  .dashboard-summary__experts {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    min-height: 0;
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
  /* label styling consolidated above */

  /* spacing: keep icon flush-left in header */
  .dashboard-summary__card-header > .dashboard-summary__icon {
    margin-right: var(--space-3);
  }

  /* Experts card body should keep list alignment (do not center) */
  .dashboard-summary__card--experts .dashboard-summary__card-body {
    justify-content: flex-start;
    align-items: stretch;
    overflow-y: auto;
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
    text-align: center;
  }

  :deep(.el-statistic__number) {
    font-size: calc(var(--space-6) + var(--space-2));
  }

  :deep(.dashboard-summary__grid > .el-col) {
    display: flex;
  }

  :deep(.dashboard-summary__grid.el-row) {
    row-gap: var(--space-4);
  }

  :deep(.dashboard-summary__date-picker.el-date-editor) {
    --el-color-primary: var(--color-primary);
    --el-date-editor-daterange-active-color: var(--color-primary);
    border-color: var(--color-border);
    background-color: var(--color-bg-surface);
  }

  :deep(.dashboard-summary__date-picker.el-date-editor:hover) {
    border-color: var(--color-primary);
  }

  :deep(.dashboard-summary__date-picker.is-active) {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 1px var(--color-primary);
  }

  @media (max-width: 900px) {
    &__filters {
      width: 100%;
      align-items: stretch;

      &.desktop-only { display: none; }
      &.mobile-only { display: flex; }
    }

    .mobile-native-date {
      flex: 1;
      width: 0;
    }

      &__title {
        font-size: 16px;
      }

      &__description {
        font-size: 12px;
      }

      &__card {
          min-height: 120px;
          height: 120px;
          padding: var(--space-2);
          gap: var(--space-2);
          background: var(--color-bg-summary-mobile) !important;
      }

      :deep(.el-statistic__number) {
        font-size: 18px;
      }

      &__card-label {
        font-size: 10px;
        text-align: left;
      }

      &__expert-avatar {
        width: 32px !important;
        height: 32px !important;
        font-size: 12px !important;
      }
      &__expert-name {
        font-size: 9px;
        font-weight: 600;
        line-height: 1.2;
      }

      &__filter-hint {
        text-align: left;
      }
  }
}
</style>

<style lang="scss">
.dashboard-date-range-popper {
  --el-color-primary: var(--color-primary);
  --el-color-primary-light-3: rgba(var(--color-primary-rgb), 0.72);
  --el-color-primary-light-5: rgba(var(--color-primary-rgb), 0.52);
  --el-datepicker-inrange-bg-color: rgba(var(--color-primary-rgb), 0.14);
}
</style>
