<template>
  <section class="page-card password-page">
    <div class="page-header">
      <p class="page-tag">Customer</p>
      <h1 class="page-title">Change Password</h1>
      <p class="page-text">
        Update your password to keep your account secure during normal account use.
      </p>
    </div>

    <div v-if="viewState === 'loading'" class="password-panel">
      <div class="password-panel__header">
        <div>
          <h2 class="password-panel__title">Loading password settings</h2>
          <p class="password-panel__subtitle">
            We are preparing the password change form.
          </p>
        </div>
      </div>

      <div class="password-panel__body loading-layout">
        <el-skeleton animated>
          <template #template>
            <div class="skeleton-grid">
              <div v-for="index in 3" :key="index" class="skeleton-card">
                <el-skeleton-item variant="text" class="skeleton-label" />
                <el-skeleton-item variant="h3" class="skeleton-value" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <div v-else-if="viewState === 'error'" class="state-card">
      <h2 class="state-title">Unable to load password settings</h2>
      <p class="state-text">{{ loadErrorMessage }}</p>
      <div class="state-actions">
        <CustomButton @click="goToProfile">Back to Profile</CustomButton>
        <CustomButton type="primary" @click="loadPage">Retry</CustomButton>
      </div>
    </div>

    <div v-else class="password-panel">
      <div class="password-panel__header">
        <div>
          <h2 class="password-panel__title">Password Change</h2>
          <p class="password-panel__subtitle">
            Enter your current password, then choose and confirm a new one.
          </p>
        </div>

        <div class="password-panel__actions">
          <CustomButton @click="goToProfile">Back to Profile</CustomButton>
          <CustomButton type="primary" :loading="isSaving" @click="savePassword">
            Change Password
          </CustomButton>
        </div>
      </div>

      <div class="password-panel__body">
        <el-form label-position="top" class="password-form">
          <div class="form-grid">
            <el-form-item label="Current Password" :error="formErrors.currentPassword">
              <el-input
                v-model="form.currentPassword"
                type="password"
                show-password
                placeholder="Enter your current password"
                @blur="validateField('currentPassword')"
                @input="handleFieldInput('currentPassword')"
              />
            </el-form-item>

            <el-form-item label="New Password" :error="formErrors.newPassword">
              <el-input
                v-model="form.newPassword"
                type="password"
                show-password
                placeholder="Enter your new password"
                @blur="validateField('newPassword')"
                @input="handleFieldInput('newPassword')"
              />
            </el-form-item>

            <el-form-item label="Confirm New Password" :error="formErrors.confirmationPassword">
              <el-input
                v-model="form.confirmationPassword"
                type="password"
                show-password
                placeholder="Confirm your new password"
                @blur="validateField('confirmationPassword')"
                @input="handleFieldInput('confirmationPassword')"
              />
            </el-form-item>
          </div>
        </el-form>

        <p class="password-hint">
          Your new password must be at least 8 characters long.
        </p>

        <p v-if="saveErrorMessage" class="feedback-message feedback-message--error">
          {{ saveErrorMessage }}
        </p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import CustomButton from '@/components/common/CustomButton.vue'

defineOptions({ name: 'CustomerChangePassword' })

type ViewState = 'loading' | 'ready' | 'error'
type ChangePasswordField = keyof ChangePasswordPayload
type ChangePasswordFieldErrors = Partial<Record<ChangePasswordField, string>>

interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
  confirmationPassword: string
}

const USER_PASSWORD_STORAGE_KEY = 'mock-user-password'
const DEFAULT_PASSWORD = 'Password123'
const MIN_PASSWORD_LENGTH = 8

const wait = async (delay = 250): Promise<void> => {
  await new Promise(resolve => window.setTimeout(resolve, delay))
}

const readStoredPassword = (): string => {
  return localStorage.getItem(USER_PASSWORD_STORAGE_KEY) || DEFAULT_PASSWORD
}

const writeStoredPassword = (password: string): void => {
  localStorage.setItem(USER_PASSWORD_STORAGE_KEY, password)
}

const validateChangePasswordPayload = (
  payload: ChangePasswordPayload
): ChangePasswordFieldErrors => {
  const normalizedPayload = {
    currentPassword: payload.currentPassword.trim(),
    newPassword: payload.newPassword.trim(),
    confirmationPassword: payload.confirmationPassword.trim()
  }
  const fieldErrors: ChangePasswordFieldErrors = {}

  if (!normalizedPayload.currentPassword) {
    fieldErrors.currentPassword = 'Current password is required.'
  }

  if (!normalizedPayload.newPassword) {
    fieldErrors.newPassword = 'New password is required.'
  } else if (normalizedPayload.newPassword.length < MIN_PASSWORD_LENGTH) {
    fieldErrors.newPassword = `New password must be at least ${MIN_PASSWORD_LENGTH} characters long.`
  }

  if (!normalizedPayload.confirmationPassword) {
    fieldErrors.confirmationPassword = 'Confirmation password is required.'
  } else if (
    normalizedPayload.newPassword &&
    normalizedPayload.confirmationPassword !== normalizedPayload.newPassword
  ) {
    fieldErrors.confirmationPassword = 'Confirmation password must match the new password.'
  }

  return fieldErrors
}

const router = useRouter()

const viewState = ref<ViewState>('loading')
const isSaving = ref(false)
const loadErrorMessage = ref('We could not load password settings.')
const saveErrorMessage = ref('')

const form = reactive<ChangePasswordPayload>({
  currentPassword: '',
  newPassword: '',
  confirmationPassword: ''
})

const formErrors = reactive<Record<ChangePasswordField, string>>({
  currentPassword: '',
  newPassword: '',
  confirmationPassword: ''
})

const applyFieldErrors = (fieldErrors: ChangePasswordFieldErrors = {}): void => {
  formErrors.currentPassword = fieldErrors.currentPassword || ''
  formErrors.newPassword = fieldErrors.newPassword || ''
  formErrors.confirmationPassword = fieldErrors.confirmationPassword || ''
}

const resetForm = (): void => {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmationPassword = ''
  applyFieldErrors()
  saveErrorMessage.value = ''
}

const loadPage = async (): Promise<void> => {
  viewState.value = 'loading'
  loadErrorMessage.value = 'We could not load password settings.'

  try {
    await wait()
    viewState.value = 'ready'
  } catch (error) {
    loadErrorMessage.value =
      error instanceof Error && error.message
        ? error.message
        : 'We could not load password settings.'
    viewState.value = 'error'
  }
}

const validateField = (field: ChangePasswordField): boolean => {
  const fieldErrors = validateChangePasswordPayload(form)
  formErrors[field] = fieldErrors[field] || ''
  return !formErrors[field]
}

const handleFieldInput = (field: ChangePasswordField): void => {
  if (formErrors[field]) {
    validateField(field)
  }
}

const validateForm = (): boolean => {
  const fieldErrors = validateChangePasswordPayload(form)
  applyFieldErrors(fieldErrors)
  return !Object.values(fieldErrors).some(Boolean)
}

const savePassword = async (): Promise<void> => {
  saveErrorMessage.value = ''

  if (!validateForm()) {
    return
  }

  isSaving.value = true

  try {
    await wait(250)

    const fieldErrors = validateChangePasswordPayload({
      currentPassword: form.currentPassword,
      newPassword: form.newPassword,
      confirmationPassword: form.confirmationPassword
    })

    if (form.currentPassword.trim() && form.currentPassword.trim() !== readStoredPassword()) {
      fieldErrors.currentPassword = 'Current password is incorrect.'
    }

    if (Object.values(fieldErrors).some(Boolean)) {
      applyFieldErrors(fieldErrors)
      saveErrorMessage.value = 'Please correct the highlighted fields and try again.'
      return
    }

    writeStoredPassword(form.newPassword.trim())
    resetForm()
    ElMessage.success('The password has been changed successfully.')
  } catch (error) {
    saveErrorMessage.value =
      error instanceof Error && error.message
        ? error.message
        : 'Unable to update your password right now.'
    ElMessage.error(saveErrorMessage.value)
  } finally {
    isSaving.value = false
  }
}

const goToProfile = (): void => {
  void router.push('/customer/profile')
}

onMounted(() => {
  void loadPage()
})
</script>

<style scoped lang="scss">
.page-card {
  padding: var(--space-6);
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 16px var(--color-shadow);
}

.password-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.page-tag {
  margin: 0;
  color: var(--color-primary);
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 2rem;
}

.page-text {
  margin: 0;
  max-width: 48rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.password-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-bg-surface);
}

.password-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-muted);
}

.password-panel__title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1.25rem;
}

.password-panel__subtitle {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.password-panel__actions,
.state-actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.password-panel__body {
  padding: var(--space-6);
}

.loading-layout {
  padding-top: var(--space-6);
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.skeleton-card {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.skeleton-label,
.skeleton-value {
  width: 100%;
}

.skeleton-value {
  margin-top: var(--space-3);
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--space-4);
}

.password-hint {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.feedback-message {
  margin: 0;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  line-height: 1.6;
}

.feedback-message--error {
  color: var(--color-danger);
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border);
}

.state-card {
  padding: var(--space-8);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-surface);
  box-shadow: 0 4px 16px var(--color-shadow);
  text-align: center;
}

.state-title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1.25rem;
}

.state-text {
  margin: var(--space-3) auto 0;
  max-width: 36rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.state-actions {
  justify-content: center;
  margin-top: var(--space-4);
}

@media (max-width: 768px) {
  .password-panel__header {
    flex-direction: column;
  }

  .password-panel__actions {
    width: 100%;
  }
}
</style>
