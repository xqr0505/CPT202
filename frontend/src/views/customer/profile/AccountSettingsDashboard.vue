<template>
  <section class="page-card settings-page">
    <div class="page-header">
      <div>
        <p class="page-tag">Account</p>
        <h1 class="page-title">Account Settings</h1>
        <p class="page-text">
          Manage your personal details, contact information, password, and account controls from
          one polished settings dashboard.
        </p>
      </div>

      <div class="page-meta">
        <el-tag :type="completenessPercentage === 100 ? 'success' : 'warning'" effect="light">
          {{ completenessPercentage === 100 ? 'Profile Ready' : 'Needs Attention' }}
        </el-tag>
        <span class="page-meta__text">{{ profileMetaText }}</span>
      </div>
    </div>

    <div v-if="viewState === 'loading'" class="loading-shell">
      <div v-for="index in 6" :key="index" class="loading-card">
        <el-skeleton animated>
          <template #template>
            <div class="skeleton-card">
              <el-skeleton-item variant="h3" class="skeleton-card__title" />
              <el-skeleton-item variant="text" class="skeleton-card__text" />
              <el-skeleton-item variant="text" class="skeleton-card__text skeleton-card__text--short" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <div v-else-if="viewState === 'error'" class="state-card">
      <h2 class="state-title">Unable to load account settings</h2>
      <p class="state-text">{{ loadErrorMessage }}</p>
      <div class="state-actions">
        <CustomButton type="primary" @click="loadDashboard">Retry</CustomButton>
      </div>
    </div>

    <div v-else class="dashboard">
      <section class="settings-card settings-card--avatar">
        <div class="settings-card__header">
          <div>
            <p class="settings-card__eyebrow">Avatar</p>
            <h2 class="settings-card__title">Profile photo</h2>
            <p class="settings-card__subtitle">
              Keep a recognizable avatar at the top of your account settings so your profile feels
              consistent across visits and devices.
            </p>
          </div>
          <el-tag effect="plain">{{ avatarTagText }}</el-tag>
        </div>

        <div class="settings-card__body">
          <div
            v-if="avatarNotice"
            class="status-banner"
            :class="`status-banner--${avatarNotice.tone}`"
            aria-live="polite"
          >
            <div class="status-banner__body">
              <strong class="status-banner__title">{{ avatarNotice.title }}</strong>
              <p class="status-banner__text">{{ avatarNotice.message }}</p>
            </div>
          </div>

          <div class="avatar-panel">
            <div class="avatar-panel__preview">
              <el-avatar :src="currentAvatarUrl" :size="112" class="avatar-panel__image">
                {{ avatarInitials }}
              </el-avatar>

              <div class="avatar-panel__details">
                <strong class="avatar-panel__name">{{ avatarDisplayName }}</strong>
                <span class="avatar-panel__status">{{ avatarSummaryText }}</span>
                <span class="avatar-panel__meta">Source: {{ accountDataSourceLabel }}</span>
              </div>
            </div>

            <div class="avatar-panel__actions">
              <input
                ref="avatarInputRef"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                class="visually-hidden"
                @change="handleAvatarFileChange"
              />

              <CustomButton
                type="primary"
                :loading="isUploadingAvatar"
                :disabled="!canUploadAvatar"
                @click="triggerAvatarSelection"
              >
                {{ currentAvatarUrl ? 'Change Avatar' : 'Upload Avatar' }}
              </CustomButton>

              <span class="avatar-panel__hint">{{ avatarHelperText }}</span>
            </div>
          </div>
        </div>
      </section>

      <el-form
        ref="profileFormRef"
        :model="profileForm"
        :rules="profileRules"
        label-position="top"
        class="settings-form"
      >
        <section
          ref="personalSectionRef"
          class="settings-card"
          :class="{ 'settings-card--highlighted': highlightedSection === 'personal' }"
        >
          <div class="settings-card__header">
            <div>
              <p class="settings-card__eyebrow">Personal Information</p>
              <h2 class="settings-card__title">Identity details</h2>
              <p class="settings-card__subtitle">
                Keep the name on your account accurate so bookings, support requests, and profile
                records stay consistent.
              </p>
            </div>
            <el-tag effect="plain">{{ profileForm.fullName.trim() ? 'On file' : 'Missing' }}</el-tag>
          </div>

          <div class="settings-card__body">
            <div class="field-grid field-grid--single">
              <el-form-item label="Full name" prop="fullName">
                <el-input
                  v-model="profileForm.fullName"
                  placeholder="Enter your full name"
                  maxlength="60"
                />
              </el-form-item>
            </div>

            <p class="section-note">
              This name is used as your primary account identity within the shared account settings
              flow.
            </p>
          </div>
        </section>

        <section
          ref="contactSectionRef"
          class="settings-card"
          :class="{ 'settings-card--highlighted': highlightedSection === 'contact' }"
        >
          <div class="settings-card__header">
            <div>
              <p class="settings-card__eyebrow">Contact Information</p>
              <h2 class="settings-card__title">Communication details</h2>
              <p class="settings-card__subtitle">
                Save the email address and phone number used for account notices and booking
                follow-up.
              </p>
            </div>
            <el-tag effect="plain">{{ profileForm.email.trim() && profileForm.localPhoneNumber.trim() ? 'Reachable' : 'Review needed' }}</el-tag>
          </div>

          <div class="settings-card__body">
            <div class="field-grid">
              <el-form-item label="Email address" prop="email">
                <el-input
                  v-model="profileForm.email"
                  type="email"
                  placeholder="Enter your email address"
                />
              </el-form-item>

              <el-form-item label="Phone number" prop="localPhoneNumber">
                <div class="phone-field">
                  <el-select
                    v-model="profileForm.countryCode"
                    filterable
                    class="phone-field__code"
                    placeholder="Code"
                    @change="handleCountryCodeChange"
                  >
                    <el-option
                      v-for="option in countryCodeOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>

                  <el-input
                    v-model="profileForm.localPhoneNumber"
                    class="phone-field__number"
                    placeholder="Enter local number"
                    maxlength="14"
                  />
                </div>
              </el-form-item>
            </div>

            <div class="contact-summary">
              <div class="contact-summary__item">
                <span class="contact-summary__label">Saved phone format</span>
                <strong class="contact-summary__value">{{ composedPhonePreview || 'Not provided' }}</strong>
              </div>
              <div class="contact-summary__item">
                <span class="contact-summary__label">Profile data source</span>
                <strong class="contact-summary__value">{{ accountDataSourceLabel }}</strong>
              </div>
            </div>
          </div>
        </section>
      </el-form>

      <section
        ref="securitySectionRef"
        class="settings-card"
        :class="{ 'settings-card--highlighted': highlightedSection === 'security' }"
      >
        <div class="settings-card__header">
          <div>
            <p class="settings-card__eyebrow">Security</p>
            <h2 class="settings-card__title">Password protection</h2>
            <p class="settings-card__subtitle">
              Change your password with clear requirements, instant strength guidance, and
              compatibility with the existing password endpoint.
            </p>
          </div>
          <el-tag :type="passwordStrength.tagType" effect="light">
            {{ passwordStrength.label }}
          </el-tag>
        </div>

        <div class="settings-card__body">
          <div
            v-if="passwordNotice"
            class="status-banner"
            :class="`status-banner--${passwordNotice.tone}`"
            aria-live="polite"
          >
            <div class="status-banner__body">
              <strong class="status-banner__title">{{ passwordNotice.title }}</strong>
              <p class="status-banner__text">{{ passwordNotice.message }}</p>
            </div>
          </div>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-position="top"
            class="password-form"
          >
            <el-form-item label="Current password" prop="currentPassword">
              <el-input
                v-model="passwordForm.currentPassword"
                type="password"
                show-password
                autocomplete="current-password"
                placeholder="Enter your current password"
              />
            </el-form-item>

            <el-form-item label="New password" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="Create a new password"
              />
            </el-form-item>

            <el-form-item label="Confirm new password" prop="confirmationPassword">
              <el-input
                v-model="passwordForm.confirmationPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="Confirm your new password"
              />
            </el-form-item>
          </el-form>

          <div class="strength-panel">
            <div class="strength-panel__header">
              <div>
                <span class="strength-panel__label">Password strength</span>
                <strong class="strength-panel__value">{{ passwordStrength.label }}</strong>
              </div>
              <span class="strength-panel__hint">{{ passwordStrength.feedback }}</span>
            </div>

            <div class="strength-meter">
              <span
                v-for="segment in 4"
                :key="segment"
                class="strength-meter__segment"
                :class="[
                  { 'strength-meter__segment--active': segment <= passwordStrength.score },
                  `strength-meter__segment--${passwordStrength.tone}`
                ]"
              />
            </div>
          </div>

          <ul class="rule-list">
            <li
              v-for="rule in passwordChecklist"
              :key="rule.label"
              class="rule-list__item"
              :class="{ 'rule-list__item--met': rule.met }"
            >
              <span class="rule-list__indicator" />
              <span>{{ rule.label }}</span>
            </li>
          </ul>

          <div class="form-footer form-footer--stacked">
            <div class="form-footer__meta">
              <strong>{{ passwordMetaText }}</strong>
              <span>
                Your current password is required before a new one can be applied.
              </span>
            </div>

            <div class="form-footer__actions">
              <CustomButton
                :disabled="!hasPasswordInput || isSavingPassword"
                @click="resetPasswordForm"
              >
                Clear
              </CustomButton>
              <CustomButton
                type="primary"
                :loading="isSavingPassword"
                :disabled="!canAttemptPasswordSave"
                @click="savePassword"
              >
                Update Password
              </CustomButton>
            </div>
          </div>
        </div>
      </section>

      <section class="settings-card">
        <div class="settings-card__header">
          <div>
            <p class="settings-card__eyebrow">Appearance</p>
            <h2 class="settings-card__title">Light mode and style settings</h2>
            <p class="settings-card__subtitle">
              Keep your appearance preferences accessible from the same account settings flow while
              continuing to use the existing style settings behavior.
            </p>
          </div>
          <el-tag effect="plain">{{ currentPageStyleText }}</el-tag>
        </div>

        <div class="settings-card__body">
            <div class="appearance-panel">
              <span class="appearance-panel__label">Current appearance</span>
              <strong class="appearance-panel__value">{{ currentPageStyleText }}</strong>
              <p class="appearance-panel__text">
                Appearance preferences are separate from the live account profile APIs. This section
                only reflects the current interface theme without mixing it into profile data.
              </p>
            </div>

          <div class="appearance-actions">
            <CustomButton @click="goToStyleSettings">Open Style Settings</CustomButton>
          </div>
        </div>
      </section>

      <section class="settings-card settings-card--actions">
        <div class="settings-card__header">
          <div>
            <p class="settings-card__eyebrow">Save Changes</p>
            <h2 class="settings-card__title">Unsaved changes and save controls</h2>
            <p class="settings-card__subtitle">
              Review the current edit state, restore the last saved values, or save profile changes
              when you are ready.
            </p>
          </div>
          <el-tag :type="hasUnsavedProfileChanges ? 'warning' : 'success'" effect="light">
            {{ hasUnsavedProfileChanges ? 'Unsaved edits' : 'Up to date' }}
          </el-tag>
        </div>

        <div class="settings-card__body">
          <div
            v-if="profileNotice"
            class="status-banner"
            :class="`status-banner--${profileNotice.tone}`"
            aria-live="polite"
          >
            <div class="status-banner__body">
              <strong class="status-banner__title">{{ profileNotice.title }}</strong>
              <p class="status-banner__text">{{ profileNotice.message }}</p>
            </div>
          </div>

          <div
            v-if="hasUnsavedProfileChanges"
            class="status-banner status-banner--warning"
            aria-live="polite"
          >
            <div class="status-banner__body">
              <strong class="status-banner__title">You have unsaved changes</strong>
              <p class="status-banner__text">
                Review the edited fields above, then save or reset the profile before leaving this
                page.
              </p>
            </div>
          </div>

          <div class="form-footer">
            <div class="form-footer__meta">
              <strong>{{ profileMetaText }}</strong>
              <span>{{ hasUnsavedProfileChanges ? 'Changes are pending until you save them to the backend.' : 'Your saved details are currently in sync with the backend.' }}</span>
            </div>

            <div class="form-footer__actions">
              <CustomButton
                :disabled="!hasUnsavedProfileChanges || isSavingProfile"
                @click="resetProfileForm"
              >
                Reset
              </CustomButton>
              <CustomButton
                type="primary"
                :loading="isSavingProfile"
                :disabled="!hasUnsavedProfileChanges"
                @click="saveProfile"
              >
                Save Changes
              </CustomButton>
            </div>
          </div>
        </div>
      </section>

      <section
        ref="statusSectionRef"
        class="settings-card settings-card--danger"
        :class="{ 'settings-card--highlighted': highlightedSection === 'status' }"
      >
        <div class="settings-card__header">
          <div>
            <p class="settings-card__eyebrow">Danger Zone</p>
            <h2 class="settings-card__title">Danger Zone</h2>
            <p class="settings-card__subtitle">
              Deactivate your account as a soft status change. Your data stays in the database, but
              normal account access should stop after confirmation.
            </p>
          </div>
          <el-tag :type="accountStatusTagType" effect="light">{{ accountStatusLabel }}</el-tag>
        </div>

        <div class="settings-card__body">
          <div
            v-if="deactivationNotice"
            class="status-banner"
            :class="`status-banner--${deactivationNotice.tone}`"
            aria-live="polite"
          >
            <div class="status-banner__body">
              <strong class="status-banner__title">{{ deactivationNotice.title }}</strong>
              <p class="status-banner__text">{{ deactivationNotice.message }}</p>
            </div>
          </div>

          <div class="status-overview">
            <div class="status-overview__item">
              <span class="status-overview__label">Account state</span>
              <strong class="status-overview__value">{{ accountStatusLabel }}</strong>
            </div>
            <div class="status-overview__item">
              <span class="status-overview__label">Deactivation type</span>
              <strong class="status-overview__value">Soft deactivation</strong>
            </div>
            <div class="status-overview__item">
              <span class="status-overview__label">After confirmation</span>
              <strong class="status-overview__value">Sign out and access loss</strong>
            </div>
          </div>

          <div class="danger-item">
            <div class="danger-item__copy">
              <div class="danger-item__title-row">
                <h3 class="danger-item__title">Deactivate Account</h3>
                <span class="danger-item__tag">Real action</span>
              </div>
              <p class="danger-item__text">
                This does not physically delete your data. It updates your account status to
                deactivated and should prevent normal future use of the account.
              </p>
            </div>

            <CustomButton
              type="danger"
              :loading="isDeactivatingAccount"
              :disabled="!canDeactivateAccount"
              @click="deactivateAccount"
            >
              Deactivate Account
            </CustomButton>
          </div>

          <p class="danger-note">
            {{ dangerZoneHelperText }}
          </p>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch
} from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import CustomButton from '@/components/common/CustomButton.vue'
import {
  changeCurrentUserPassword,
  deactivateCurrentUserAccount,
  getCurrentUserProfile,
  uploadCurrentUserAvatar,
  updateCurrentUserProfile,
  type AccountProfile,
  type AvatarUploadResponse,
  type ChangePasswordPayload,
  type UpdateUserProfilePayload
} from '@/api/user'
import {
  getAuthToken,
  isAuthFailureError,
  logout as clearAuthAndRedirect
} from '@/api/request'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'AccountSettingsDashboard' })

type ViewState = 'loading' | 'ready' | 'error'
type StatusTone = 'success' | 'error' | 'info' | 'warning'
type SectionKey = 'personal' | 'contact' | 'security' | 'status'

interface Props {
  initialSection?: SectionKey | 'overview'
}

interface CountryCodeOption {
  label: string
  value: string
}

interface ProfileFormModel {
  fullName: string
  email: string
  countryCode: string
  localPhoneNumber: string
}

interface PasswordFormModel {
  currentPassword: string
  newPassword: string
  confirmationPassword: string
}

interface SectionNotice {
  tone: StatusTone
  title: string
  message: string
}

interface CompletionItem {
  label: string
  missingLabel: string
  complete: boolean
  section: SectionKey
}

const props = withDefaults(defineProps<Props>(), {
  initialSection: 'overview'
})

const DEFAULT_COUNTRY_CODE = '+86'
const MAX_AVATAR_FILE_SIZE_BYTES = 2 * 1024 * 1024
const ALLOWED_AVATAR_MIME_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
const EMPTY_PROFILE: AccountProfile = {
  id: 0,
  fullName: '',
  email: '',
  phoneNumber: '',
  avatarUrl: '',
  status: 'ACTIVE'
}

const COUNTRY_CODE_OPTIONS: ReadonlyArray<CountryCodeOption> = [
  { label: 'China (+86)', value: '+86' },
  { label: 'Hong Kong SAR (+852)', value: '+852' },
  { label: 'Singapore (+65)', value: '+65' },
  { label: 'Japan (+81)', value: '+81' },
  { label: 'South Korea (+82)', value: '+82' },
  { label: 'Malaysia (+60)', value: '+60' },
  { label: 'Thailand (+66)', value: '+66' },
  { label: 'India (+91)', value: '+91' },
  { label: 'United States / Canada (+1)', value: '+1' },
  { label: 'United Kingdom (+44)', value: '+44' },
  { label: 'Germany (+49)', value: '+49' },
  { label: 'France (+33)', value: '+33' },
  { label: 'Australia (+61)', value: '+61' },
  { label: 'United Arab Emirates (+971)', value: '+971' },
  { label: 'Brazil (+55)', value: '+55' }
]

const COUNTRY_CODE_PATTERN = /^\+\d{1,3}$/
const LOCAL_PHONE_NUMBER_PATTERN = /^\d+$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const COMMON_REGION_PHONE_RULES: Readonly<Record<string, { exactLength: number; message: string }>> = {
  '+86': {
    exactLength: 11,
    message: 'China phone numbers must be 11 digits.'
  },
  '+65': {
    exactLength: 8,
    message: 'Singapore phone numbers must be 8 digits.'
  },
  '+1': {
    exactLength: 10,
    message: 'US/Canada phone numbers must be 10 digits.'
  }
}

const router = useRouter()
const userStore = useUserStore()

const viewState = ref<ViewState>('loading')
const loadErrorMessage = ref('We could not load your account settings.')
const avatarNotice = ref<SectionNotice | null>(null)
const profileNotice = ref<SectionNotice | null>(null)
const passwordNotice = ref<SectionNotice | null>(null)
const deactivationNotice = ref<SectionNotice | null>(null)
const isUploadingAvatar = ref(false)
const isSavingProfile = ref(false)
const isSavingPassword = ref(false)
const isDeactivatingAccount = ref(false)
const originalProfile = ref<AccountProfile | null>(null)
const lastProfileSavedAt = ref<Date | null>(null)
const lastPasswordUpdatedAt = ref<Date | null>(null)
const highlightedSection = ref<SectionKey | null>(null)
const highlightTimer = ref<number | null>(null)
const shouldBypassUnsavedChangesPrompt = ref(false)

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const avatarInputRef = ref<HTMLInputElement>()

const personalSectionRef = ref<HTMLElement>()
const contactSectionRef = ref<HTMLElement>()
const securitySectionRef = ref<HTMLElement>()
const statusSectionRef = ref<HTMLElement>()

const profileForm = reactive<ProfileFormModel>({
  fullName: '',
  email: '',
  countryCode: DEFAULT_COUNTRY_CODE,
  localPhoneNumber: ''
})

const passwordForm = reactive<PasswordFormModel>({
  currentPassword: '',
  newPassword: '',
  confirmationPassword: ''
})

const hasApiSession = computed(() => Boolean(getAuthToken()))
const accountDataSourceLabel = computed(() => 'Live account data')
const currentAccountStatus = computed(() => originalProfile.value?.status ?? EMPTY_PROFILE.status)
const currentAvatarUrl = computed(() => originalProfile.value?.avatarUrl?.trim() || '')
const avatarTagText = computed(() => (currentAvatarUrl.value ? 'Custom avatar' : 'Default avatar'))
const avatarDisplayName = computed(() => {
  return (
    profileForm.fullName.trim() ||
    originalProfile.value?.fullName?.trim() ||
    userStore.userInfo?.nickname?.trim() ||
    userStore.userInfo?.username?.trim() ||
    'Account profile'
  )
})
const avatarInitials = computed(() => buildAvatarInitials(avatarDisplayName.value))
const avatarSummaryText = computed(() => {
  return currentAvatarUrl.value
    ? 'Your saved avatar is ready and will load again the next time you open this page.'
    : 'No avatar is saved yet, so a clean default placeholder is shown for now.'
})
const canUploadAvatar = computed(() => {
  return hasApiSession.value && currentAccountStatus.value === 'ACTIVE' && !isUploadingAvatar.value
})
const avatarHelperText = computed(() => {
  if (!hasApiSession.value) {
    return 'An authenticated session is required to upload and save an avatar to your profile.'
  }

  if (currentAccountStatus.value !== 'ACTIVE') {
    return 'Only active accounts can upload a new avatar.'
  }

  return 'JPG, PNG, or WEBP up to 2 MB. Uploading replaces the current profile photo immediately.'
})

const accountStatusLabel = computed(() => {
  switch (currentAccountStatus.value) {
    case 'DEACTIVATED':
      return 'Deactivated'
    case 'LOCKED':
      return 'Locked'
    case 'ACTIVE':
    default:
      return 'Active'
  }
})

const accountStatusTagType = computed(() => {
  switch (currentAccountStatus.value) {
    case 'DEACTIVATED':
      return 'danger' as const
    case 'LOCKED':
      return 'warning' as const
    case 'ACTIVE':
    default:
      return 'success' as const
  }
})

const canDeactivateAccount = computed(() => {
  return hasApiSession.value && currentAccountStatus.value === 'ACTIVE' && !isDeactivatingAccount.value
})

const dangerZoneHelperText = computed(() => {
  if (!hasApiSession.value) {
    return 'An authenticated session is required to deactivate this account.'
  }

  if (currentAccountStatus.value === 'DEACTIVATED') {
    return 'This account is already marked as deactivated.'
  }

  return 'After confirmation, the backend updates the existing account status field to deactivated and the frontend signs you out immediately.'
})

const countryCodeOptions = computed<CountryCodeOption[]>(() => {
  const currentCountryCode = profileForm.countryCode.trim()
  const exists = COUNTRY_CODE_OPTIONS.some(option => option.value === currentCountryCode)

  if (!currentCountryCode || exists) {
    return [...COUNTRY_CODE_OPTIONS]
  }

  return [
    {
      label: `Current code (${currentCountryCode})`,
      value: currentCountryCode
    },
    ...COUNTRY_CODE_OPTIONS
  ]
})

const composedPhonePreview = computed(() => {
  return buildPhoneNumber(profileForm.countryCode, profileForm.localPhoneNumber)
})

const trimmedProfileDraft = computed<UpdateUserProfilePayload>(() => {
  return {
    fullName: profileForm.fullName.trim(),
    email: profileForm.email.trim(),
    phoneNumber: buildPhoneNumber(profileForm.countryCode, profileForm.localPhoneNumber)
  }
})

const profileCompletionItems = computed<CompletionItem[]>(() => {
  return [
    {
      label: 'Full name',
      missingLabel: 'Complete personal info',
      complete: Boolean(profileForm.fullName.trim()),
      section: 'personal'
    },
    {
      label: 'Email address',
      missingLabel: 'Add contact email',
      complete: Boolean(profileForm.email.trim()),
      section: 'contact'
    },
    {
      label: 'Phone number',
      missingLabel: 'Add phone number',
      complete: Boolean(profileForm.localPhoneNumber.trim()),
      section: 'contact'
    }
  ]
})

const completenessPercentage = computed(() => {
  if (!profileCompletionItems.value.length) {
    return 0
  }

  return Math.round(
    (profileCompletionItems.value.filter(item => item.complete).length /
      profileCompletionItems.value.length) *
      100
  )
})

const currentPageStyleText = computed(() => {
  if (typeof document === 'undefined') {
    return 'Light Mode'
  }

  const html = document.documentElement
  const isDarkMode =
    html.getAttribute('data-theme') === 'dark' || html.classList.contains('dark')

  return isDarkMode ? 'Dark Mode' : 'Light Mode'
})

const hasUnsavedProfileChanges = computed(() => {
  if (!originalProfile.value) {
    return false
  }

  return (
    trimmedProfileDraft.value.fullName !== originalProfile.value.fullName ||
    trimmedProfileDraft.value.email !== originalProfile.value.email ||
    trimmedProfileDraft.value.phoneNumber !== originalProfile.value.phoneNumber
  )
})

const profileMetaText = computed(() => {
  if (currentAccountStatus.value === 'DEACTIVATED') {
    return 'Account is currently deactivated'
  }

  if (hasUnsavedProfileChanges.value) {
    return 'Unsaved profile edits in progress'
  }

  if (lastProfileSavedAt.value) {
    return `Saved ${formatTimestamp(lastProfileSavedAt.value)}`
  }

  return 'Profile synced with current account data'
})

const passwordMetaText = computed(() => {
  if (lastPasswordUpdatedAt.value) {
    return `Password updated ${formatTimestamp(lastPasswordUpdatedAt.value)}`
  }

  return 'Ready for a secure password update'
})

const passwordChecklist = computed(() => {
  const passwordValue = passwordForm.newPassword.trim()

  return [
    {
      label: 'At least 8 characters',
      met: passwordValue.length >= 8
    },
    {
      label: 'Contains an uppercase letter',
      met: /[A-Z]/.test(passwordValue)
    },
    {
      label: 'Contains a lowercase letter',
      met: /[a-z]/.test(passwordValue)
    },
    {
      label: 'Contains a number',
      met: /\d/.test(passwordValue)
    }
  ]
})

const passwordStrength = computed(() => {
  const metCount = passwordChecklist.value.filter(item => item.met).length
  const passwordLength = passwordForm.newPassword.length

  if (!passwordLength) {
    return {
      label: 'Not started',
      feedback: 'Use 8+ characters with uppercase, lowercase, and a number.',
      score: 0,
      tone: 'muted',
      tagType: 'info' as const
    }
  }

  if (metCount <= 1) {
    return {
      label: 'Weak',
      feedback: 'Add more password rule coverage before saving.',
      score: 1,
      tone: 'weak',
      tagType: 'danger' as const
    }
  }

  if (metCount <= 3) {
    return {
      label: 'Moderate',
      feedback: 'Almost there. Complete the missing rules for a stronger password.',
      score: 2,
      tone: 'medium',
      tagType: 'warning' as const
    }
  }

  if (passwordLength >= 12) {
    return {
      label: 'Strong',
      feedback: 'This password meets all required rules with extra length.',
      score: 4,
      tone: 'strong',
      tagType: 'success' as const
    }
  }

  return {
    label: 'Good',
    feedback: 'All required rules are satisfied.',
    score: 3,
    tone: 'good',
    tagType: 'success' as const
  }
})

const hasPasswordInput = computed(() => {
  return Boolean(
    passwordForm.currentPassword.trim() ||
      passwordForm.newPassword.trim() ||
      passwordForm.confirmationPassword.trim()
  )
})

const canAttemptPasswordSave = computed(() => {
  return hasPasswordInput.value && !isSavingPassword.value
})

const getErrorMessage = (error: unknown, fallbackMessage: string): string => {
  if (
    typeof error === 'object' &&
    error !== null &&
    'response' in error &&
    typeof error.response === 'object' &&
    error.response !== null &&
    'data' in error.response &&
    typeof error.response.data === 'object' &&
    error.response.data !== null &&
    'message' in error.response.data &&
    typeof error.response.data.message === 'string' &&
    error.response.data.message.trim()
  ) {
    return error.response.data.message
  }

  return error instanceof Error && error.message ? error.message : fallbackMessage
}

const normalizeProfile = (profile: Partial<AccountProfile>): AccountProfile => {
  return {
    id: Number(profile.id ?? EMPTY_PROFILE.id),
    fullName: String(profile.fullName ?? '').trim(),
    email: String(profile.email ?? '').trim(),
    phoneNumber: String(profile.phoneNumber ?? '').trim(),
    avatarUrl: typeof profile.avatarUrl === 'string' ? profile.avatarUrl.trim() : '',
    status:
      typeof profile.status === 'string' && profile.status.trim()
        ? profile.status.trim().toUpperCase()
        : EMPTY_PROFILE.status
  }
}

const isCountryCode = (value: string): boolean => {
  return COUNTRY_CODE_OPTIONS.some(option => option.value === value)
}

const splitPhoneNumber = (phoneNumber: string): Pick<ProfileFormModel, 'countryCode' | 'localPhoneNumber'> => {
  const normalizedPhoneNumber = phoneNumber.trim()

  if (!normalizedPhoneNumber) {
    return {
      countryCode: DEFAULT_COUNTRY_CODE,
      localPhoneNumber: ''
    }
  }

  const matchedPhoneNumber = normalizedPhoneNumber.match(/^(\+\d{1,3})\s(.+)$/)

  if (matchedPhoneNumber) {
    return {
      countryCode: matchedPhoneNumber[1] || DEFAULT_COUNTRY_CODE,
      localPhoneNumber: matchedPhoneNumber[2] || ''
    }
  }

  return {
    countryCode: DEFAULT_COUNTRY_CODE,
    localPhoneNumber: normalizedPhoneNumber
  }
}

const buildPhoneNumber = (countryCode: string, localPhoneNumber: string): string => {
  const trimmedNumber = localPhoneNumber.trim()

  if (!trimmedNumber) {
    return ''
  }

  return `${countryCode.trim()} ${trimmedNumber}`.trim()
}

const formatTimestamp = (value: Date): string => {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  }).format(value)
}

const buildAvatarInitials = (value: string): string => {
  const parts = value
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)

  if (!parts.length) {
    return 'U'
  }

  return parts.map(part => part.charAt(0).toUpperCase()).join('')
}

const syncUserStoreProfile = (profile: AccountProfile): void => {
  const currentUser = userStore.userInfo
  const currentRole = userStore.userRole

  if (!currentUser || !currentRole) {
    return
  }

  userStore.setUserInfo(
    {
      ...currentUser,
      fullName: profile.fullName,
      email: profile.email,
      phoneNumber: profile.phoneNumber,
      avatar: profile.avatarUrl,
      nickname: profile.fullName || currentUser.nickname,
      username: currentUser.username || profile.email || `user-${profile.id}`
    },
    currentRole
  )
}

const populateProfileForm = (profile: AccountProfile): void => {
  const phoneParts = splitPhoneNumber(profile.phoneNumber)

  profileForm.fullName = profile.fullName
  profileForm.email = profile.email
  profileForm.countryCode =
    phoneParts.countryCode && (isCountryCode(phoneParts.countryCode) || COUNTRY_CODE_PATTERN.test(phoneParts.countryCode))
      ? phoneParts.countryCode
      : DEFAULT_COUNTRY_CODE
  profileForm.localPhoneNumber = phoneParts.localPhoneNumber
  profileFormRef.value?.clearValidate()
}

const resetPasswordForm = (clearNotice = true): void => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmationPassword = ''
  passwordFormRef.value?.clearValidate()

  if (clearNotice) {
    passwordNotice.value = null
  }
}

const resetProfileFormState = (): void => {
  profileForm.fullName = ''
  profileForm.email = ''
  profileForm.countryCode = DEFAULT_COUNTRY_CODE
  profileForm.localPhoneNumber = ''
  profileFormRef.value?.clearValidate()
}

const clearSensitiveAccountState = (): void => {
  originalProfile.value = null
  lastProfileSavedAt.value = null
  lastPasswordUpdatedAt.value = null
  highlightedSection.value = null
  avatarNotice.value = null
  profileNotice.value = null
  passwordNotice.value = null
  deactivationNotice.value = null
  resetProfileFormState()
  resetPasswordForm(false)
  viewState.value = 'loading'
}

const handleAuthenticationLoss = (): void => {
  shouldBypassUnsavedChangesPrompt.value = true
  clearSensitiveAccountState()
  userStore.token = null
  userStore.userInfo = null
  userStore.userRole = null
  clearAuthAndRedirect()
}

const validateRequiredText = (label: string) => {
  return (
    rule: unknown,
    value: unknown,
    callback: (error?: Error) => void
  ): void => {
    const trimmedValue = typeof value === 'string' ? value.trim() : ''

    if (!trimmedValue) {
      callback(new Error(`${label} is required.`))
      return
    }

    callback()
  }
}

const validatePhoneNumber = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void => {
  const localPhoneNumber = typeof value === 'string' ? value.trim() : ''

  if (!profileForm.countryCode.trim()) {
    callback(new Error('Phone number is required.'))
    return
  }

  if (!COUNTRY_CODE_PATTERN.test(profileForm.countryCode)) {
    callback(new Error('Please enter a valid phone number.'))
    return
  }

  if (!localPhoneNumber) {
    callback(new Error('Phone number is required.'))
    return
  }

  if (!LOCAL_PHONE_NUMBER_PATTERN.test(localPhoneNumber)) {
    callback(new Error('Phone number must contain digits only.'))
    return
  }

  const commonRegionRule = COMMON_REGION_PHONE_RULES[profileForm.countryCode]

  if (commonRegionRule && localPhoneNumber.length !== commonRegionRule.exactLength) {
    callback(new Error(commonRegionRule.message))
    return
  }

  if (!commonRegionRule && (localPhoneNumber.length < 4 || localPhoneNumber.length > 14)) {
    callback(new Error('Please enter a valid phone number.'))
    return
  }

  callback()
}

const validateNewPassword = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void => {
  const newPassword = typeof value === 'string' ? value.trim() : ''

  if (!newPassword.trim()) {
    callback(new Error('New password is required.'))
    return
  }

  if (passwordChecklist.value.every(item => item.met)) {
    callback()
    return
  }

  callback(
    new Error(
      'Password must be at least 8 characters and include uppercase, lowercase, and a number.'
    )
  )
}

const validateConfirmationPassword = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void => {
  const confirmationPassword = typeof value === 'string' ? value.trim() : ''

  if (!confirmationPassword) {
    callback(new Error('Confirmation password is required.'))
    return
  }

  if (confirmationPassword !== passwordForm.newPassword.trim()) {
    callback(new Error('Confirmation password must match the new password.'))
    return
  }

  callback()
}

const profileRules: FormRules<ProfileFormModel> = {
  fullName: [
    {
      validator: validateRequiredText('Full name'),
      trigger: 'blur'
    }
  ],
  email: [
    {
      validator: validateRequiredText('Email address'),
      trigger: 'blur'
    },
    {
      pattern: EMAIL_PATTERN,
      message: 'Please enter a valid email address.',
      trigger: 'blur'
    }
  ],
  localPhoneNumber: [
    {
      validator: validatePhoneNumber,
      trigger: ['blur', 'change']
    }
  ]
}

const passwordRules: FormRules<PasswordFormModel> = {
  currentPassword: [
    {
      validator: validateRequiredText('Current password'),
      trigger: 'blur'
    }
  ],
  newPassword: [
    {
      validator: validateNewPassword,
      trigger: 'blur'
    }
  ],
  confirmationPassword: [
    {
      validator: validateConfirmationPassword,
      trigger: 'blur'
    }
  ]
}

const highlightSectionCard = (section: SectionKey): void => {
  highlightedSection.value = section

  if (highlightTimer.value) {
    window.clearTimeout(highlightTimer.value)
  }

  highlightTimer.value = window.setTimeout(() => {
    highlightedSection.value = null
  }, 1800)
}

const getSectionElement = (section: SectionKey): HTMLElement | undefined => {
  const sectionMap: Record<SectionKey, HTMLElement | undefined> = {
    personal: personalSectionRef.value,
    contact: contactSectionRef.value,
    security: securitySectionRef.value,
    status: statusSectionRef.value
  }

  return sectionMap[section]
}

const scrollToSection = async (
  section: SectionKey,
  behavior: ScrollBehavior = 'smooth'
): Promise<void> => {
  await nextTick()

  const targetElement = getSectionElement(section)

  if (!targetElement) {
    return
  }

  targetElement.scrollIntoView({
    behavior,
    block: 'start'
  })

  highlightSectionCard(section)
}

const handleCountryCodeChange = (): void => {
  if (profileForm.localPhoneNumber.trim()) {
    void profileFormRef.value?.validateField('localPhoneNumber')
  }
}

const resetAvatarInput = (): void => {
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

const triggerAvatarSelection = (): void => {
  avatarNotice.value = null
  avatarInputRef.value?.click()
}

const validateAvatarSelection = (file: File): string | null => {
  const normalizedType = file.type.trim().toLowerCase()

  if (!ALLOWED_AVATAR_MIME_TYPES.includes(normalizedType)) {
    return 'Only JPG, PNG, and WEBP image files are supported.'
  }

  if (file.size > MAX_AVATAR_FILE_SIZE_BYTES) {
    return 'Avatar image must be 2 MB or smaller.'
  }

  return null
}

const handleAvatarFileChange = async (event: Event): Promise<void> => {
  const target = event.target as HTMLInputElement | null
  const selectedFile = target?.files?.[0]

  if (!selectedFile) {
    return
  }

  avatarNotice.value = null

  const validationMessage = validateAvatarSelection(selectedFile)
  if (validationMessage) {
    avatarNotice.value = {
      tone: 'error',
      title: 'Avatar upload rejected',
      message: validationMessage
    }
    resetAvatarInput()
    return
  }

  if (!hasApiSession.value) {
    resetAvatarInput()
    handleAuthenticationLoss()
    return
  }

  if (currentAccountStatus.value !== 'ACTIVE') {
    avatarNotice.value = {
      tone: 'warning',
      title: 'Avatar updates unavailable',
      message: 'Only active accounts can upload a new avatar.'
    }
    resetAvatarInput()
    return
  }

  isUploadingAvatar.value = true

  try {
    const response: AvatarUploadResponse = await uploadCurrentUserAvatar(selectedFile)

    if (!response.avatarUrl) {
      throw new Error('Avatar upload completed without a usable image URL.')
    }

    const updatedProfile = normalizeProfile({
      ...(originalProfile.value ?? EMPTY_PROFILE),
      avatarUrl: response.avatarUrl
    })

    originalProfile.value = updatedProfile
    syncUserStoreProfile(updatedProfile)
    lastProfileSavedAt.value = new Date()

    avatarNotice.value = {
      tone: 'success',
      title: 'Avatar updated successfully',
      message: 'Your new avatar was uploaded and saved to your profile.'
    }
  } catch (error) {
    if (isAuthFailureError(error)) {
      handleAuthenticationLoss()
      return
    }

    avatarNotice.value = {
      tone: 'error',
      title: 'Unable to upload avatar',
      message: getErrorMessage(error, 'Unable to upload your avatar right now.')
    }
  } finally {
    isUploadingAvatar.value = false
    resetAvatarInput()
  }
}

const loadDashboard = async (): Promise<void> => {
  viewState.value = 'loading'
  loadErrorMessage.value = 'We could not load your account settings.'
  avatarNotice.value = null
  profileNotice.value = null
  passwordNotice.value = null
  deactivationNotice.value = null

  try {
    if (!hasApiSession.value) {
      handleAuthenticationLoss()
      return
    }

    const normalizedProfile = normalizeProfile(await getCurrentUserProfile())

    originalProfile.value = normalizedProfile
    populateProfileForm(normalizedProfile)
    syncUserStoreProfile(normalizedProfile)

    viewState.value = 'ready'

    if (props.initialSection !== 'overview') {
      await scrollToSection(props.initialSection, 'auto')
    }
  } catch (error) {
    if (isAuthFailureError(error)) {
      handleAuthenticationLoss()
      return
    }

    loadErrorMessage.value = getErrorMessage(
      error,
      'We could not load your account settings.'
    )
    viewState.value = 'error'
  }
}

const saveProfile = async (): Promise<void> => {
  profileNotice.value = null

  if (!profileFormRef.value || !originalProfile.value) {
    return
  }

  if (!hasUnsavedProfileChanges.value) {
    profileNotice.value = {
      tone: 'info',
      title: 'No changes to save',
      message: 'Edit a profile field before saving, or keep the current values as they are.'
    }
    return
  }

  const isValid = await profileFormRef.value.validate().catch(() => false)

  if (isValid !== true) {
    profileNotice.value = {
      tone: 'error',
      title: 'Please review the highlighted fields',
      message: 'Correct the validation issues in your personal or contact information and try again.'
    }
    return
  }

  isSavingProfile.value = true

  try {
    const payload = trimmedProfileDraft.value
    await updateCurrentUserProfile(payload)

    const savedProfile = normalizeProfile({
      id: originalProfile.value.id,
      status: originalProfile.value.status,
      avatarUrl: originalProfile.value.avatarUrl,
      ...payload
    })

    originalProfile.value = savedProfile
    populateProfileForm(savedProfile)
    syncUserStoreProfile(savedProfile)
    lastProfileSavedAt.value = new Date()

    profileNotice.value = {
      tone: 'success',
      title: 'Profile saved successfully',
      message: 'Your personal and contact details were updated using the existing profile endpoint.'
    }
  } catch (error) {
    if (isAuthFailureError(error)) {
      handleAuthenticationLoss()
      return
    }

    profileNotice.value = {
      tone: 'error',
      title: 'Unable to save profile changes',
      message: getErrorMessage(error, 'Unable to save your profile right now.')
    }
  } finally {
    isSavingProfile.value = false
  }
}

const resetProfileForm = (): void => {
  if (!originalProfile.value) {
    return
  }

  populateProfileForm(originalProfile.value)
  profileNotice.value = {
    tone: 'info',
    title: 'Changes discarded',
    message: 'The form has been restored to the last saved profile data.'
  }
}

const deactivateAccount = async (): Promise<void> => {
  deactivationNotice.value = null

  if (!hasApiSession.value) {
    handleAuthenticationLoss()
    return
  }

  if (currentAccountStatus.value !== 'ACTIVE') {
    deactivationNotice.value = {
      tone: 'warning',
      title: 'Account cannot be deactivated again',
      message: 'This account is no longer in an active state.'
    }
    return
  }

  try {
    await ElMessageBox.confirm(
      'Deactivate this account? This is a soft deactivation: your data stays in the database, but normal account access should stop and you will be signed out immediately.',
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

  isDeactivatingAccount.value = true

  try {
    await deactivateCurrentUserAccount()

    const deactivatedProfile = normalizeProfile({
      ...(originalProfile.value ?? EMPTY_PROFILE),
      status: 'DEACTIVATED'
    })

    originalProfile.value = deactivatedProfile

    ElMessage.success('Account deactivated successfully. Redirecting to login.')
    shouldBypassUnsavedChangesPrompt.value = true
    clearAuthAndRedirect()
  } catch (error) {
    if (isAuthFailureError(error)) {
      handleAuthenticationLoss()
      return
    }

    deactivationNotice.value = {
      tone: 'error',
      title: 'Unable to deactivate account',
      message: getErrorMessage(error, 'Unable to deactivate your account right now.')
    }
  } finally {
    isDeactivatingAccount.value = false
  }
}

const savePassword = async (): Promise<void> => {
  passwordNotice.value = null

  if (!passwordFormRef.value) {
    return
  }

  const isValid = await passwordFormRef.value.validate().catch(() => false)

  if (isValid !== true) {
    passwordNotice.value = {
      tone: 'error',
      title: 'Password requirements not met',
      message: 'Review the rule checklist and fix the highlighted password fields before saving.'
    }
    return
  }

  isSavingPassword.value = true

  try {
    const payload: ChangePasswordPayload = {
      currentPassword: passwordForm.currentPassword.trim(),
      newPassword: passwordForm.newPassword.trim(),
      confirmationPassword: passwordForm.confirmationPassword.trim()
    }

    if (!hasApiSession.value) {
      handleAuthenticationLoss()
      return
    }

    await changeCurrentUserPassword(payload)
    lastPasswordUpdatedAt.value = new Date()
    ElMessage.success('Password updated successfully. Please log in again.')
    handleAuthenticationLoss()
    return
  } catch (error) {
    if (isAuthFailureError(error)) {
      handleAuthenticationLoss()
      return
    }

    passwordNotice.value = {
      tone: 'error',
      title: 'Unable to update password',
      message: getErrorMessage(error, 'Unable to update your password right now.')
    }
  } finally {
    isSavingPassword.value = false
  }
}

const goToStyleSettings = (): void => {
  void router.push('/customer/profile/style-settings')
}

const handleBeforeUnload = (event: BeforeUnloadEvent): void => {
  if (!hasUnsavedProfileChanges.value) {
    return
  }

  event.preventDefault()
  event.returnValue = ''
}

watch(
  () => passwordForm.newPassword,
  () => {
    if (passwordForm.confirmationPassword.trim()) {
      void passwordFormRef.value?.validateField('confirmationPassword')
    }
  }
)

onBeforeRouteLeave(async () => {
  if (shouldBypassUnsavedChangesPrompt.value) {
    return true
  }

  if (!hasUnsavedProfileChanges.value) {
    return true
  }

  try {
    await ElMessageBox.confirm(
      'You have unsaved profile changes. Leave this page without saving them?',
      'Unsaved changes',
      {
        type: 'warning',
        confirmButtonText: 'Leave page',
        cancelButtonText: 'Stay here'
      }
    )

    return true
  } catch {
    return false
  }
})

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  void loadDashboard()
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)

  if (highlightTimer.value) {
    window.clearTimeout(highlightTimer.value)
  }
})
</script>

<style scoped lang="scss">
.page-card {
  padding: var(--space-6);
  background:
    radial-gradient(circle at top right, rgba(51, 144, 251, 0.12), transparent 32%),
    var(--color-bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 12px 40px rgba(74, 80, 77, 0.08);
}

.settings-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.page-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
}

.page-tag {
  margin: 0 0 var(--space-2);
  color: var(--color-primary);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 2.1rem;
  line-height: 1.1;
}

.page-text {
  margin: var(--space-3) 0 0;
  max-width: 46rem;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.page-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
}

.page-meta__text {
  color: var(--color-text-secondary);
  font-size: 0.95rem;
  text-align: left;
}

.dashboard,
.loading-shell {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
}

.loading-card {
  border: 1px solid rgba(51, 144, 251, 0.18);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(51, 144, 251, 0.12), rgba(255, 255, 255, 0.95)),
    var(--color-bg-surface);
  box-shadow: 0 12px 30px rgba(74, 80, 77, 0.07);
}

.settings-card__eyebrow {
  margin: 0;
  color: var(--color-primary);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.settings-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-surface);
  box-shadow: 0 8px 24px rgba(74, 80, 77, 0.05);
  transition:
    border-color var(--transition-base),
    box-shadow var(--transition-base),
    transform var(--transition-base);
}

.settings-card--highlighted {
  border-color: rgba(51, 144, 251, 0.45);
  box-shadow: 0 14px 32px rgba(51, 144, 251, 0.18);
  transform: translateY(-2px);
}

.settings-card--danger {
  border-color: rgba(212, 92, 115, 0.24);
  background:
    linear-gradient(180deg, rgba(212, 92, 115, 0.06), transparent 46%),
    var(--color-bg-surface);
}

.settings-card--avatar {
  background:
    linear-gradient(180deg, rgba(51, 144, 251, 0.08), transparent 54%),
    var(--color-bg-surface);
}

.settings-card--actions {
  background:
    linear-gradient(180deg, rgba(211, 155, 46, 0.06), transparent 48%),
    var(--color-bg-surface);
}

.settings-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-5) var(--space-4);
  border-bottom: 1px solid rgba(216, 218, 215, 0.85);
}

.settings-card__title {
  margin: var(--space-2) 0 0;
  color: var(--color-text-primary);
  font-size: 1.35rem;
}

.settings-card__subtitle {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.settings-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-5);
}

.field-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-4);
}

.field-grid--single {
  grid-template-columns: minmax(0, 1fr);
}

.avatar-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  padding: var(--space-4);
  border: 1px solid rgba(216, 218, 215, 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.avatar-panel__preview {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-width: 0;
}

.avatar-panel__image {
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(51, 144, 251, 0.18), rgba(51, 144, 251, 0.34));
  color: var(--color-primary);
  font-size: 1.85rem;
  font-weight: 800;
}

.avatar-panel__details {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.avatar-panel__name {
  color: var(--color-text-primary);
  font-size: 1.15rem;
}

.avatar-panel__status,
.avatar-panel__meta,
.avatar-panel__hint {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.avatar-panel__actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
  max-width: 320px;
}

.section-note,
.danger-note {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.phone-field {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  gap: var(--space-3);
}

.contact-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-3);
}

.status-overview {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-3);
}

.contact-summary__item,
.status-overview__item {
  padding: var(--space-4);
  border: 1px solid rgba(216, 218, 215, 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.contact-summary__label,
.status-overview__label {
  display: block;
  margin-bottom: var(--space-2);
  color: var(--color-text-secondary);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.contact-summary__value,
.status-overview__value {
  color: var(--color-text-primary);
  font-size: 1rem;
}

.appearance-panel {
  padding: var(--space-4);
  border: 1px solid rgba(216, 218, 215, 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.appearance-panel__label {
  display: block;
  margin-bottom: var(--space-2);
  color: var(--color-text-secondary);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.appearance-panel__value {
  display: block;
  color: var(--color-text-primary);
  font-size: 1rem;
}

.appearance-panel__text {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.appearance-actions {
  display: flex;
}

.strength-panel {
  padding: var(--space-4);
  border: 1px solid rgba(216, 218, 215, 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.strength-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.strength-panel__label {
  display: block;
  margin-bottom: var(--space-1);
  color: var(--color-text-secondary);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.strength-panel__value {
  color: var(--color-text-primary);
  font-size: 1rem;
}

.strength-panel__hint {
  max-width: 220px;
  color: var(--color-text-secondary);
  font-size: 0.92rem;
  line-height: 1.5;
  text-align: right;
}

.strength-meter {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: var(--space-4);
}

.strength-meter__segment {
  height: 10px;
  border-radius: 999px;
  background: rgba(216, 218, 215, 0.9);
}

.strength-meter__segment--active.strength-meter__segment--weak {
  background: var(--color-danger);
}

.strength-meter__segment--active.strength-meter__segment--medium,
.strength-meter__segment--active.strength-meter__segment--good {
  background: var(--color-warning);
}

.strength-meter__segment--active.strength-meter__segment--strong {
  background: var(--color-success);
}

.rule-list {
  display: grid;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.rule-list__item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--color-text-secondary);
}

.rule-list__item--met {
  color: var(--color-text-primary);
}

.rule-list__indicator {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  border: 2px solid var(--color-border-strong);
  background: transparent;
  transition: all var(--transition-base);
}

.rule-list__item--met .rule-list__indicator {
  border-color: var(--color-success);
  background: var(--color-success);
  box-shadow: 0 0 0 4px rgba(58, 167, 109, 0.14);
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  border: 1px solid rgba(216, 218, 215, 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.form-footer--stacked {
  padding: 0;
  border: none;
  background: transparent;
}

.form-footer__meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  color: var(--color-text-secondary);
}

.form-footer__meta strong {
  color: var(--color-text-primary);
}

.form-footer__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.danger-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.danger-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid rgba(212, 92, 115, 0.18);
  border-radius: var(--radius-md);
  background: rgba(212, 92, 115, 0.05);
}

.danger-item__copy {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.danger-item__title-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.danger-item__title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1rem;
}

.danger-item__tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(212, 92, 115, 0.12);
  color: var(--color-danger);
  font-size: 0.82rem;
  font-weight: 700;
}

.danger-item__text {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.status-banner {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid rgba(216, 218, 215, 0.9);
  border-left-width: 4px;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
}

.status-banner--success {
  border-color: rgba(58, 167, 109, 0.22);
  border-left-color: var(--color-success);
  background: rgba(58, 167, 109, 0.07);
}

.status-banner--error {
  border-color: rgba(212, 92, 115, 0.2);
  border-left-color: var(--color-danger);
  background: rgba(212, 92, 115, 0.08);
}

.status-banner--info {
  border-color: rgba(91, 124, 240, 0.18);
  border-left-color: var(--color-info);
  background: rgba(91, 124, 240, 0.08);
}

.status-banner--warning {
  border-color: rgba(211, 155, 46, 0.2);
  border-left-color: var(--color-warning);
  background: rgba(211, 155, 46, 0.1);
}

.status-banner__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.status-banner__title {
  color: var(--color-text-primary);
}

.status-banner__text {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.state-card {
  padding: var(--space-8);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-surface);
  box-shadow: 0 8px 30px rgba(74, 80, 77, 0.08);
  text-align: center;
}

.state-title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1.35rem;
}

.state-text {
  margin: var(--space-3) auto 0;
  max-width: 36rem;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.state-actions {
  display: flex;
  justify-content: center;
  margin-top: var(--space-5);
}

.loading-card {
  padding: var(--space-5);
}

.skeleton-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.skeleton-text {
  width: 100%;
}

.skeleton-card__title {
  width: 180px;
}

.skeleton-card__text {
  width: 100%;
}

.skeleton-card__text--short {
  width: 72%;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

:deep(.el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.58);
}

:deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-soft));
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-radius: 12px;
  min-height: 44px;
  box-shadow: 0 0 0 1px rgba(216, 218, 215, 0.95) inset;
}

:deep(.el-form-item__label) {
  color: var(--color-text-primary);
  font-weight: 600;
}

@media (max-width: 768px) {
  .page-card {
    padding: var(--space-5);
  }

  .page-title {
    font-size: 1.75rem;
  }

  .settings-card__header,
  .avatar-panel,
  .form-footer,
  .strength-panel__header,
  .danger-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .field-grid,
  .contact-summary,
  .status-overview {
    grid-template-columns: minmax(0, 1fr);
  }

  .phone-field {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-footer__actions,
  .state-actions,
  .appearance-actions {
    width: 100%;
  }

  .avatar-panel__preview,
  .avatar-panel__actions {
    width: 100%;
  }

  .avatar-panel__actions {
    max-width: none;
  }

  .danger-item {
    align-items: stretch;
  }
}
</style>
