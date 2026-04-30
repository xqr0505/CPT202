<template>
  <section class="search-page">
    <header class="hero card">
      <div>
        <h1>Find the right specialist for your next consultation</h1>
        <p class="hero-copy">
          Search by name, category, price, and date availability. The list keeps your filter state in the URL so detail-page back navigation stays intact.
        </p>
      </div>
    </header>

    <SpecialistFilterBar
      v-model="filters"
      :categories="categories"
      @search="handleSearch"
      @reset="handleReset"
    />

    <section class="results-head">
      <div>
        <h2>Search results</h2>
        <p class="results-copy">
          {{ resultSummary }}
        </p>
      </div>
    </section>

    <section v-if="loading" class="loading-state card">
      <el-skeleton animated :rows="6" />
    </section>

    <EmptyPlaceholder
      v-else-if="!specialists.length"
      description="No specialists matched your current filters."
    />

    <section v-else class="results-grid">
      <SpecialistCard
        v-for="specialist in specialists"
        :key="specialist.id"
        :specialist="specialist"
        :selected-date="filters.date"
        @view="handleViewDetail"
      />
    </section>

    <div v-if="total > pageSize" class="pagination-wrap card">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchSpecialistCategories, fetchSpecialists } from '@/api/specialist'
import { getUser } from '@/api/request'
import EmptyPlaceholder from '@/components/business/EmptyPlaceholder.vue'
import SpecialistCard from '@/components/business/SpecialistCard.vue'
import SpecialistFilterBar from '@/components/business/SpecialistFilterBar.vue'
import {
  SPECIALIST_SORT_OPTIONS,
  type SpecialistCategory,
  type SpecialistSearchForm,
  type SpecialistSearchParams,
  type SpecialistSummary,
} from '@/types/specialist'

defineOptions({ name: 'SpecialistSearch' })

const DEFAULT_PAGE_SIZE = 12
const AI_BOOKING_CONTEXT_STORAGE_KEY = 'ai.booking.context'
const DEFAULT_FILTERS: SpecialistSearchForm = {
  keyword: '',
  categoryId: null,
  date: '',
  sortBy: SPECIALIST_SORT_OPTIONS.RECOMMENDED,
}

interface StoredSessionUser {
  userId?: number | string | null
  id?: number | string | null
}

const route = useRoute()
const router = useRouter()

const filters = ref<SpecialistSearchForm>({ ...DEFAULT_FILTERS })
const specialists = ref<SpecialistSummary[]>([])
const categories = ref<SpecialistCategory[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)

const resultSummary = computed(() => {
  if (!total.value) {
    return 'Adjust the filters and run a search to discover specialists.'
  }

  const dateNote = filters.value.date ? ` available on ${filters.value.date}` : ''
  return `${total.value} specialist${total.value > 1 ? 's' : ''} found${dateNote}.`
})

const parsePositiveInteger = (value: unknown, fallback: number) => {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

const resolveCurrentUserId = (): number | null => {
  const storedUser = getUser() as StoredSessionUser | null
  const rawUserId = storedUser?.userId ?? storedUser?.id
  const parsedUserId = Number(rawUserId)
  if (!Number.isFinite(parsedUserId) || parsedUserId <= 0) {
    return null
  }
  return Math.trunc(parsedUserId)
}

const resolveAiBookingContextStorageKey = (): string => {
  const currentUserId = resolveCurrentUserId()
  return currentUserId
    ? `${AI_BOOKING_CONTEXT_STORAGE_KEY}:${currentUserId}`
    : AI_BOOKING_CONTEXT_STORAGE_KEY
}

const syncAiSearchPageContext = () => {
  if (typeof window === 'undefined') {
    return
  }

  const contextStorageKey = resolveAiBookingContextStorageKey()
  window.sessionStorage.setItem(
    contextStorageKey,
    JSON.stringify({
      selectedDate: filters.value.date || undefined,
      visibleSpecialists: specialists.value.map(item => ({
        id: item.id,
        name: item.name,
        consultationFee: item.consultationFee,
      })),
    })
  )
  if (contextStorageKey !== AI_BOOKING_CONTEXT_STORAGE_KEY) {
    window.sessionStorage.removeItem(AI_BOOKING_CONTEXT_STORAGE_KEY)
  }
}

const normalizeCategoryId = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') {
    return null
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const isPastDateString = (value: string): boolean => {
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) {
    return false
  }

  const selectedDate = new Date(year, month - 1, day)
  selectedDate.setHours(0, 0, 0, 0)

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return selectedDate < today
}

const normalizeSearchDate = (value: unknown): string => {
  if (typeof value !== 'string' || !value) {
    return ''
  }
  return isPastDateString(value) ? '' : value
}

const parseRouteState = () => {
  const query = route.query
  const parsedSortBy =
    typeof query.sortBy === 'string' &&
    Object.values(SPECIALIST_SORT_OPTIONS).includes(query.sortBy as SpecialistSearchForm['sortBy'])
      ? (query.sortBy as SpecialistSearchForm['sortBy'])
      : SPECIALIST_SORT_OPTIONS.RECOMMENDED

  filters.value = {
    keyword: typeof query.keyword === 'string' ? query.keyword : '',
    categoryId: normalizeCategoryId(query.categoryId),
    date: normalizeSearchDate(query.date),
    sortBy: parsedSortBy,
  }
  currentPage.value = parsePositiveInteger(query.pageNo, 1)
  pageSize.value = parsePositiveInteger(query.pageSize, DEFAULT_PAGE_SIZE)
}

const buildSearchQuery = (pageNo: number) => {
  const query: Record<string, string> = {
    pageNo: String(pageNo),
    pageSize: String(pageSize.value),
  }
  const categoryId = normalizeCategoryId(filters.value.categoryId)

  if (filters.value.keyword.trim()) {
    query.keyword = filters.value.keyword.trim()
  }
  if (categoryId !== null) {
    query.categoryId = String(categoryId)
  }
  const normalizedDate = normalizeSearchDate(filters.value.date)
  if (normalizedDate) {
    query.date = normalizedDate
  }
  if (filters.value.sortBy !== SPECIALIST_SORT_OPTIONS.RECOMMENDED) {
    query.sortBy = filters.value.sortBy
  }

  return query
}

const loadCategories = async () => {
  categories.value = await fetchSpecialistCategories()
}

const loadSpecialists = async () => {
  loading.value = true

  try {
    const categoryId = normalizeCategoryId(filters.value.categoryId)
    const params: SpecialistSearchParams = {
      keyword: filters.value.keyword || undefined,
      categoryId: categoryId ?? undefined,
      date: filters.value.date || undefined,
      sortBy: filters.value.sortBy,
      pageNo: currentPage.value,
      pageSize: pageSize.value,
    }
    const result = await fetchSpecialists(params)
    specialists.value = result.list
    const parsedTotal = Number((result as unknown as { total?: unknown })?.total)
    total.value = Number.isFinite(parsedTotal) ? parsedTotal : 0
    syncAiSearchPageContext()
  } finally {
    loading.value = false
  }
}

const pushSearchState = (pageNo = 1) => {
  router.push({
    path: '/customer/search',
    query: buildSearchQuery(pageNo),
  })
}

const handleSearch = () => {
  pushSearchState(1)
}

const handleReset = () => {
  filters.value = { ...DEFAULT_FILTERS }
  pushSearchState(1)
}

const handlePageChange = (page: number) => {
  pushSearchState(page)
}

const handleViewDetail = (id: number) => {
  router.push({
    path: `/customer/specialists/${id}`,
    query: {
      from: route.fullPath,
      ...(filters.value.date ? { date: filters.value.date } : {}),
    },
  })
}

watch(
  () => route.query,
  async () => {
    parseRouteState()
    await loadSpecialists()
  },
  { immediate: true },
)

onMounted(async () => {
  syncAiSearchPageContext()
  await loadCategories()
})

watch(
  () => filters.value.date,
  () => {
    syncAiSearchPageContext()
  }
)
</script>

<style scoped lang="scss">
.search-page {
  display: grid;
  gap: var(--space-6);
}

.hero {
  padding: var(--space-8);
  background:
    radial-gradient(circle at top right, rgba(var(--color-primary-rgb), 0.18), transparent 30%),
    linear-gradient(135deg, var(--color-bg-surface), rgba(var(--color-primary-rgb), 0.08));
}

.eyebrow {
  margin: 0 0 var(--space-3);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero h1,
.results-head h2 {
  margin: 0;
  color: var(--color-text-primary);
}

.hero-copy,
.results-copy {
  margin: var(--space-3) 0 0;
  max-width: 72ch;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.results-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
}

.results-grid {
  display: grid;
  gap: var(--space-4);
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
}

.loading-state,
.pagination-wrap {
  padding: var(--space-6);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
}
</style>
