<template>
  <article class="dashboard-trend">
    <div class="dashboard-trend__header">
      <h3 class="dashboard-trend__title">{{ DASHBOARD_TREND_TITLE }}</h3>
      <p class="dashboard-trend__description">{{ DASHBOARD_TREND_DESCRIPTION }}</p>
    </div>

    <div v-if="viewState === DASHBOARD_VIEW_STATES.loading" class="dashboard-trend__skeleton">
      <el-skeleton animated :rows="DASHBOARD_CHART_LOADING_ROWS" />
    </div>

    <div v-else-if="viewState === DASHBOARD_VIEW_STATES.error" class="dashboard-trend__state-card">
      <div>
        <h4 class="dashboard-trend__state-title">{{ DASHBOARD_ERROR_TITLE }}</h4>
        <p class="dashboard-trend__state-text">{{ DASHBOARD_ERROR_MESSAGE }}</p>
      </div>
      <CustomButton type="primary" @click="emitRetry">
        {{ DASHBOARD_RETRY_LABEL }}
      </CustomButton>
    </div>

    <EmptyPlaceholder
      v-else-if="!hasData"
      :description="DASHBOARD_CHART_EMPTY_TEXT"
      class="dashboard-trend__empty"
    />

    <div
      v-else
      ref="chartElementRef"
      class="dashboard-trend__canvas"
      :style="{ height: `${DASHBOARD_CHART_HEIGHT}px` }"
    />
  </article>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'
import type { DashboardDateRangeNullable, DashboardViewState } from '@/constants/dashboard'
import type { DashboardTrendItem } from '@/api/booking'
import {
  DASHBOARD_CHART_COLOR_TOKEN_FILL,
  DASHBOARD_CHART_COLOR_TOKEN_LINE,
  DASHBOARD_CHART_EMPTY_TEXT,
  DASHBOARD_CHART_FALLBACK_FILL_COLOR,
  DASHBOARD_CHART_FALLBACK_LINE_COLOR,
  DASHBOARD_CHART_GRID_BOTTOM,
  DASHBOARD_CHART_GRID_LEFT,
  DASHBOARD_CHART_GRID_RIGHT,
  DASHBOARD_CHART_GRID_TOP,
  DASHBOARD_CHART_HEIGHT,
  DASHBOARD_CHART_LOADING_ROWS,
  DASHBOARD_CHART_LOCALE,
  DASHBOARD_CHART_TOOLTIP_APPOINTMENT_LABEL_PLURAL,
  DASHBOARD_CHART_TOOLTIP_APPOINTMENT_LABEL_SINGULAR,
  DASHBOARD_CHART_TOOLTIP_HOUR_LABEL_PLURAL,
  DASHBOARD_CHART_TOOLTIP_HOUR_LABEL_SINGULAR,
  DASHBOARD_CHART_TRANSPARENT_COLOR,
  DASHBOARD_DAY_LABEL_FORMAT,
  DASHBOARD_ERROR_MESSAGE,
  DASHBOARD_ERROR_TITLE,
  DASHBOARD_HOUR_VALUE_FORMAT,
  DASHBOARD_LINE_AREA_OPACITY,
  DASHBOARD_LINE_SMOOTH,
  DASHBOARD_LINE_SYMBOL_SIZE,
  DASHBOARD_LINE_WIDTH,
  DASHBOARD_MONTH_LABEL_FORMAT,
  DASHBOARD_RETRY_LABEL,
  DASHBOARD_TREND_DESCRIPTION,
  DASHBOARD_TREND_TITLE,
  DASHBOARD_VIEW_STATES,
  DASHBOARD_DATE_LABEL_DAY_PATTERN,
  DASHBOARD_DATE_LABEL_MONTH_PATTERN
} from '@/constants/dashboard'
import CustomButton from '@/components/common/CustomButton.vue'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'

defineOptions({ name: 'DashboardTrendChart' })

interface Props {
  trendData: DashboardTrendItem[]
  viewState: DashboardViewState
  dateRange: DashboardDateRangeNullable
}

const props = defineProps<Props>()

const emit = defineEmits<{
  retry: []
}>()

const chartElementRef = ref<HTMLDivElement | null>(null)
let chartInstance: ECharts | null = null
let resizeObserver: ResizeObserver | null = null
let themeObserver: MutationObserver | null = null

const hasData = computed<boolean>(() => props.trendData.length > 0)

const emitRetry = (): void => {
  emit('retry')
}

const getCssVariable = (token: string, fallbackValue: string): string => {
  const tokenValue = getComputedStyle(document.documentElement).getPropertyValue(token).trim()
  return tokenValue || fallbackValue
}

const formatFriendlyDateLabel = (dateLabel: string): string => {
  if (DASHBOARD_DATE_LABEL_MONTH_PATTERN.test(dateLabel)) {
    const parsedDate = new Date(`${dateLabel}-01T00:00:00`)
    return new Intl.DateTimeFormat(DASHBOARD_CHART_LOCALE, DASHBOARD_MONTH_LABEL_FORMAT).format(parsedDate)
  }

  if (DASHBOARD_DATE_LABEL_DAY_PATTERN.test(dateLabel)) {
    const activeYear = props.dateRange?.[0]?.slice(0, 4) ?? new Date().getFullYear().toString()
    const parsedDate = new Date(`${activeYear}-${dateLabel}T00:00:00`)
    return new Intl.DateTimeFormat(DASHBOARD_CHART_LOCALE, DASHBOARD_DAY_LABEL_FORMAT).format(parsedDate)
  }

  return dateLabel
}

const formatPluralLabel = (value: number, singularLabel: string, pluralLabel: string): string => {
  return value === 1 ? singularLabel : pluralLabel
}

const buildOption = (): EChartsOption => {
  const lineColor = getCssVariable(DASHBOARD_CHART_COLOR_TOKEN_LINE, DASHBOARD_CHART_FALLBACK_LINE_COLOR)
  const fillColor = getCssVariable(DASHBOARD_CHART_COLOR_TOKEN_FILL, DASHBOARD_CHART_FALLBACK_FILL_COLOR)
  const textColor = getCssVariable('--color-text-primary', '#2f3e36')
  const xAxisData = props.trendData.map((item) => item.dateLabel)
  const seriesData = props.trendData.map((item) => item.count)

  return {
    textStyle: { color: textColor },
    tooltip: {
      trigger: 'axis',
      textStyle: { color: textColor },
      formatter: (params) => {
        const firstParam = Array.isArray(params) ? params[0] : params
        const dataIndex = firstParam?.dataIndex ?? 0
        const currentItem = props.trendData[dataIndex]
        const countValue = currentItem?.count ?? 0
        const hoursValue = new Intl.NumberFormat(
          DASHBOARD_CHART_LOCALE,
          DASHBOARD_HOUR_VALUE_FORMAT
        ).format(currentItem?.hours ?? 0)
        const dateText = formatFriendlyDateLabel(currentItem?.dateLabel ?? '')
        const appointmentLabel = formatPluralLabel(
          countValue,
          DASHBOARD_CHART_TOOLTIP_APPOINTMENT_LABEL_SINGULAR,
          DASHBOARD_CHART_TOOLTIP_APPOINTMENT_LABEL_PLURAL
        )
        const hourLabel = formatPluralLabel(
          currentItem?.hours ?? 0,
          DASHBOARD_CHART_TOOLTIP_HOUR_LABEL_SINGULAR,
          DASHBOARD_CHART_TOOLTIP_HOUR_LABEL_PLURAL
        )
        return `${dateText}: ${countValue} ${appointmentLabel}, ${hoursValue} ${hourLabel}`
      }
    },
    grid: {
      top: DASHBOARD_CHART_GRID_TOP,
      right: DASHBOARD_CHART_GRID_RIGHT,
      bottom: DASHBOARD_CHART_GRID_BOTTOM,
      left: DASHBOARD_CHART_GRID_LEFT,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData,
      axisLabel: { color: textColor }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: textColor }
    },
    series: [
      {
        type: 'line',
        smooth: DASHBOARD_LINE_SMOOTH,
        data: seriesData,
        showSymbol: true,
        symbolSize: DASHBOARD_LINE_SYMBOL_SIZE,
        lineStyle: {
          width: DASHBOARD_LINE_WIDTH,
          color: lineColor
        },
        areaStyle: {
          opacity: DASHBOARD_LINE_AREA_OPACITY,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: fillColor },
            { offset: 1, color: DASHBOARD_CHART_TRANSPARENT_COLOR }
          ])
        },
        itemStyle: {
          color: lineColor
        }
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

  // Re-render chart options when theme changes
  if (typeof MutationObserver !== 'undefined') {
    themeObserver = new MutationObserver(() => {
      if (chartInstance) {
        chartInstance.setOption(buildOption(), true)
        chartInstance.resize()
      } else {
        void renderChart()
      }
    })
    themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
  }
})

watch(
  () => [props.trendData, props.viewState, props.dateRange] as const,
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
  if (themeObserver) {
    themeObserver.disconnect()
  }
  themeObserver = null
  disposeChart()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.dashboard-trend {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-5);
  border: none;
  background: var(--color-bg-surface);
  border-radius: var(--radius-xl);
  box-shadow: none;

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

  &__canvas {
    width: 100%;
  }

  @media (max-width: 900px) {
    &__title {
      font-size: 18px;
    }
  }
}
</style>
