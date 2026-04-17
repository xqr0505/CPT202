<template>
  <div class="booking-requests-page">
    <div class="page-header">
      <h1 class="page-title">Booking Requests</h1>
      <p class="page-subtitle">
        Review pending requests, make decisions, and check your previous responses.
      </p>
    </div>

    <el-tabs v-model="activeTab" class="request-tabs">
      <el-tab-pane label="Pending Requests" name="pending">
        <div v-if="loadingPending" class="loading-container">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>Loading pending requests...</span>
        </div>

        <el-card v-else class="request-card" shadow="never">
          <template v-if="pendingRequests.length > 0">
            <el-table :data="pendingRequests" style="width: 100%">
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

      <el-tab-pane label="Decision History" name="history">
        <div v-if="loadingHistory" class="loading-container">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>Loading decision history...</span>
        </div>

        <el-card v-else class="request-card" shadow="never">
          <template v-if="handledRequests.length > 0">
            <el-table :data="handledRequests" style="width: 100%">
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
        </el-descriptions>
      </div>

      <template #footer>
        <div v-if="selectedDetail?.status === 'PENDING'" class="detail-footer">
          <el-button @click="showDetailDialog = false">Close</el-button>
          <el-button type="success" :loading="processing" @click="handleApprove">
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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
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

async function handleApprove() {
  if (!selectedDetail.value) return

  processing.value = true
  try {
    await approveBookingRequest(selectedDetail.value.id)
    ElMessage.success('Booking request approved successfully')
    showDetailDialog.value = false
    selectedDetail.value = null
    await refreshAll()
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
    activeTab.value = 'history'
  } catch (error) {
    console.error('Failed to reject booking request:', error)
    ElMessage.error('Failed to reject booking request')
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  refreshAll()
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

.detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}
</style>