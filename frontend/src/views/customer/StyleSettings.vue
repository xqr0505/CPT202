<template>
  <section class="page-card style-page">
    <div class="page-header">
      <p class="page-tag">Customer</p>
      <h1 class="page-title">Page Style Settings</h1>
      <p class="page-text">
        Choose the page style option you want the system to apply across the interface.
      </p>
    </div>

    <div v-if="viewState === 'loading'" class="style-panel">
      <div class="style-panel__header">
        <div>
          <h2 class="style-panel__title">Loading style settings</h2>
          <p class="style-panel__subtitle">
            We are preparing your saved interface preference.
          </p>
        </div>
      </div>

      <div class="style-panel__body loading-layout">
        <el-skeleton animated>
          <template #template>
            <div class="skeleton-grid">
              <div v-for="index in 2" :key="index" class="skeleton-card">
                <el-skeleton-item variant="text" class="skeleton-label" />
                <el-skeleton-item variant="h3" class="skeleton-value" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <div v-else-if="viewState === 'error'" class="state-card">
      <h2 class="state-title">Unable to load style settings</h2>
      <p class="state-text">{{ loadErrorMessage }}</p>
      <div class="state-actions">
        <CustomButton @click="goToProfile">Back to Profile</CustomButton>
        <CustomButton type="primary" @click="loadSettings">Retry</CustomButton>
      </div>
    </div>

    <div v-else class="style-panel">
      <div class="style-panel__header">
        <div>
          <h2 class="style-panel__title">Page Style Preference</h2>
          <p class="style-panel__subtitle">
            Select your preferred page style option and save the changes.
          </p>
        </div>

        <div class="style-panel__actions">
          <CustomButton @click="goToProfile">Back to Profile</CustomButton>
          <CustomButton type="primary" :loading="isSaving" @click="saveSettings">
            Save Changes
          </CustomButton>
        </div>
      </div>

      <div class="style-panel__body">
        <div class="style-summary">
          <p class="summary-label">Current page style</p>
          <p class="summary-value">{{ selectedPreferenceLabel }}</p>
          <p class="summary-text">
            <template v-if="savedPreference">
              A saved page style preference was found. This selected style will continue to be
              applied across the interface.
            </template>
            <template v-else>
              No page style preference has been saved. The default page style option is Light Mode.
            </template>
          </p>
        </div>

        <el-radio-group v-model="selectedPreference" class="style-options">
          <label
            v-for="option in options"
            :key="option.value"
            class="style-option"
            :class="{ 'style-option--selected': selectedPreference === option.value }"
          >
            <el-radio :value="option.value">
              <span class="style-option__title">{{ option.label }}</span>
            </el-radio>
            <p class="style-option__text">{{ option.description }}</p>
          </label>
        </el-radio-group>

        <p v-if="saveErrorMessage" class="feedback-message feedback-message--error">
          {{ saveErrorMessage }}
        </p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import CustomButton from '@/components/common/CustomButton.vue'
import {
  fetchUserStyleSettings,
  isUserAccountDeactivatedError,
  isUserProfileAuthError,
  updateUserStyleSettings,
  type UserThemePreference
} from '@/api/user'

defineOptions({ name: 'CustomerStyleSettings' })

type ViewState = 'loading' | 'ready' | 'error'

const router = useRouter()

const viewState = ref<ViewState>('loading')
const isSaving = ref(false)
const loadErrorMessage = ref('We could not load style settings.')
const saveErrorMessage = ref('')
const savedPreference = ref<UserThemePreference | null>(null)
const selectedPreference = ref<UserThemePreference>('light')

const options = [
  {
    value: 'light' as const,
    label: 'Light Mode',
    description: 'Apply the default light page style across the interface.'
  },
  {
    value: 'dark' as const,
    label: 'Dark Mode',
    description: 'Apply the dark page style across the interface.'
  }
]

const selectedPreferenceLabel = computed(() => {
  return selectedPreference.value === 'dark' ? 'Dark Mode' : 'Light Mode'
})

const redirectToLogin = (reason?: 'deactivated'): void => {
  void router.replace({
    path: '/auth/login',
    query: reason
      ? { reason }
      : undefined
  })
}

const loadSettings = async (): Promise<void> => {
  viewState.value = 'loading'
  loadErrorMessage.value = 'We could not load style settings.'
  saveErrorMessage.value = ''

  try {
    const settings = await fetchUserStyleSettings()
    savedPreference.value = settings.savedPreference
    selectedPreference.value = settings.effectivePreference
    viewState.value = 'ready'
  } catch (error) {
    if (isUserAccountDeactivatedError(error)) {
      redirectToLogin('deactivated')
      return
    }

    if (isUserProfileAuthError(error)) {
      redirectToLogin()
      return
    }

    loadErrorMessage.value =
      error instanceof Error && error.message
        ? error.message
        : 'We could not load style settings.'
    viewState.value = 'error'
  }
}

const saveSettings = async (): Promise<void> => {
  saveErrorMessage.value = ''
  isSaving.value = true

  try {
    const settings = await updateUserStyleSettings(selectedPreference.value)
    savedPreference.value = settings.savedPreference
    selectedPreference.value = settings.effectivePreference
    ElMessage.success('Page style preference updated successfully.')
  } catch (error) {
    if (isUserAccountDeactivatedError(error)) {
      redirectToLogin('deactivated')
      return
    }

    if (isUserProfileAuthError(error)) {
      redirectToLogin()
      return
    }

    saveErrorMessage.value =
      error instanceof Error && error.message
        ? error.message
        : 'Unable to update page style settings right now.'
    ElMessage.error(saveErrorMessage.value)
  } finally {
    isSaving.value = false
  }
}

const goToProfile = (): void => {
  void router.push('/customer/profile')
}

onMounted(() => {
  void loadSettings()
})
</script>

<style scoped lang="scss">
.page-card {
  padding: var(--space-6);
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 16px var(--color-shadow);
}

.style-page {
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

.style-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-bg-surface);
}

.style-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-muted);
}

.style-panel__title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1.25rem;
}

.style-panel__subtitle {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.style-panel__actions,
.state-actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.style-panel__body {
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
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

.style-summary {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.summary-label {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
}

.summary-value {
  margin: var(--space-2) 0 0;
  color: var(--color-text-primary);
  font-size: 1.125rem;
  font-weight: 600;
}

.summary-text {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.style-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--space-4);
}

.style-option {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}

.style-option--selected {
  border-color: var(--color-border-strong);
  box-shadow: 0 4px 12px var(--color-shadow);
}

.style-option__title {
  color: var(--color-text-primary);
  font-weight: 600;
}

.style-option__text {
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
  .style-panel__header {
    flex-direction: column;
  }

  .style-panel__actions {
    width: 100%;
  }
}
</style>
