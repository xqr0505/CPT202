import type { CustomerDashboardSummary } from '@/api/booking'

export const DASHBOARD_PAGE_TITLE = 'Dashboard'

export const DASHBOARD_SECTION_TITLE = 'Summary'
export const DASHBOARD_SECTION_DESCRIPTION =
  'View all-time metrics or filter by date range.'

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
export const DASHBOARD_EMPTY_EXPERTS_TEXT = 'No experts consulted yet'

export const DASHBOARD_CURRENCY_PREFIX = '$'
export const DASHBOARD_AMOUNT_PRECISION = 2

export const DASHBOARD_INITIAL_SKELETON_CARD_COUNT = 4
export const DASHBOARD_EXPERT_INITIAL_LIMIT = 2
export const DASHBOARD_EXPERT_AVATAR_SIZE = 44

export const DASHBOARD_EMPTY_NUMERIC_VALUE = 0

export const DASHBOARD_VIEW_STATES = {
  loading: 'loading',
  ready: 'ready',
  error: 'error'
} as const

export type DashboardViewState =
  typeof DASHBOARD_VIEW_STATES[keyof typeof DASHBOARD_VIEW_STATES]

export type DashboardDateRange = [string, string]
export type DashboardDateRangeNullable = DashboardDateRange | null

export const createEmptyDashboardSummary = (): CustomerDashboardSummary => ({
  totalCompletedAppointments: DASHBOARD_EMPTY_NUMERIC_VALUE,
  totalAmountSpent: DASHBOARD_EMPTY_NUMERIC_VALUE,
  totalConsultationHours: DASHBOARD_EMPTY_NUMERIC_VALUE,
  consultedExperts: []
})
