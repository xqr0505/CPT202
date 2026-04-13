import type { DashboardStatistics } from '@/api/booking'

export const DASHBOARD_PAGE_TITLE = 'Dashboard'

export const DASHBOARD_SECTION_TITLE = 'Summary'
export const DASHBOARD_SECTION_DESCRIPTION =
  'View all-time metrics or filter by date range.'
export const DASHBOARD_TREND_TITLE = 'Consultation Activity Trend'
export const DASHBOARD_TREND_DESCRIPTION =
  'Track your consultation frequency over time.'

export const DASHBOARD_DATE_PICKER_TYPE = 'daterange'
export const DASHBOARD_DATE_VALUE_FORMAT = 'YYYY-MM-DD'
export const DASHBOARD_DATE_RANGE_SEPARATOR = 'To'
export const DASHBOARD_DATE_START_PLACEHOLDER = 'Start date'
export const DASHBOARD_DATE_END_PLACEHOLDER = 'End date'
export const DASHBOARD_DATE_FILTER_HINT = 'Date filter is optional'


export const DASHBOARD_STATISTIC_LABELS = {
  totalCompletedAppointments: 'Completed Appointments',
  totalAmountSpent: 'Amount Spent',
  totalConsultationHours: 'Consultation Hours',
  consultedExperts: 'My Consulted Experts'
} as const

export const DASHBOARD_RETRY_LABEL = 'Retry'
export const DASHBOARD_ERROR_TITLE = 'Unable to load consultation summary'
export const DASHBOARD_ERROR_MESSAGE =
  'Please try again or adjust the selected date range.'
export const DASHBOARD_CHART_EMPTY_TEXT =
  'No completed consultations in this period.'
export const DASHBOARD_EMPTY_EXPERTS_TEXT = 'No experts consulted yet'

export const DASHBOARD_CURRENCY_PREFIX = '$'
export const DASHBOARD_AMOUNT_PRECISION = 2

export const DASHBOARD_INITIAL_SKELETON_CARD_COUNT = 4
export const DASHBOARD_EXPERT_INITIAL_LIMIT = 2
export const DASHBOARD_EXPERT_AVATAR_SIZE = 44

export const DASHBOARD_EMPTY_NUMERIC_VALUE = 0
export const DASHBOARD_CHART_HEIGHT = 320
export const DASHBOARD_CHART_LOADING_ROWS = 6
export const DASHBOARD_CHART_TOOLTIP_APPOINTMENT_LABEL_SINGULAR = 'Appointment'
export const DASHBOARD_CHART_TOOLTIP_APPOINTMENT_LABEL_PLURAL = 'Appointments'
export const DASHBOARD_CHART_TOOLTIP_HOUR_LABEL_SINGULAR = 'Hour'
export const DASHBOARD_CHART_TOOLTIP_HOUR_LABEL_PLURAL = 'Hours'
export const DASHBOARD_CHART_TOOLTIP_HOUR_PRECISION = 2
export const DASHBOARD_CHART_LOCALE = 'en-US'
export const DASHBOARD_DATE_LABEL_MONTH_PATTERN = /^\d{4}-\d{2}$/
export const DASHBOARD_DATE_LABEL_DAY_PATTERN = /^\d{2}-\d{2}$/
export const DASHBOARD_LINE_SMOOTH = true
export const DASHBOARD_LINE_WIDTH = 3
export const DASHBOARD_LINE_SYMBOL_SIZE = 8
export const DASHBOARD_LINE_AREA_OPACITY = 1
export const DASHBOARD_CHART_GRID_TOP = 24
export const DASHBOARD_CHART_GRID_RIGHT = 24
export const DASHBOARD_CHART_GRID_BOTTOM = 24
export const DASHBOARD_CHART_GRID_LEFT = 12
export const DASHBOARD_CHART_COLOR_TOKEN_LINE = '--color-dashboard-trend-line'
export const DASHBOARD_CHART_COLOR_TOKEN_FILL = '--color-dashboard-trend-fill-start'
export const DASHBOARD_CHART_FALLBACK_LINE_COLOR = '#409EFF'
export const DASHBOARD_CHART_FALLBACK_FILL_COLOR = 'rgba(64, 158, 255, 0.24)'
export const DASHBOARD_CHART_TRANSPARENT_COLOR = 'rgba(255, 255, 255, 0)'

export const DASHBOARD_MONTH_LABEL_FORMAT: Intl.DateTimeFormatOptions = {
  month: 'long',
  year: 'numeric'
}

export const DASHBOARD_DAY_LABEL_FORMAT: Intl.DateTimeFormatOptions = {
  month: 'short',
  day: 'numeric',
  year: 'numeric'
}

export const DASHBOARD_HOUR_VALUE_FORMAT: Intl.NumberFormatOptions = {
  minimumFractionDigits: 0,
  maximumFractionDigits: DASHBOARD_CHART_TOOLTIP_HOUR_PRECISION
}

export const DASHBOARD_VIEW_STATES = {
  loading: 'loading',
  ready: 'ready',
  error: 'error'
} as const

export type DashboardViewState =
  typeof DASHBOARD_VIEW_STATES[keyof typeof DASHBOARD_VIEW_STATES]

export type DashboardDateRange = [string, string]
export type DashboardDateRangeNullable = DashboardDateRange | null

export const createEmptyDashboardStatistics = (): DashboardStatistics => ({
  totalCompletedAppointments: DASHBOARD_EMPTY_NUMERIC_VALUE,
  totalAmountSpent: DASHBOARD_EMPTY_NUMERIC_VALUE,
  totalConsultationHours: DASHBOARD_EMPTY_NUMERIC_VALUE,
  consultedExperts: [],
  trendData: [],
  categoryData: [],
  habitData: []
})
