<template>
  <article class="dashboard-department">
    <div class="dashboard-department__header">
      <h3 class="dashboard-department__title">{{ DASHBOARD_DEPARTMENT_TITLE }}</h3>
      <p class="dashboard-department__description">{{ DASHBOARD_DEPARTMENT_DESCRIPTION }}</p>
    </div>

    <div v-if="viewState === DASHBOARD_VIEW_STATES.loading" class="dashboard-department__skeleton">
      <el-skeleton animated :rows="DASHBOARD_DEPARTMENT_LOADING_ROWS" />
    </div>

    <div v-else-if="viewState === DASHBOARD_VIEW_STATES.error" class="dashboard-department__state-card">
      <div>
        <h4 class="dashboard-department__state-title">{{ DASHBOARD_ERROR_TITLE }}</h4>
        <p class="dashboard-department__state-text">{{ DASHBOARD_ERROR_MESSAGE }}</p>
      </div>
      <CustomButton type="primary" @click="emitRetry">
        {{ DASHBOARD_RETRY_LABEL }}
      </CustomButton>
    </div>

    <EmptyPlaceholder
      v-else-if="!hasData"
      :description="DASHBOARD_DEPARTMENT_EMPTY_TEXT"
      class="dashboard-department__empty"
    />

    <div
      v-else
      class="dashboard-department__chart-wrapper"
      :style="{ height: `${DASHBOARD_DEPARTMENT_CHART_HEIGHT}px` }"
    >
      <div ref="chartElementRef" class="dashboard-department__canvas" />
      <div class="dashboard-department__center">
        <span class="dashboard-department__center-value">{{ formattedTotalAmount }}</span>
        <span class="dashboard-department__center-label">{{ DASHBOARD_DEPARTMENT_CENTER_LABEL }}</span>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'
import type { DashboardCategoryItem } from '@/api/booking'
import type { DashboardViewState } from '@/constants/dashboard'
import {
  DASHBOARD_CURRENCY_PREFIX,
  DASHBOARD_DEPARTMENT_CENTER_LABEL,
  DASHBOARD_DEPARTMENT_CHART_HEIGHT,
  DASHBOARD_DEPARTMENT_COLOR_TOKEN_PALETTE,
  DASHBOARD_DEPARTMENT_DESCRIPTION,
  DASHBOARD_DEPARTMENT_EMPTY_TEXT,
  DASHBOARD_DEPARTMENT_FALLBACK_PALETTE,
  DASHBOARD_DEPARTMENT_LEGEND_ICON,
  DASHBOARD_DEPARTMENT_LEGEND_ITEM_GAP,
  DASHBOARD_DEPARTMENT_LOADING_ROWS,
  DASHBOARD_DEPARTMENT_PERCENT_PRECISION,
  DASHBOARD_DEPARTMENT_RADIUS_INNER,
  DASHBOARD_DEPARTMENT_RADIUS_OUTER,
  DASHBOARD_DEPARTMENT_TITLE,
  DASHBOARD_DEPARTMENT_TOOLTIP_AMOUNT_LABEL,
  DASHBOARD_DEPARTMENT_TOOLTIP_SHARE_LABEL,
  DASHBOARD_DEPARTMENT_TOOLTIP_VISITS_LABEL,
  DASHBOARD_ERROR_MESSAGE,
  DASHBOARD_ERROR_TITLE,
  DASHBOARD_RETRY_LABEL,
  DASHBOARD_VIEW_STATES
} from '@/constants/dashboard'
import CustomButton from '@/components/common/CustomButton.vue'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'

defineOptions({ name: 'DashboardDepartmentChart' })

interface Props {
  categoryData: DashboardCategoryItem[]
  viewState: DashboardViewState
}

const props = defineProps<Props>()

const emit = defineEmits<{
  retry: []
}>()

const chartElementRef = ref<HTMLDivElement | null>(null)
let chartInstance: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const hasData = computed<boolean>(() => props.categoryData.length > 0)

const totalAmount = computed<number>(() =>
  props.categoryData.reduce((sum, item) => sum + Number(item.amount ?? 0), 0)
)

const formattedTotalAmount = computed<string>(() =>
  `${DASHBOARD_CURRENCY_PREFIX}${new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(totalAmount.value)}`
)

const emitRetry = (): void => {
  emit('retry')
}

const getPalette = (): string[] => {
  const rawTokenValue = getComputedStyle(document.documentElement)
    .getPropertyValue(DASHBOARD_DEPARTMENT_COLOR_TOKEN_PALETTE)
    .trim()
  if (!rawTokenValue) {
    return [...DASHBOARD_DEPARTMENT_FALLBACK_PALETTE]
  }
  return rawTokenValue
    .split(',')
    .map((color) => color.trim())
    .filter(Boolean)
}

const buildOption = (): EChartsOption => {
  const pieData = props.categoryData.map((item) => ({
    name: item.categoryName,
    value: Number(item.amount ?? 0),
    count: Number(item.count ?? 0)
  }))

  return {
    color: getPalette(),
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const currentParam = Array.isArray(params) ? params[0] : params
        const typedParam = (currentParam ?? {}) as {
          value?: number
          percent?: number
          data?: { count?: number }
          name?: string
        }
        const amountValue = Number(typedParam.value ?? 0)
        const percentValue = Number(typedParam.percent ?? 0)
        const countValue = Number(typedParam.data?.count ?? 0)
        return [
          `${typedParam.name ?? ''}`,
          `${DASHBOARD_DEPARTMENT_TOOLTIP_AMOUNT_LABEL}: ${DASHBOARD_CURRENCY_PREFIX}${new Intl.NumberFormat('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
          }).format(amountValue)}`,
          `${DASHBOARD_DEPARTMENT_TOOLTIP_SHARE_LABEL}: ${percentValue.toFixed(DASHBOARD_DEPARTMENT_PERCENT_PRECISION)}%`,
          `${DASHBOARD_DEPARTMENT_TOOLTIP_VISITS_LABEL}: ${countValue}`
        ].join('<br/>')
      }
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemGap: DASHBOARD_DEPARTMENT_LEGEND_ITEM_GAP,
      icon: DASHBOARD_DEPARTMENT_LEGEND_ICON
    },
    series: [
      {
        type: 'pie',
        radius: [DASHBOARD_DEPARTMENT_RADIUS_INNER, DASHBOARD_DEPARTMENT_RADIUS_OUTER],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        label: { show: false },
        labelLine: { show: false },
        data: pieData
      }
    ]
  }
}

const renderChart = async (): Promise<void> => {
  if (props.viewState !== DASHBOARD_VIEW_STATES.ready || !hasData.value) {
    return
  }

  await nextTick()
  if (!chartElementRef.value) {
    return
  }

  if (!chartInstance) {
    chartInstance = echarts.init(chartElementRef.value)
  }

  chartInstance.setOption(buildOption(), true)
  chartInstance.resize()

  if (resizeObserver) {
    resizeObserver.observe(chartElementRef.value)
  }
}

const disposeChart = (): void => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

onMounted(() => {
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize()
    })
    if (chartElementRef.value) {
      resizeObserver.observe(chartElementRef.value)
    }
  }
  void renderChart()
})

watch(
  () => [props.categoryData, props.viewState] as const,
  () => {
    if (props.viewState !== DASHBOARD_VIEW_STATES.ready || !hasData.value) {
      disposeChart()
      return
    }
    void renderChart()
  },
  { deep: true, flush: 'post' }
)

onBeforeUnmount(() => {
  if (resizeObserver && chartElementRef.value) {
    resizeObserver.unobserve(chartElementRef.value)
  }
  resizeObserver = null
  disposeChart()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.dashboard-department {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 var(--space-1) var(--space-4) var(--color-shadow);

  &__header {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  &__title {
    margin: 0;
    color: var(--color-text-primary);
    font-size: calc(var(--space-4) + var(--space-1));
    font-weight: 700;
  }

  &__description {
    margin: 0;
    color: var(--color-text-secondary);
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
    border-radius: var(--radius-md);
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

  &__skeleton {
    padding: var(--space-2) 0;
  }

  &__empty {
    margin: 0;
  }

  &__chart-wrapper {
    position: relative;
    width: 100%;
  }

  &__canvas {
    width: 100%;
    height: 100%;
  }

  &__center {
    position: absolute;
    left: 50%;
    top: 42%;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
    pointer-events: none;
  }

  &__center-value {
    color: var(--color-text-primary);
    font-size: calc(var(--space-6) + var(--space-1));
    font-weight: 700;
    line-height: 1;
  }

  &__center-label {
    color: var(--color-text-secondary);
    font-size: calc(var(--space-3) + var(--space-1));
    font-weight: 600;
  }
}
</style>
