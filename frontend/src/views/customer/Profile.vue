<template>
  <section class="page-card profile-page">
    <div class="page-header">
      <p class="page-tag">Customer</p>
      <h1 class="page-title">Profile and Account Settings</h1>
      <p class="page-text">
        Use this account settings page to review your profile details and manage your password,
        page style preference, and account access.
      </p>
    </div>

    <div v-if="viewState === 'loading'" class="profile-panel">
      <div class="profile-panel__header">
        <div>
          <h2 class="profile-panel__title">Loading profile</h2>
          <p class="profile-panel__subtitle">
            We are preparing your customer profile details.
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
        <CustomButton type="primary" @click="loadProfile">Retry</CustomButton>
      </div>
    </div>

    <div v-else class="profile-content">
      <div class="profile-panel">
        <div class="profile-panel__header">
          <div>
            <h2 class="profile-panel__title">Personal Information</h2>
            <p class="profile-panel__subtitle">
              Keep your name, email address, and phone number up to date.
            </p>
          </div>

          <div class="profile-panel__actions">
            <CustomButton type="primary" @click="goToEditPage">Edit Profile</CustomButton>
          </div>
        </div>

        <div class="profile-panel__body">
          <dl class="detail-grid">
            <div
              v-for="item in profileDetails"
              :key="item.label"
              class="detail-card"
            >
              <dt class="detail-label">{{ item.label }}</dt>
              <dd
                class="detail-value"
                :class="{ 'detail-value--empty': !item.value }"
              >
                {{ item.value || 'Not provided' }}
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <div class="profile-panel">
        <div class="profile-panel__header">
          <div>
            <h2 class="profile-panel__title">Account Settings</h2>
            <p class="profile-panel__subtitle">
              Manage your password, page style preferences, and account access.
            </p>
          </div>
        </div>

        <div class="profile-panel__body settings-list">
          <div class="settings-item">
            <div class="settings-copy">
              <h3 class="settings-title">Change Password</h3>
              <p class="settings-text">
                Update your password to keep your account secure during normal use.
              </p>
            </div>
            <CustomButton @click="goToChangePassword">Change Password</CustomButton>
          </div>

          <div class="settings-item">
            <div class="settings-copy">
              <h3 class="settings-title">Page Style Settings</h3>
              <p class="settings-text">
                Choose how the interface looks, including light mode and dark mode. Current page
                style: {{ currentPageStyleText }}.
              </p>
            </div>
            <CustomButton @click="goToStyleSettings">Page Style Settings</CustomButton>
          </div>

          <div class="settings-item settings-item--danger">
            <div class="settings-copy">
              <h3 class="settings-title">Deactivate Account</h3>
              <p class="settings-text">
                Permanently deactivate this account if you no longer want to use the consultation service.
              </p>
            </div>
            <CustomButton type="danger" @click="confirmDeactivateAccount">Deactivate Account</CustomButton>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import CustomButton from '@/components/common/CustomButton.vue'

defineOptions({ name: 'CustomerProfile' })

type ViewState = 'loading' | 'ready' | 'error'
type UserThemePreference = 'light' | 'dark'

interface ProfileUser {
  fullName: string
  email: string
  phoneNumber: string
}

const USER_PROFILE_STORAGE_KEY = 'mock-user-profile'
const USER_THEME_STORAGE_KEY = 'mock-user-theme-preference'

const mockUser: ProfileUser = {
  fullName: 'Emma Chen',
  email: 'emma.chen@example.com',
  phoneNumber: '+86 138 0013 8000'
}

const wait = async (delay = 250): Promise<void> => {
  await new Promise(resolve => window.setTimeout(resolve, delay))
}

const readStoredProfile = (): ProfileUser => {
  const storedProfile = localStorage.getItem(USER_PROFILE_STORAGE_KEY)

  if (!storedProfile) {
    return { ...mockUser }
  }

  try {
    const parsedProfile = JSON.parse(storedProfile) as Partial<ProfileUser>
    return {
      ...mockUser,
      ...parsedProfile
    }
  } catch {
    return { ...mockUser }
  }
}

const readStoredThemePreference = (): UserThemePreference | null => {
  const storedPreference = localStorage.getItem(USER_THEME_STORAGE_KEY)
  return storedPreference === 'dark' || storedPreference === 'light'
    ? storedPreference
    : null
}

const router = useRouter()

const viewState = ref<ViewState>('loading')
const profile = ref<ProfileUser | null>(null)
const loadErrorMessage = ref('We could not load your profile details.')

const profileDetails = computed(() => [
  {
    label: 'Name',
    value: profile.value?.fullName?.trim() || ''
  },
  {
    label: 'Email',
    value: profile.value?.email?.trim() || ''
  },
  {
    label: 'Phone',
    value: profile.value?.phoneNumber?.trim() || ''
  }
])

const currentPageStyleText = computed(() => {
  const savedThemePreference = readStoredThemePreference()

  return savedThemePreference === 'dark'
    ? 'Dark Mode (saved preference)'
    : savedThemePreference === 'light'
      ? 'Light Mode (saved preference)'
      : 'Light Mode (default page style option)'
})

const loadProfile = async (): Promise<void> => {
  viewState.value = 'loading'
  loadErrorMessage.value = 'We could not load your profile details.'

  try {
    await wait()
    const currentProfile = readStoredProfile()
    profile.value = currentProfile
    viewState.value = 'ready'
  } catch (error) {
    loadErrorMessage.value =
      error instanceof Error && error.message
        ? error.message
        : 'We could not load your profile details.'
    viewState.value = 'error'
  }
}

const goToEditPage = (): void => {
  void router.push('/customer/profile/edit')
}

const goToChangePassword = (): void => {
  void router.push('/customer/profile/password')
}

const goToStyleSettings = (): void => {
  void router.push('/customer/profile/style-settings')
}

const confirmDeactivateAccount = async (): Promise<void> => {
  try {
    await ElMessageBox.confirm(
      'Deactivating your account is permanent and is different from logging out. Once confirmed, your current session will end and protected customer pages will no longer be accessible.',
      'Deactivate Account',
      {
        type: 'warning',
        confirmButtonText: 'Deactivate Account',
        cancelButtonText: 'Cancel'
      }
    )
  } catch {
    return
  }

  try {
    ElMessage.success('Mock mode: no real account changes were made.')
  } catch (error) {
    ElMessage.error(
      error instanceof Error && error.message
        ? error.message
        : 'Unable to deactivate your account right now.'
    )
  }
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

.profile-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.profile-content {
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

.skeleton-grid,
.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.skeleton-card,
.detail-card {
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

.detail-card {
  margin: 0;
}

.detail-label {
  margin: 0 0 var(--space-2);
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
}

.detail-value {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1rem;
  font-weight: 500;
  word-break: break-word;
}

.detail-value--empty {
  color: var(--color-text-tertiary);
  font-weight: 400;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.settings-item--danger {
  border-color: var(--color-border-strong);
}

.settings-copy {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.settings-title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1rem;
}

.settings-text {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
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

  .settings-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
