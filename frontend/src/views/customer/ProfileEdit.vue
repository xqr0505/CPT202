<template>
  <section class="page-card profile-edit-page">
    <div class="page-header">
      <h1 class="page-title">Edit Profile</h1>
      <p class="page-text">
        Update your saved contact details and keep your account information accurate.
      </p>
    </div>

    <div v-if="viewState === 'loading'" class="profile-panel">
      <div class="profile-panel__header">
        <div>
          <h2 class="profile-panel__title">Loading profile</h2>
          <p class="profile-panel__subtitle">
            We are preparing your editable profile information.
          </p>
        </div>
      </div>

      <div class="profile-panel__body loading-layout">
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
      <h2 class="state-title">Unable to load your profile</h2>
      <p class="state-text">{{ loadErrorMessage }}</p>
      <div class="state-actions">
        <CustomButton @click="goToProfile">Back to Profile</CustomButton>
        <CustomButton type="primary" @click="loadProfile">Retry</CustomButton>
      </div>
    </div>

    <div v-else class="profile-panel">
      <div class="profile-panel__header">
        <div>
          <h2 class="profile-panel__title">Contact Details</h2>
          <p class="profile-panel__subtitle">
            Edit your name, email address, and phone number below.
          </p>
        </div>

        <div class="profile-panel__actions">
          <CustomButton @click="goToProfile">Cancel</CustomButton>
          <CustomButton type="primary" :loading="isSaving" @click="saveProfile">
            Save Changes
          </CustomButton>
        </div>
      </div>

      <div class="profile-panel__body">
        <el-form label-position="top" class="profile-form">
          <div class="form-grid">
            <el-form-item label="Name" :error="formErrors.fullName">
              <el-input
                v-model="form.fullName"
                placeholder="Enter your full name"
                @blur="validateField('fullName')"
                @input="handleFieldInput('fullName')"
              />
            </el-form-item>

            <el-form-item label="Email" :error="formErrors.email">
              <el-input
                v-model="form.email"
                placeholder="Enter your email address"
                @blur="validateField('email')"
                @input="handleFieldInput('email')"
              />
            </el-form-item>

            <el-form-item label="Phone" :error="formErrors.phoneNumber">
              <el-input
                v-model="form.phoneNumber"
                placeholder="Enter your phone number"
                @blur="validateField('phoneNumber')"
                @input="handleFieldInput('phoneNumber')"
              />
            </el-form-item>
          </div>
        </el-form>

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

defineOptions({ name: 'CustomerProfileEdit' })

type ViewState = 'loading' | 'ready' | 'error'
type UserProfileField = keyof UpdateUserProfilePayload
type UserProfileFieldErrors = Partial<Record<UserProfileField, string>>

interface UpdateUserProfilePayload {
  fullName: string
  email: string
  phoneNumber: string
}

const USER_PROFILE_STORAGE_KEY = 'mock-user-profile'

const DEFAULT_PROFILE: UpdateUserProfilePayload = {
  fullName: 'Emma Chen',
  email: 'emma.chen@example.com',
  phoneNumber: '+86 138 0013 8000'
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const phonePattern = /^\+?[0-9][0-9()\-\s]{6,19}$/

const wait = async (delay = 250): Promise<void> => {
  await new Promise(resolve => window.setTimeout(resolve, delay))
}

const readStoredProfile = (): UpdateUserProfilePayload => {
  const storedProfile = localStorage.getItem(USER_PROFILE_STORAGE_KEY)

  if (!storedProfile) {
    return { ...DEFAULT_PROFILE }
  }

  try {
    const parsedProfile = JSON.parse(storedProfile) as Partial<UpdateUserProfilePayload>
    return {
      ...DEFAULT_PROFILE,
      ...parsedProfile
    }
  } catch {
    return { ...DEFAULT_PROFILE }
  }
}

const writeStoredProfile = (profile: UpdateUserProfilePayload): void => {
  localStorage.setItem(USER_PROFILE_STORAGE_KEY, JSON.stringify(profile))
}

const validateUserProfilePayload = (
  payload: UpdateUserProfilePayload
): UserProfileFieldErrors => {
  const normalizedPayload = {
    fullName: payload.fullName.trim(),
    email: payload.email.trim(),
    phoneNumber: payload.phoneNumber.trim()
  }
  const fieldErrors: UserProfileFieldErrors = {}

  if (!normalizedPayload.fullName) {
    fieldErrors.fullName = 'Name is required.'
  }

  if (!normalizedPayload.email) {
    fieldErrors.email = 'Email is required.'
  } else if (!emailPattern.test(normalizedPayload.email)) {
    fieldErrors.email = 'Enter a valid email address.'
  }

  if (!normalizedPayload.phoneNumber) {
    fieldErrors.phoneNumber = 'Phone number is required.'
  } else if (!phonePattern.test(normalizedPayload.phoneNumber)) {
    fieldErrors.phoneNumber = 'Enter a valid phone number.'
  }

  return fieldErrors
}

const router = useRouter()

const viewState = ref<ViewState>('loading')
const isSaving = ref(false)
const loadErrorMessage = ref('We could not load your profile details.')
const saveErrorMessage = ref('')

const form = reactive<UpdateUserProfilePayload>({
  fullName: '',
  email: '',
  phoneNumber: ''
})

const formErrors = reactive<Record<UserProfileField, string>>({
  fullName: '',
  email: '',
  phoneNumber: ''
})

const applyFieldErrors = (fieldErrors: UserProfileFieldErrors = {}): void => {
  formErrors.fullName = fieldErrors.fullName || ''
  formErrors.email = fieldErrors.email || ''
  formErrors.phoneNumber = fieldErrors.phoneNumber || ''
}

const resetForm = (profile: UpdateUserProfilePayload): void => {
  form.fullName = profile.fullName
  form.email = profile.email
  form.phoneNumber = profile.phoneNumber
  applyFieldErrors()
  saveErrorMessage.value = ''
}

const loadProfile = async (): Promise<void> => {
  viewState.value = 'loading'
  loadErrorMessage.value = 'We could not load your profile details.'
  saveErrorMessage.value = ''

  try {
    await wait()
    const profile = readStoredProfile()
    resetForm(profile)
    viewState.value = 'ready'
  } catch (error) {
    loadErrorMessage.value =
      error instanceof Error && error.message
        ? error.message
        : 'We could not load your profile details.'
    viewState.value = 'error'
  }
}

const validateField = (field: UserProfileField): boolean => {
  const fieldErrors = validateUserProfilePayload(form)
  formErrors[field] = fieldErrors[field] || ''
  return !formErrors[field]
}

const handleFieldInput = (field: UserProfileField): void => {
  if (formErrors[field]) {
    validateField(field)
  }
}

const validateForm = (): boolean => {
  const fieldErrors = validateUserProfilePayload(form)
  applyFieldErrors(fieldErrors)
  return !Object.values(fieldErrors).some(Boolean)
}

const saveProfile = async (): Promise<void> => {
  saveErrorMessage.value = ''

  if (!validateForm()) {
    return
  }

  isSaving.value = true

  try {
    await wait(250)

    const updatedProfile: UpdateUserProfilePayload = {
      fullName: form.fullName,
      email: form.email,
      phoneNumber: form.phoneNumber
    }

    writeStoredProfile({
      fullName: updatedProfile.fullName.trim(),
      email: updatedProfile.email.trim(),
      phoneNumber: updatedProfile.phoneNumber.trim()
    })
    ElMessage.success('Personal information updated successfully.')
    await router.push('/customer/profile')
  } catch (error) {
    saveErrorMessage.value =
      error instanceof Error && error.message
        ? error.message
        : 'Unable to save your profile right now.'
    ElMessage.error(saveErrorMessage.value)
  } finally {
    isSaving.value = false
  }
}

const goToProfile = (): void => {
  void router.push('/customer/profile')
}

onMounted(() => {
  void loadProfile()
})
</script>

<style scoped lang="scss">
.page-card {
  padding: var(--space-6);
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 16px var(--color-shadow);
}

.profile-edit-page {
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

.profile-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-bg-surface);
}

.profile-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-muted);
}

.profile-panel__title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1.25rem;
}

.profile-panel__subtitle {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.profile-panel__actions,
.state-actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.profile-panel__body {
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

.profile-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--space-4);
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
  .profile-panel__header {
    flex-direction: column;
  }

  .profile-panel__actions {
    width: 100%;
  }
}
</style>
