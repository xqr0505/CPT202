<template>
  <div class="booking-requests-page">
    <div class="page-header">
      <h1 class="page-title">Booking Requests</h1>
      <p class="page-subtitle">
        Review pending requests, make decisions, and check your previous responses.
      </p>
    </div>

    <el-tabs v-model="activeTab" class="request-tabs">
      <el-tab-pane name="pending">
        <template #label>
          <span class="tab-label">
            Pending Requests
            <el-badge
              v-if="pendingRequests.length > 0"
              :value="pendingRequests.length"
              :type="urgentRequestCount > 0 ? 'danger' : 'primary'"
            />
          </span>
        </template>
        <div v-if="loadingPending" class="loading-container">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>Loading pending requests...</span>
        </div>

        <el-card v-else class="request-card" shadow="never">
          <template v-if="pendingRequests.length > 0">
            <div class="queue-summary">
              <div>
                <strong>{{ sortedPendingRequests.length }}</strong>
                pending request{{ sortedPendingRequests.length === 1 ? '' : 's' }}
              </div>
              <div>
                <strong>{{ soonRequestCount }}</strong>
                due within 6 hours
              </div>
            </div>
            <el-alert
              v-if="urgentRequestCount > 0"
              class="urgent-alert"
              :title="`${urgentRequestCount} request${urgentRequestCount === 1 ? '' : 's'} requires urgent attention`"
              type="error"
              show-icon
              :closable="false"
            />
            <el-table
              :data="sortedPendingRequests"
              :row-class-name="getPendingRowClassName"
              style="width: 100%"
            >
              <el-table-column prop="customerName" label="Customer Name" min-width="180" />
              <el-table-column label="Consultation Time" min-width="220">
                <template #default="{ row }">
                  <div class="time-block">
                    <div>{{ formatDateTime(row.requestedStartTime) }}</div>
                    <div class="time-separator">to</div>
                    <div>{{ formatDateTime(row.requestedEndTime) }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="topic" label="Topic" min-width="180" />
              <el-table-column label="Submission Time" min-width="200">
                <template #default="{ row }">
                  {{ formatDateTime(row.submissionTime) }}
                </template>
              </el-table-column>
              <el-table-column label="Auto Reject At" min-width="200">
                <template #default="{ row }">
                  {{ formatDateTime(row.autoRejectAt) }}
                </template>
              </el-table-column>
              <el-table-column label="Time Left" min-width="150">
                <template #default="{ row }">
                  <div class="time-left-cell">
                    <el-tag :type="getTimeLeftTagType(row.autoRejectAt)" effect="light">
                      {{ formatTimeLeft(row.autoRejectAt) }}
                    </el-tag>
                    <span class="urgency-reason">{{ getUrgencyReason(row) }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="Action" min-width="120" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="openDetail(row.id)">
                    View
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>

          <el-empty
            v-else
            description="No pending booking requests assigned to you."
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane name="history">
        <template #label>
          <span class="tab-label">Decision History</span>
        </template>
        <div v-if="loadingHistory" class="loading-container">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>Loading decision history...</span>
        </div>

        <el-card v-else class="request-card" shadow="never">
          <template v-if="handledRequests.length > 0">
            <div class="history-toolbar">
              <el-radio-group v-model="historyFilter" size="small">
                <el-radio-button label="ALL">All</el-radio-button>
                <el-radio-button label="APPROVED">Approved</el-radio-button>
                <el-radio-button label="REJECTED">Rejected</el-radio-button>
                <el-radio-button label="AUTO_REJECTED">Auto rejected</el-radio-button>
              </el-radio-group>
            </div>
            <el-table :data="filteredHandledRequests" style="width: 100%">
              <el-table-column prop="customerName" label="Customer Name" min-width="180" />
              <el-table-column prop="topic" label="Topic" min-width="180" />
              <el-table-column label="Decision" min-width="130">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'APPROVED' ? 'success' : 'danger'">
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Decision Time" min-width="200">
                <template #default="{ row }">
                  {{ formatDateTime(row.decisionTime) }}
                </template>
              </el-table-column>
              <el-table-column label="Refund" min-width="190">
                <template #default="{ row }">
                  <div v-if="row.refundStatus" class="refund-cell">
                    <el-tag :type="getRefundTagType(row.refundStatus)" effect="light">
                      {{ row.refundStatus }}
                    </el-tag>
                    <span class="refund-amount">
                      {{ formatMoney(row.refundAmount) }}
                    </span>
                  </div>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="Action" min-width="120" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="openDetail(row.id)">
                    View
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>

          <el-empty
            v-else
            description="No handled booking requests yet."
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="showDetailDialog"
      title="Booking Request Details"
      width="680px"
    >
      <div v-if="detailLoading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>Loading request details...</span>
      </div>

      <div v-else-if="selectedDetail" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Customer Name">
            {{ selectedDetail.customerName }}
          </el-descriptions-item>
          <el-descriptions-item label="Consultation Time">
            {{ formatDateTime(selectedDetail.requestedStartTime) }}
            to
            {{ formatDateTime(selectedDetail.requestedEndTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="Topic">
            {{ selectedDetail.topic }}
          </el-descriptions-item>
          <el-descriptions-item label="Price">
            {{ formatMoney(selectedDetail.price) }}
          </el-descriptions-item>
          <el-descriptions-item label="Submission Time">
            {{ formatDateTime(selectedDetail.submissionTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="Customer Notes">
            {{ selectedDetail.customerNotes || 'No additional notes provided.' }}
          </el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag
              :type="
                selectedDetail.status === 'APPROVED'
                  ? 'success'
                  : selectedDetail.status === 'REJECTED'
                    ? 'danger'
                    : 'warning'
              "
            >
              {{ selectedDetail.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDetail.decisionTime" label="Decision Time">
            {{ formatDateTime(selectedDetail.decisionTime) }}
          </el-descriptions-item>
          <el-descriptions-item
            v-if="selectedDetail.rejectionReason"
            label="Rejection Reason"
          >
            {{ selectedDetail.rejectionReason }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDetail.refundStatus" label="Refund Status">
            <el-tag :type="getRefundTagType(selectedDetail.refundStatus)" effect="light">
              {{ selectedDetail.refundStatus }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDetail.refundStatus" label="Refund Amount">
            {{ formatMoney(selectedDetail.refundAmount) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDetail.refundStatus" label="Penalty Amount">
            {{ formatMoney(selectedDetail.penaltyAmount) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDetail.refundRule" label="Refund Rule">
            {{ selectedDetail.refundRule }}
          </el-descriptions-item>
        </el-descriptions>

        <el-timeline class="decision-timeline">
          <el-timeline-item
            v-for="item in decisionTimeline"
            :key="item.title"
            :timestamp="item.time"
            :type="item.type"
          >
            {{ item.title }}
          </el-timeline-item>
        </el-timeline>
      </div>

      <template #footer>
        <div v-if="selectedDetail?.status === 'PENDING'" class="detail-footer">
          <el-button @click="showDetailDialog = false">Close</el-button>
          <el-button type="success" :loading="processing" @click="confirmApprove">
            Approve
          </el-button>
          <el-button type="danger" :loading="processing" @click="openRejectDialog">
            Reject
          </el-button>
        </div>
        <div v-else class="detail-footer">
          <el-button @click="showDetailDialog = false">Close</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showRejectDialog"
      title="Reject Booking Request"
      width="560px"
    >
      <el-form label-position="top">
        <el-form-item label="Reason Template">
          <div class="reason-template-list">
            <el-button
              v-for="template in rejectionReasonTemplates"
              :key="template"
              size="small"
              @click="applyReasonTemplate(template)"
            >
              {{ template }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="Rejection Reason" required>
          <el-input
            v-model="rejectionReason"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="Please provide a clear rejection reason."
          />
        </el-form-item>
      </el-form>

      <div v-if="selectedDetail" class="reject-impact">
        <div class="impact-title">Refund Impact</div>
        <div class="impact-grid">
          <span>Refund Status</span>
          <strong>PENDING</strong>
          <span>Refund Amount</span>
          <strong>{{ formatMoney(selectedDetail.price) }}</strong>
          <span>Penalty Amount</span>
          <strong>{{ formatMoney(0) }}</strong>
          <span>Rule</span>
          <strong>SPECIALIST_REJECT_FULL_REFUND</strong>
        </div>
      </div>

      <template #footer>
        <el-button @click="showRejectDialog = false">Cancel</el-button>
        <el-button type="danger" :loading="processing" @click="handleReject">
          Submit Rejection
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import {
  approveBookingRequest,
  getBookingRequestDetail,
  getHandledBookingRequests,
  getPendingBookingRequests,
  rejectBookingRequest,
  type SpecialistBookingDetailVO,
  type SpecialistHandledBookingVO,
  type SpecialistPendingBookingVO
} from '@/api/booking'

const activeTab = ref('pending')
const historyFilter = ref<'ALL' | 'APPROVED' | 'REJECTED' | 'AUTO_REJECTED'>('ALL')

const loadingPending = ref(false)
const loadingHistory = ref(false)
const detailLoading = ref(false)
const processing = ref(false)

const pendingRequests = ref<SpecialistPendingBookingVO[]>([])
const handledRequests = ref<SpecialistHandledBookingVO[]>([])

const showDetailDialog = ref(false)
const showRejectDialog = ref(false)

const selectedDetail = ref<SpecialistBookingDetailVO | null>(null)
const rejectionReason = ref('')
const currentTime = ref(Date.now())
let countdownTimer: number | undefined

const rejectionReasonTemplates = [
  'Schedule conflict',
  'Patient condition not suitable',
  'Insufficient information',
  'Need reschedule',
  'Other'
]

type TimelineItemType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

interface DecisionTimelineItem {
  title: string
  time: string
  type: TimelineItemType
}

const sortedPendingRequests = computed(() => {
  return [...pendingRequests.value].sort((left, right) => {
    const leftMinutes = getTimeLeftMinutes(left.autoRejectAt)
    const rightMinutes = getTimeLeftMinutes(right.autoRejectAt)
    return (leftMinutes ?? Number.MAX_SAFE_INTEGER) - (rightMinutes ?? Number.MAX_SAFE_INTEGER)
  })
})

const urgentRequestCount = computed(() => {
  return pendingRequests.value.filter(request => {
    const minutes = getTimeLeftMinutes(request.autoRejectAt)
    return minutes !== null && minutes > 0 && minutes <= 60
  }).length
})

const soonRequestCount = computed(() => {
  return pendingRequests.value.filter(request => {
    const minutes = getTimeLeftMinutes(request.autoRejectAt)
    return minutes !== null && minutes > 0 && minutes <= 360
  }).length
})

const filteredHandledRequests = computed(() => {
  if (historyFilter.value === 'ALL') return handledRequests.value
  if (historyFilter.value === 'AUTO_REJECTED') {
    return handledRequests.value.filter(request =>
      request.status === 'REJECTED' && request.refundRule === 'SYSTEM_TIMEOUT_FULL_REFUND'
    )
  }
  return handledRequests.value.filter(request => request.status === historyFilter.value)
})

const decisionTimeline = computed(() => {
  if (!selectedDetail.value) return []

  const detail = selectedDetail.value
  const items: DecisionTimelineItem[] = [
    {
      title: 'Booking created',
      time: formatDateTime(detail.submissionTime),
      type: 'primary'
    }
  ]

  if (detail.status === 'PENDING') {
    items.push({
      title: 'Waiting for specialist decision',
      time: '',
      type: 'warning'
    })
    return items
  }

  items.push({
    title: detail.status === 'APPROVED' ? 'Approved by specialist' : getRejectedTimelineTitle(detail),
    time: formatDateTime(detail.decisionTime),
    type: detail.status === 'APPROVED' ? 'success' : 'danger'
  })

  if (detail.refundStatus) {
    items.push({
      title: `Refund ${detail.refundStatus.toLowerCase()}`,
      time: '',
      type: getRefundTimelineType(detail.refundStatus)
    })
  }

  return items
})

function formatDateTime(value?: string): string {
  if (!value) return '-'
  const date = new Date(value)
  return date.toLocaleString('en-GB', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getTimeLeftMinutes(value?: string): number | null {
  if (!value) return null
  const expiresAt = new Date(value).getTime()
  if (Number.isNaN(expiresAt)) return null
  return Math.ceil((expiresAt - currentTime.value) / 60000)
}

function formatTimeLeft(value?: string): string {
  const minutes = getTimeLeftMinutes(value)
  if (minutes === null) return '-'
  if (minutes <= 0) return 'Expired'

  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  if (hours <= 0) return `${remainingMinutes}m`
  if (remainingMinutes === 0) return `${hours}h`
  return `${hours}h ${remainingMinutes}m`
}

function getTimeLeftTagType(value?: string): 'success' | 'warning' | 'danger' | 'info' {
  const minutes = getTimeLeftMinutes(value)
  if (minutes === null) return 'info'
  if (minutes <= 60) return 'danger'
  if (minutes <= 360) return 'warning'
  return 'info'
}

function getPendingRowClassName({ row }: { row: SpecialistPendingBookingVO }): string {
  const minutes = getTimeLeftMinutes(row.autoRejectAt)
  if (minutes === null) return ''
  if (minutes <= 60) return 'pending-row--urgent'
  if (minutes <= 360) return 'pending-row--soon'
  return ''
}

function formatMoney(value?: number): string {
  const amount = Number(value ?? 0)
  return `CNY ${amount.toFixed(2)}`
}

function getRefundTagType(status?: string): 'success' | 'warning' | 'info' | 'danger' {
  if (status === 'PROCESSED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'PENALTY_APPLIED') return 'danger'
  return 'info'
}

function getRefundTimelineType(status?: string): TimelineItemType {
  return getRefundTagType(status)
}

function getUrgencyReason(row: SpecialistPendingBookingVO): string {
  const minutes = getTimeLeftMinutes(row.autoRejectAt)
  if (minutes === null) return 'No timeout window'
  if (minutes <= 0) return 'Timeout reached'
  if (minutes <= 60) return `Auto rejects in ${formatTimeLeft(row.autoRejectAt)}`
  if (minutes <= 360) return `Due within ${Math.ceil(minutes / 60)} hours`
  return 'Standard queue'
}

function getRejectedTimelineTitle(detail: SpecialistBookingDetailVO): string {
  return detail.refundRule === 'SYSTEM_TIMEOUT_FULL_REFUND'
    ? 'Auto rejected by timeout'
    : 'Rejected by specialist'
}

function applyReasonTemplate(template: string) {
  rejectionReason.value = template === 'Other' ? '' : template
}

function notifyApprovalQueueChanged() {
  window.dispatchEvent(new CustomEvent('specialist-approval-queue-updated'))
}

async function fetchPendingRequests() {
  loadingPending.value = true
  try {
    pendingRequests.value = await getPendingBookingRequests()
  } catch (error) {
    console.error('Failed to fetch pending booking requests:', error)
    ElMessage.error('Failed to load pending booking requests')
  } finally {
    loadingPending.value = false
  }
}

async function fetchHandledRequests() {
  loadingHistory.value = true
  try {
    handledRequests.value = await getHandledBookingRequests()
  } catch (error) {
    console.error('Failed to fetch handled booking requests:', error)
    ElMessage.error('Failed to load decision history')
  } finally {
    loadingHistory.value = false
  }
}

async function refreshAll() {
  await Promise.all([fetchPendingRequests(), fetchHandledRequests()])
}

async function openDetail(id: number) {
  showDetailDialog.value = true
  detailLoading.value = true
  try {
    selectedDetail.value = await getBookingRequestDetail(id)
  } catch (error) {
    console.error('Failed to fetch booking request detail:', error)
    ElMessage.error('Failed to load booking request detail')
    showDetailDialog.value = false
  } finally {
    detailLoading.value = false
  }
}

async function confirmApprove() {
  if (!selectedDetail.value) return

  const detail = selectedDetail.value
  try {
    await ElMessageBox.confirm(
      `${detail.customerName} - ${detail.topic}\n${formatDateTime(detail.requestedStartTime)} to ${formatDateTime(detail.requestedEndTime)}`,
      'Approve this booking?',
      {
        confirmButtonText: 'Approve',
        cancelButtonText: 'Cancel',
        type: 'success'
      }
    )
    await handleApprove()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('Approval confirmation failed:', error)
    }
  }
}

async function handleApprove() {
  if (!selectedDetail.value) return

  processing.value = true
  try {
    await approveBookingRequest(selectedDetail.value.id)
    ElMessage.success('Booking request approved successfully')
    showDetailDialog.value = false
    selectedDetail.value = null
    await refreshAll()
    notifyApprovalQueueChanged()
    activeTab.value = 'history'
  } catch (error) {
    console.error('Failed to approve booking request:', error)
    ElMessage.error('Failed to approve booking request')
  } finally {
    processing.value = false
  }
}

function openRejectDialog() {
  rejectionReason.value = ''
  showRejectDialog.value = true
}

async function handleReject() {
  if (!selectedDetail.value) return

  if (!rejectionReason.value.trim()) {
    ElMessage.warning('Rejection reason is required')
    return
  }

  processing.value = true
  try {
    await rejectBookingRequest(selectedDetail.value.id, rejectionReason.value.trim())
    ElMessage.success('Booking request rejected successfully')
    showRejectDialog.value = false
    showDetailDialog.value = false
    selectedDetail.value = null
    rejectionReason.value = ''
    await refreshAll()
    notifyApprovalQueueChanged()
    activeTab.value = 'history'
  } catch (error) {
    console.error('Failed to reject booking request:', error)
    ElMessage.error('Failed to reject booking request')
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  countdownTimer = window.setInterval(() => {
    currentTime.value = Date.now()
  }, 60000)
  refreshAll()
})

onBeforeUnmount(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
})
</script>

<style scoped lang="scss">
.booking-requests-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.request-tabs {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.request-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.queue-summary {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  color: var(--color-text-secondary);
  font-size: 13px;
}

.queue-summary > div {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
}

.urgent-alert {
  margin-bottom: var(--space-4);
}

.time-left-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.urgency-reason {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.history-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--space-4);
}

.refund-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.refund-amount {
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}

:deep(.pending-row--urgent) {
  background: rgba(245, 108, 108, 0.1);
}

:deep(.pending-row--soon) {
  background: rgba(230, 162, 60, 0.1);
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  min-height: 220px;
  color: var(--color-text-secondary);
}

.time-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.time-separator {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.decision-timeline {
  margin-top: var(--space-2);
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.reason-template-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.reject-impact {
  margin-top: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
}

.impact-title {
  margin-bottom: var(--space-3);
  font-weight: 700;
  color: var(--color-text-primary);
}

.impact-grid {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(0, 1.4fr);
  gap: var(--space-2) var(--space-3);
  color: var(--color-text-secondary);
  font-size: 13px;
}

.impact-grid strong {
  color: var(--color-text-primary);
  font-weight: 600;
  word-break: break-word;
}
</style>
