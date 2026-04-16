<template>
  <article class="dashboard-habit">
    <div class="dashboard-habit__header">
      <h3 class="dashboard-habit__title">{{ DASHBOARD_HABIT_TITLE }}</h3>
      <p class="dashboard-habit__description">{{ DASHBOARD_HABIT_DESCRIPTION }}</p>
    </div>

    <div v-if="viewState === DASHBOARD_VIEW_STATES.loading" class="dashboard-habit__skeleton">
      <el-skeleton animated :rows="DASHBOARD_HABIT_LOADING_ROWS" />
    </div>

    <div v-else-if="viewState === DASHBOARD_VIEW_STATES.error" class="dashboard-habit__state-card">
      <div>
        <h4 class="dashboard-habit__state-title">{{ DASHBOARD_ERROR_TITLE }}</h4>
        <p class="dashboard-habit__state-text">{{ DASHBOARD_ERROR_MESSAGE }}</p>
      </div>
      <CustomButton type="primary" @click="emitRetry">
        {{ DASHBOARD_RETRY_LABEL }}
      </CustomButton>
    </div>

    <EmptyPlaceholder
      v-else-if="!hasData"
      :description="DASHBOARD_HABIT_EMPTY_TEXT"
      class="dashboard-habit__empty"
    />

    <div
      v-else
      ref="chartElementRef"
      class="dashboard-habit__canvas"
      :style="{ height: `${DASHBOARD_HABIT_CHART_HEIGHT}px` }"
    />
  </article>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'
import type { DashboardHabitItem } from '@/api/booking'
import type { DashboardViewState } from '@/constants/dashboard'
import {
  DASHBOARD_ERROR_MESSAGE,
  DASHBOARD_ERROR_TITLE,
  DASHBOARD_HABIT_CHART_HEIGHT,
  DASHBOARD_HABIT_COLOR_TOKEN_FILL,
  DASHBOARD_HABIT_COLOR_TOKEN_LINE,
  DASHBOARD_HABIT_COLOR_TOKEN_POINT,
  DASHBOARD_HABIT_COLOR_TOKEN_SPLIT_LINE,
  DASHBOARD_HABIT_DESCRIPTION,
  DASHBOARD_HABIT_EMPTY_TEXT,
  DASHBOARD_HABIT_FALLBACK_FILL_COLOR,
  DASHBOARD_HABIT_FALLBACK_LINE_COLOR,
  DASHBOARD_HABIT_FALLBACK_POINT_COLOR,
  DASHBOARD_HABIT_FALLBACK_SPLIT_LINE_COLOR,
  DASHBOARD_HABIT_LOADING_ROWS,
  DASHBOARD_HABIT_RADAR_LINE_WIDTH,
  DASHBOARD_HABIT_RADAR_MAX_PADDING,
  DASHBOARD_HABIT_RADAR_MIN_MAX_VALUE,
  DASHBOARD_HABIT_RADAR_RADIUS,
  DASHBOARD_HABIT_RADAR_SPLIT_NUMBER,
  DASHBOARD_HABIT_RADAR_SYMBOL_SIZE,
  DASHBOARD_HABIT_TITLE,
  DASHBOARD_HABIT_TOOLTIP_VISITS_LABEL,
  DASHBOARD_RETRY_LABEL,
  DASHBOARD_VIEW_STATES,
  DASHBOARD_WEEKDAY_ORDER
} from '@/constants/dashboard'
import CustomButton from '@/components/common/CustomButton.vue'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'

defineOptions({ name: 'DashboardHabitChart' })

interface Props {
  habitData: DashboardHabitItem[]
  viewState: DashboardViewState
}

const props = defineProps<Props>()

const emit = defineEmits<{
  retry: []
}>()

const chartElementRef = ref<HTMLDivElement | null>(null)
let chartInstance: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const weekdayLabelSet = new Set<string>(DASHBOARD_WEEKDAY_ORDER)

const normalizedHabitData = computed(() => {
  const countByWeekday = new Map<string, number>()
  props.habitData.forEach((item) => {
    const dayLabel = item.dayOfWeek
    if (!weekdayLabelSet.has(dayLabel)) {
      return
    }
    countByWeekday.set(dayLabel, Number(item.count ?? 0))
  })

  return DASHBOARD_WEEKDAY_ORDER.map((day) => ({
    day,
    count: countByWeekday.get(day) ?? 0
  }))
})

const hasData = computed<boolean>(() =>
  normalizedHabitData.value.some((item) => item.count > 0)
)

const radarMaxValue = computed<number>(() => {
  const maxValue = Math.max(...normalizedHabitData.value.map((item) => item.count), 0)
  return Math.max(maxValue + DASHBOARD_HABIT_RADAR_MAX_PADDING, DASHBOARD_HABIT_RADAR_MIN_MAX_VALUE)
})

const emitRetry = (): void => {
  emit('retry')
}

const getCssVariable = (token: string, fallbackValue: string): string => {
  const tokenValue = getComputedStyle(document.documentElement).getPropertyValue(token).trim()
  return tokenValue || fallbackValue
}

const buildOption = (): EChartsOption => {
  const lineColor = getCssVariable(DASHBOARD_HABIT_COLOR_TOKEN_LINE, DASHBOARD_HABIT_FALLBACK_LINE_COLOR)
  const fillColor = getCssVariable(DASHBOARD_HABIT_COLOR_TOKEN_FILL, DASHBOARD_HABIT_FALLBACK_FILL_COLOR)
  const pointColor = getCssVariable(DASHBOARD_HABIT_COLOR_TOKEN_POINT, DASHBOARD_HABIT_FALLBACK_POINT_COLOR)
  const splitLineColor = getCssVariable(
    DASHBOARD_HABIT_COLOR_TOKEN_SPLIT_LINE,
    DASHBOARD_HABIT_FALLBACK_SPLIT_LINE_COLOR
  )
  const labels = normalizedHabitData.value.map((item) => item.day)
  const values = normalizedHabitData.value.map((item) => item.count)

  return {
    tooltip: {
      trigger: 'item',
      formatter: () => {
        return normalizedHabitData.value
          .map((item) => `${item.day}: ${item.count} ${DASHBOARD_HABIT_TOOLTIP_VISITS_LABEL}`)
          .join('<br/>')
      }
    },
    radar: {
      radius: DASHBOARD_HABIT_RADAR_RADIUS,
      splitNumber: DASHBOARD_HABIT_RADAR_SPLIT_NUMBER,
      indicator: labels.map((label) => ({ name: label, max: radarMaxValue.value })),
      splitLine: {
        lineStyle: {
          color: splitLineColor
        }
      },
      axisLine: {
        lineStyle: {
          color: splitLineColor
        }
      }
    },
    series: [
      {
        type: 'radar',
        symbolSize: DASHBOARD_HABIT_RADAR_SYMBOL_SIZE,
        lineStyle: {
          width: DASHBOARD_HABIT_RADAR_LINE_WIDTH,
          color: lineColor
        },
        itemStyle: {
          color: pointColor
        },
        areaStyle: {
          color: fillColor
        },
        data: [
          {
            value: values,
            name: DASHBOARD_HABIT_TITLE
          }
        ]
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
  () => [props.habitData, props.viewState] as const,
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

.dashboard-habit {
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

  &__canvas {
    width: 100%;
  }
}
</style>
