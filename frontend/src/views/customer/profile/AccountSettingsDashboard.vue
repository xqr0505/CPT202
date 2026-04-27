<template>
  <section class="page-card settings-page">
    <div class="page-header">
      <div>
        <p class="page-tag">Account</p>
        <h1 class="page-title">Account Settings</h1>
        <p class="page-text">
          Manage your profile details, contact methods, password, and account controls from one
          polished settings dashboard.
        </p>
      </div>

      <div class="page-meta">
        <el-tag :type="completenessPercentage === 100 ? 'success' : 'warning'" effect="light">
          {{ completenessPercentage === 100 ? 'Profile Ready' : 'Needs Attention' }}
        </el-tag>

        <div class="completeness-panel">
          <div class="completeness-panel__header">
            <strong>Profile completeness: {{ completenessPercentage }}%</strong>
            <span>{{ completenessStatusText }}</span>
          </div>
          <el-progress :percentage="completenessPercentage" :show-text="false" :stroke-width="10" />
          <p class="page-meta__text">{{ completenessHelperText }}</p>
        </div>

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
      <section
        ref="avatarSectionRef"
        class="settings-card settings-card--avatar"
        :class="{ 'settings-card--highlighted': highlightedSection === 'avatar' }"
      >
        <div class="settings-card__header">
          <div>
            <p class="settings-card__eyebrow">Avatar</p>
            <h2 class="settings-card__title">Profile photo</h2>
            <p class="settings-card__subtitle">
              Keep your account recognizable everywhere with a photo that is saved directly to your
              profile.
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
                <span class="avatar-panel__meta">{{ profileMetaText }}</span>
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
                Keep the name on your account current so your bookings and support records stay
                consistent.
              </p>
            </div>
            <el-tag effect="plain">{{ savedProfile.fullName ? 'On file' : 'Missing' }}</el-tag>
          </div>

          <div class="settings-card__body">
            <div
              v-if="personalNotice"
              class="status-banner"
              :class="`status-banner--${personalNotice.tone}`"
              aria-live="polite"
            >
              <div class="status-banner__body">
                <strong class="status-banner__title">{{ personalNotice.title }}</strong>
                <p class="status-banner__text">{{ personalNotice.message }}</p>
              </div>
            </div>

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
              This is the name shown across your account and recent profile activity.
            </p>

            <div class="module-actions">
              <span class="module-actions__hint">{{ personalActionHint }}</span>
              <CustomButton
                type="primary"
                :loading="isSavingName"
                :disabled="!canSaveName"
                @click="saveName"
              >
                {{ nameSaveButtonText }}
              </CustomButton>
            </div>
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
                Manage the email address and phone number used for sign-in, reminders, and account
                notices.
              </p>
            </div>
            <el-tag effect="plain">{{ contactTagText }}</el-tag>
          </div>

          <div class="settings-card__body">
            <div
              v-if="contactNotice"
              class="status-banner"
              :class="`status-banner--${contactNotice.tone}`"
              aria-live="polite"
            >
              <div class="status-banner__body">
                <strong class="status-banner__title">{{ contactNotice.title }}</strong>
                <p class="status-banner__text">{{ contactNotice.message }}</p>
              </div>
            </div>

            <div class="contact-block">
              <div class="contact-block__header">
                <div>
                  <strong class="contact-block__title">Email address</strong>
                  <p class="contact-block__text">
                    Changing your email requires a verification code and one more password check for
                    security.
                  </p>
                </div>
              </div>

              <el-form-item label="Email address" prop="email">
                <el-input
                  v-model="profileForm.email"
                  type="email"
                  placeholder="Enter your email address"
                />
              </el-form-item>

              <div
                v-if="emailChangeNotice"
                class="status-banner"
                :class="`status-banner--${emailChangeNotice.tone}`"
                aria-live="polite"
              >
                <div class="status-banner__body">
                  <strong class="status-banner__title">{{ emailChangeNotice.title }}</strong>
                  <p class="status-banner__text">{{ emailChangeNotice.message }}</p>
                </div>
              </div>

              <div class="email-verification-panel">
                <div class="email-verification-panel__header">
                  <div>
                    <span class="email-verification-panel__label">Email verification</span>
                    <p class="email-verification-panel__text">
                      Enter your new email, send a 6-digit code, and confirm the change here.
                    </p>
                  </div>

                  <CustomButton
                    type="primary"
                    :loading="isSendingEmailChangeCode"
                    :disabled="!canSendEmailChangeCode"
                    @click="sendEmailChangeCode"
                  >
                    {{ emailChangeSendButtonText }}
                  </CustomButton>
                </div>

                <div class="email-verification-panel__body">
                  <el-input
                    v-model="emailChangeVerificationCode"
                    maxlength="6"
                    placeholder="Enter 6-digit verification code"
                    inputmode="numeric"
                    :disabled="!hasPendingEmailChange"
                    @input="sanitizeEmailChangeCodeInput"
                  />

                  <p class="email-verification-panel__hint">{{ emailChangeHelperText }}</p>

                  <div class="module-actions module-actions--inline">
                    <span class="module-actions__hint">{{ emailChangeStatusText }}</span>
                    <CustomButton
                      type="primary"
                      :loading="isSavingEmail"
                      :disabled="!canConfirmEmailChange"
                      @click="confirmEmailChange"
                    >
                      Confirm Email Change
                    </CustomButton>
                  </div>
                </div>
              </div>
            </div>

            <div class="contact-block">
              <div class="contact-block__header">
                <div>
                  <strong class="contact-block__title">Phone number</strong>
                  <p class="contact-block__text">
                    Save a phone number you can receive reminders and follow-up notifications on.
                  </p>
                </div>
              </div>

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

              <div class="contact-summary">
                <div class="contact-summary__item">
                  <span class="contact-summary__label">Saved phone format</span>
                  <strong class="contact-summary__value">{{ savedPhoneDisplay }}</strong>
                </div>
                <div class="contact-summary__item">
                  <span class="contact-summary__label">Email verification status</span>
                  <strong class="contact-summary__value">{{ emailChangeStatusText }}</strong>
                </div>
                <div class="contact-summary__item">
                  <span class="contact-summary__label">Profile status</span>
                  <strong class="contact-summary__value">{{ profileMetaText }}</strong>
                </div>
              </div>

              <div class="module-actions">
                <span class="module-actions__hint">{{ phoneActionHint }}</span>
                <CustomButton
                  type="primary"
                  :loading="isSavingPhone"
                  :disabled="!canSavePhone"
                  @click="savePhone"
                >
                  Save Phone
                </CustomButton>
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
              Update your password with clear requirements and a confirmation step before the
              backend applies it.
            </p>
          </div>
          <el-tag :type="passwordStrength.tagType" effect="light">
            {{ passwordStrength.label }}
          </el-tag>
        </div>

        <div class="settings-card__body">
          <div class="remember-credentials-panel">
            <div class="remember-credentials-panel__info">
              <strong class="remember-credentials-panel__title">Allow device to remember login credentials</strong>
              <p class="remember-credentials-panel__text">
                Off: instantly clears saved login account.<br>
                On: only saves your email when you select "Remember Account" on next login (valid for 7 days).
              </p>
            </div>
            <el-switch
              v-model="rememberCredentialsAllowed"
              active-text="On"
              inactive-text="Off"
              @change="handleRememberCredentialsAllowedChange"
            />
          </div>

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

          <div class="module-actions">
            <span class="module-actions__hint">{{ passwordMetaText }}</span>
            <div class="module-actions__buttons">
              <CustomButton :disabled="!hasPasswordInput || isSavingPassword" @click="resetPasswordForm()">
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
            <p class="settings-card__eyebrow">Recent Security Activity</p>
            <h2 class="settings-card__title">Recent account timeline</h2>
            <p class="settings-card__subtitle">
              Review your latest account updates recorded by the backend activity log.
            </p>
          </div>
          <el-tag effect="plain">{{ securityActivityStatusText }}</el-tag>
        </div>

        <div class="settings-card__body">
          <div v-if="isLoadingSecurityActivity && !securityActivityTimelineGroups.length" class="activity-loading">
            <el-skeleton animated :rows="3" />
          </div>

          <div v-else-if="securityActivityNotice" class="activity-error">
            <strong class="activity-empty__title">{{ securityActivityNotice.title }}</strong>
            <p class="activity-empty__text">{{ securityActivityNotice.message }}</p>
            <div class="activity-actions">
              <CustomButton @click="loadSecurityActivity()">Retry</CustomButton>
            </div>
          </div>

          <div v-else-if="securityActivityTimelineGroups.length" class="activity-groups">
            <section
              v-for="group in securityActivityTimelineGroups"
              :key="group.label"
              class="activity-group"
            >
              <h3 class="activity-group__title">{{ group.label }}</h3>

              <div class="activity-timeline">
                <article
                  v-for="item in group.items"
                  :key="item.id"
                  class="activity-timeline__item"
                >
                  <span class="activity-timeline__marker" />
                  <div class="activity-timeline__content">
                    <div class="activity-timeline__header">
                      <strong class="activity-timeline__title">{{ item.title }}</strong>
                      <span class="activity-timeline__time">{{ item.time }}</span>
                    </div>
                    <p v-if="item.detail" class="activity-timeline__summary">{{ item.detail }}</p>
                  </div>
                </article>
              </div>
            </section>
          </div>

          <div v-else class="activity-empty">
            <strong class="activity-empty__title">No recent security activity yet</strong>
            <p class="activity-empty__text">
              Profile, password, avatar, and account-status updates will appear here as soon as
              they are recorded.
            </p>
          </div>
        </div>
      </section>

      <section class="settings-card">
        <div class="settings-card__header">
          <div>
            <p class="settings-card__eyebrow">Appearance</p>
            <h2 class="settings-card__title">Style settings</h2>
            <p class="settings-card__subtitle">
              Open your style preferences without mixing them into your saved account profile.
            </p>
          </div>
          <el-tag effect="plain">{{ currentPageStyleText }}</el-tag>
        </div>

        <div class="settings-card__body">
          <div class="appearance-panel">
            <span class="appearance-panel__label">Current appearance</span>
            <strong class="appearance-panel__value">{{ currentPageStyleText }}</strong>
            <p class="appearance-panel__text">
              Your account details are saved separately. Appearance changes only affect how the
              interface looks for you.
            </p>
          </div>

          <div class="appearance-actions">
            <CustomButton @click="goToStyleSettings">Open Style Settings</CustomButton>
          </div>
        </div>
      </section>

      <el-dialog
        v-model="isDiffDialogOpen"
        :title="pendingAction?.title || 'Confirm changes'"
        width="min(640px, calc(100vw - 32px))"
        destroy-on-close
        :close-on-click-modal="!isPendingActionBusy"
        :close-on-press-escape="!isPendingActionBusy"
        @closed="handleDiffDialogClosed"
      >
        <div class="change-summary-dialog">
          <p class="change-summary-dialog__text">You are about to update:</p>

          <div class="diff-list">
            <div
              v-for="item in pendingActionDiffItems"
              :key="item.key"
              class="diff-list__item"
            >
              <span class="diff-list__bullet">•</span>
              <div class="diff-list__content">
                <strong>{{ item.label }}</strong>
                <span>
                  {{ item.previousDisplayValue }}
                  <span class="diff-list__arrow">-></span>
                  {{ item.nextDisplayValue }}
                </span>
              </div>
            </div>
          </div>

          <p class="change-summary-dialog__text">Confirm changes?</p>

          <div
            v-if="pendingActionDialogError"
            class="status-banner status-banner--error"
            aria-live="polite"
          >
            <div class="status-banner__body">
              <strong class="status-banner__title">Unable to continue</strong>
              <p class="status-banner__text">{{ pendingActionDialogError }}</p>
            </div>
          </div>
        </div>

        <template #footer>
          <div class="change-summary-dialog__actions">
            <CustomButton :disabled="isPendingActionBusy" @click="closePendingActionDialogs">
              Cancel
            </CustomButton>
            <CustomButton
              type="primary"
              :loading="isPendingActionBusy"
              @click="continuePendingAction"
            >
              {{ pendingAction?.confirmButtonText || 'Confirm changes' }}
            </CustomButton>
          </div>
        </template>
      </el-dialog>

      <el-dialog
        v-model="isReauthDialogOpen"
        :title="pendingAction?.passwordTitle || 'Re-enter current password'"
        width="min(520px, calc(100vw - 32px))"
        destroy-on-close
        :close-on-click-modal="!isPendingActionBusy"
        :close-on-press-escape="!isPendingActionBusy"
        @closed="handleReauthDialogClosed"
      >
        <div class="change-summary-dialog">
          <p class="change-summary-dialog__text">
            {{
              pendingAction?.passwordDescription ||
              'For security, please enter your current password before continuing.'
            }}
          </p>

          <div class="dialog-field">
            <label class="dialog-field__label" for="reauth-current-password">
              Current password
            </label>
            <el-input
              id="reauth-current-password"
              v-model="reauthCurrentPassword"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="Enter your current password"
              @keyup.enter="verifyAndRunPendingAction"
            />
          </div>

          <div
            v-if="reauthDialogError"
            class="status-banner status-banner--error"
            aria-live="polite"
          >
            <div class="status-banner__body">
              <strong class="status-banner__title">Unable to continue</strong>
              <p class="status-banner__text">{{ reauthDialogError }}</p>
            </div>
          </div>
        </div>

        <template #footer>
          <div class="change-summary-dialog__actions">
            <CustomButton :disabled="isPendingActionBusy" @click="closePendingActionDialogs">
              Cancel
            </CustomButton>
            <CustomButton
              type="primary"
              :loading="isPendingActionBusy"
              @click="verifyAndRunPendingAction"
            >
              Verify and Continue
            </CustomButton>
          </div>
        </template>
      </el-dialog>

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
              Deactivating your account is a soft change to account status. Your data stays in the
              database, but normal access should stop after confirmation.
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
                <span class="danger-item__tag">Sensitive action</span>
              </div>
              <p class="danger-item__text">
                Your account will be marked as deactivated and you will be signed out immediately.
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

          <p class="danger-note">{{ dangerZoneHelperText }}</p>
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
  changeCurrentUserEmail,
  changeCurrentUserPassword,
  deactivateCurrentUserAccount,
  getCurrentUserProfile,
  getCurrentUserSecurityActivities,
  sendCurrentUserEmailChangeCode,
  updateCurrentUserProfile,
  uploadCurrentUserAvatar,
  verifyCurrentUserPassword,
  type AccountProfile,
  type AvatarUploadResponse,
  type ChangeCurrentUserEmailPayload,
  type ChangePasswordPayload,
  type SecurityActivityItem,
  type SendEmailChangeCodePayload,
  type UpdateUserProfilePayload
} from '@/api/user'
import {
  getAuthToken,
  isAuthFailureError,
  logout as clearAuthAndRedirect
} from '@/api/request'
import { useUserStore } from '@/stores/user'
import { clearRememberedCredentials, isRememberCredentialsAllowed, setRememberCredentialsAllowed } from '@/utils/rememberCredentials'

defineOptions({ name: 'AccountSettingsDashboard' })

type ViewState = 'loading' | 'ready' | 'error'
type StatusTone = 'success' | 'error' | 'info' | 'warning'
type SectionKey = 'avatar' | 'personal' | 'contact' | 'security' | 'status'
type PendingActionKind = 'avatar' | 'name' | 'phone' | 'email' | 'password' | 'deactivate'
type SyncFieldKey = 'fullName' | 'email' | 'phoneNumber'

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
  key: string
  missingLabel: string
  complete: boolean
}

interface DiffItem {
  key: string
  label: string
  previousDisplayValue: string
  nextDisplayValue: string
}

interface PendingAction {
  kind: PendingActionKind
  title: string
  confirmButtonText: string
  requiresPassword: boolean
  fallbackErrorMessage: string
  passwordTitle?: string
  passwordDescription?: string
  diffItems: DiffItem[]
  execute: (currentPassword?: string) => Promise<void>
}

interface SecurityActivityTimelineItem {
  id: string
  title: string
  detail: string
  time: string
}

interface SecurityActivityTimelineGroup {
  label: string
  items: SecurityActivityTimelineItem[]
}

const props = withDefaults(defineProps<Props>(), {
  initialSection: 'overview'
})

const DEFAULT_COUNTRY_CODE = '+86'
const CURRENT_PASSWORD_INCORRECT_MESSAGE = 'Current password is incorrect.'
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
const personalNotice = ref<SectionNotice | null>(null)
const contactNotice = ref<SectionNotice | null>(null)
const emailChangeNotice = ref<SectionNotice | null>(null)
const passwordNotice = ref<SectionNotice | null>(null)
const deactivationNotice = ref<SectionNotice | null>(null)
const securityActivityNotice = ref<SectionNotice | null>(null)
const rememberCredentialsAllowed = ref(isRememberCredentialsAllowed())

const isUploadingAvatar = ref(false)
const isSavingName = ref(false)
const isSavingPhone = ref(false)
const isSavingEmail = ref(false)
const isSavingPassword = ref(false)
const isDeactivatingAccount = ref(false)
const isLoadingSecurityActivity = ref(false)
const isSendingEmailChangeCode = ref(false)
const isVerifyingCurrentPassword = ref(false)

const isDiffDialogOpen = ref(false)
const isReauthDialogOpen = ref(false)
const pendingAction = ref<PendingAction | null>(null)
const pendingActionDialogError = ref('')
const reauthDialogError = ref('')
const reauthCurrentPassword = ref('')
const pendingAvatarFile = ref<File | null>(null)

const emailChangeVerificationCode = ref('')
const emailChangeTargetEmail = ref('')
const emailChangeCountdown = ref(0)
const originalProfile = ref<AccountProfile | null>(null)
const securityActivityItems = ref<SecurityActivityItem[]>([])
const lastProfileSavedAt = ref<Date | null>(null)
const lastPasswordUpdatedAt = ref<Date | null>(null)
const highlightedSection = ref<SectionKey | null>(null)
const highlightTimer = ref<number | null>(null)
const emailChangeCountdownTimer = ref<number | null>(null)
const shouldBypassUnsavedChangesPrompt = ref(false)

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

const handleRememberCredentialsAllowedChange = (value: boolean): void => {
  const allowed = Boolean(value)
  setRememberCredentialsAllowed(allowed)
  rememberCredentialsAllowed.value = allowed

  if (!allowed) {
    ElMessage.success('Cleared saved login credentials on this device.')
  }
}
const avatarInputRef = ref<HTMLInputElement>()

const avatarSectionRef = ref<HTMLElement>()
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
const savedProfile = computed<AccountProfile>(() => originalProfile.value ?? EMPTY_PROFILE)
const currentAccountStatus = computed(() => savedProfile.value.status)
const currentAvatarUrl = computed(() => savedProfile.value.avatarUrl?.trim() || '')

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

const avatarTagText = computed(() => (currentAvatarUrl.value ? 'Custom avatar' : 'Default avatar'))
const avatarDisplayName = computed(() => {
  return (
    profileForm.fullName.trim() ||
    savedProfile.value.fullName.trim() ||
    userStore.userInfo?.nickname?.trim() ||
    userStore.userInfo?.username?.trim() ||
    'Account profile'
  )
})
const avatarInitials = computed(() => buildAvatarInitials(avatarDisplayName.value))
const avatarSummaryText = computed(() => {
  return currentAvatarUrl.value
    ? 'Your saved avatar is ready across your account.'
    : 'Upload a photo to complete your profile and make your account easier to recognize.'
})
const canUploadAvatar = computed(() => {
  return hasApiSession.value && currentAccountStatus.value === 'ACTIVE' && !isUploadingAvatar.value
})
const avatarHelperText = computed(() => {
  if (!hasApiSession.value) {
    return 'An authenticated session is required to upload a profile photo.'
  }

  if (currentAccountStatus.value !== 'ACTIVE') {
    return 'Only active accounts can upload a new profile photo.'
  }

  return 'JPG, PNG, or WEBP up to 2 MB. Uploading saves the new photo to your profile.'
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

const trimmedFullName = computed(() => profileForm.fullName.trim())
const trimmedDraftEmail = computed(() => profileForm.email.trim())
const composedPhonePreview = computed(() => buildPhoneNumber(profileForm.countryCode, profileForm.localPhoneNumber))
const savedPhoneDisplay = computed(() => savedProfile.value.phoneNumber || 'Not provided')

const normalizedSavedEmail = computed(() => normalizeEmail(savedProfile.value.email))
const normalizedDraftEmail = computed(() => normalizeEmail(trimmedDraftEmail.value))
const isNameDirty = computed(() => trimmedFullName.value !== savedProfile.value.fullName)
const isPhoneDirty = computed(() => composedPhonePreview.value !== savedProfile.value.phoneNumber)
const hasPendingEmailChange = computed(() => normalizedDraftEmail.value !== normalizedSavedEmail.value)
const hasUnsavedProfileChanges = computed(() => {
  return isNameDirty.value || isPhoneDirty.value || hasPendingEmailChange.value
})

const isPendingEmailDraftValid = computed(() => {
  return hasPendingEmailChange.value && EMAIL_PATTERN.test(trimmedDraftEmail.value)
})

const isEmailChangeCodeBoundToCurrentDraft = computed(() => {
  return Boolean(emailChangeTargetEmail.value) && normalizedDraftEmail.value === emailChangeTargetEmail.value
})

const isEmailChangeCodeReady = computed(() => {
  return (
    hasPendingEmailChange.value &&
    isEmailChangeCodeBoundToCurrentDraft.value &&
    emailChangeVerificationCode.value.trim().length === 6
  )
})

const canSaveName = computed(() => {
  return hasApiSession.value && currentAccountStatus.value === 'ACTIVE' && isNameDirty.value && !isSavingName.value
})
const canSavePhone = computed(() => {
  return hasApiSession.value && currentAccountStatus.value === 'ACTIVE' && isPhoneDirty.value && !isSavingPhone.value
})
const canSendEmailChangeCode = computed(() => {
  return (
    hasApiSession.value &&
    currentAccountStatus.value === 'ACTIVE' &&
    !isSendingEmailChangeCode.value &&
    isPendingEmailDraftValid.value &&
    emailChangeCountdown.value === 0
  )
})
const canConfirmEmailChange = computed(() => {
  return (
    hasApiSession.value &&
    currentAccountStatus.value === 'ACTIVE' &&
    !isSavingEmail.value &&
    isEmailChangeCodeReady.value
  )
})
const canDeactivateAccount = computed(() => {
  return hasApiSession.value && currentAccountStatus.value === 'ACTIVE' && !isDeactivatingAccount.value
})

const nameSaveButtonText = computed(() => {
  return savedProfile.value.fullName ? 'Confirm Name Change' : 'Save Name'
})
const personalActionHint = computed(() => {
  return isNameDirty.value
    ? 'Review your updated name, then confirm the change.'
    : 'Your saved name is already in sync.'
})
const phoneActionHint = computed(() => {
  return isPhoneDirty.value
    ? 'Review the updated phone number before saving it.'
    : 'Save a phone number here when you are ready.'
})

const profileCompletionItems = computed<CompletionItem[]>(() => {
  return [
    {
      key: 'fullName',
      missingLabel: 'full name',
      complete: Boolean(savedProfile.value.fullName.trim())
    },
    {
      key: 'email',
      missingLabel: 'email',
      complete: Boolean(savedProfile.value.email.trim())
    },
    {
      key: 'phoneNumber',
      missingLabel: 'phone number',
      complete: Boolean(savedProfile.value.phoneNumber.trim())
    },
    {
      key: 'avatarUrl',
      missingLabel: 'avatar',
      complete: Boolean(savedProfile.value.avatarUrl.trim())
    }
  ]
})

const completenessPercentage = computed(() => {
  return profileCompletionItems.value.filter(item => item.complete).length * 25
})
const completenessStatusText = computed(() => `${completenessPercentage.value}% Complete`)
const completenessHelperText = computed(() => {
  if (completenessPercentage.value === 100) {
    return 'All key profile fields are complete.'
  }

  const missingItems = profileCompletionItems.value
    .filter(item => !item.complete)
    .map(item => item.missingLabel)

  return `Complete your profile by adding ${joinLabels(missingItems)}.`
})

const profileMetaText = computed(() => {
  if (currentAccountStatus.value === 'DEACTIVATED') {
    return 'Your account is currently deactivated.'
  }

  if (hasPendingEmailChange.value) {
    return 'A new email is waiting for verification.'
  }

  if (hasUnsavedProfileChanges.value) {
    return 'You have unsaved changes in your account settings.'
  }

  if (lastProfileSavedAt.value) {
    return 'Saved successfully.'
  }

  return 'Your profile is up to date.'
})

const contactTagText = computed(() => {
  return savedProfile.value.email && savedProfile.value.phoneNumber ? 'Reachable' : 'Review needed'
})

const passwordMetaText = computed(() => {
  if (lastPasswordUpdatedAt.value) {
    return `Password updated ${formatTimestamp(lastPasswordUpdatedAt.value)}`
  }

  return 'Your current password is required before a new one can be applied.'
})

const emailChangeSendButtonText = computed(() => {
  if (emailChangeCountdown.value > 0 && isEmailChangeCodeBoundToCurrentDraft.value) {
    return `${emailChangeCountdown.value}s to resend`
  }

  return 'Send Code'
})

const emailChangeStatusText = computed(() => {
  if (!hasPendingEmailChange.value) {
    return 'No verification pending'
  }

  if (isEmailChangeCodeReady.value) {
    return 'Code ready for confirmation'
  }

  if (isEmailChangeCodeBoundToCurrentDraft.value) {
    return 'Code sent, waiting for confirmation'
  }

  return 'Code not sent'
})

const emailChangeHelperText = computed(() => {
  if (!hasPendingEmailChange.value) {
    return 'Enter a different email address, send a code, and confirm the change here.'
  }

  if (!EMAIL_PATTERN.test(trimmedDraftEmail.value)) {
    return 'Enter a valid email address before requesting a verification code.'
  }

  if (isEmailChangeCodeReady.value) {
    return `A valid code is ready for ${trimmedDraftEmail.value}. Confirm the email change to save it.`
  }

  if (isEmailChangeCodeBoundToCurrentDraft.value && emailChangeCountdown.value > 0) {
    return `A verification code was sent to ${trimmedDraftEmail.value}. Enter it here, then confirm the change.`
  }

  return `Send a verification code to ${trimmedDraftEmail.value}, enter it here, and then confirm the email change.`
})

const securityActivityStatusText = computed(() => {
  if (isLoadingSecurityActivity.value && !securityActivityItems.value.length) {
    return 'Loading'
  }

  if (securityActivityNotice.value) {
    return 'Retry available'
  }

  if (!securityActivityItems.value.length) {
    return 'No recent activity'
  }

  return `${securityActivityItems.value.length} recent record${securityActivityItems.value.length === 1 ? '' : 's'}`
})

const securityActivityTimelineGroups = computed<SecurityActivityTimelineGroup[]>(() => {
  const groupMap = new Map<string, SecurityActivityTimelineItem[]>()

  securityActivityItems.value.forEach(item => {
    const label = formatRelativeDayLabel(item.createdAt)
    const existingItems = groupMap.get(label) ?? []
    existingItems.push({
      id: `${item.id}-${item.createdAt}`,
      title: item.description || getSecurityActivityFallbackLabel(item.activityType),
      detail: buildSecurityActivityDetail(item),
      time: formatActivityTime(item.createdAt)
    })
    groupMap.set(label, existingItems)
  })

  return Array.from(groupMap.entries()).map(([label, items]) => ({
    label,
    items
  }))
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

const pendingActionDiffItems = computed<DiffItem[]>(() => pendingAction.value?.diffItems ?? [])
const isPendingActionBusy = computed(() => {
  if (isVerifyingCurrentPassword.value) {
    return true
  }

  switch (pendingAction.value?.kind) {
    case 'avatar':
      return isUploadingAvatar.value
    case 'name':
      return isSavingName.value
    case 'phone':
      return isSavingPhone.value
    case 'email':
      return isSavingEmail.value
    case 'password':
      return isSavingPassword.value
    case 'deactivate':
      return isDeactivatingAccount.value
    default:
      return false
  }
})

const dangerZoneHelperText = computed(() => {
  if (!hasApiSession.value) {
    return 'An authenticated session is required to deactivate this account.'
  }

  if (currentAccountStatus.value === 'DEACTIVATED') {
    return 'This account is already marked as deactivated.'
  }

  return 'A password check is required before your account status can be changed to deactivated.'
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

const normalizeEmail = (value: string | undefined | null): string => {
  return typeof value === 'string' ? value.trim().toLowerCase() : ''
}

const toDisplayDate = (value: Date | string): Date | null => {
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }

  const normalizedValue = value.trim()

  if (!normalizedValue) {
    return null
  }

  const parsedDate = new Date(
    normalizedValue.includes('T') ? normalizedValue : normalizedValue.replace(' ', 'T')
  )

  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate
}

const formatTimestamp = (value: Date | string): string => {
  const parsedDate = toDisplayDate(value)

  if (!parsedDate) {
    return 'just now'
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  }).format(parsedDate)
}

const formatActivityTime = (value: Date | string): string => {
  const parsedDate = toDisplayDate(value)

  if (!parsedDate) {
    return 'Unknown time'
  }

  return new Intl.DateTimeFormat(undefined, {
    hour: 'numeric',
    minute: '2-digit'
  }).format(parsedDate)
}

const formatRelativeDayLabel = (value: Date | string): string => {
  const parsedDate = toDisplayDate(value)

  if (!parsedDate) {
    return 'Earlier'
  }

  const today = new Date()
  const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const startOfTarget = new Date(parsedDate.getFullYear(), parsedDate.getMonth(), parsedDate.getDate())
  const diffDays = Math.round((startOfToday.getTime() - startOfTarget.getTime()) / (24 * 60 * 60 * 1000))

  if (diffDays <= 0) {
    return 'Today'
  }

  if (diffDays === 1) {
    return 'Yesterday'
  }

  if (diffDays < 7) {
    return `${diffDays} days ago`
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: parsedDate.getFullYear() === today.getFullYear() ? undefined : 'numeric'
  }).format(parsedDate)
}

const formatDiffValue = (value: string): string => {
  return value.trim() ? value.trim() : 'Not provided'
}

const buildDiffItem = (key: string, label: string, previousValue: string, nextValue: string): DiffItem => {
  return {
    key,
    label,
    previousDisplayValue: formatDiffValue(previousValue),
    nextDisplayValue: formatDiffValue(nextValue)
  }
}

const joinLabels = (items: string[]): string => {
  if (!items.length) {
    return 'the remaining details'
  }

  if (items.length === 1) {
    return items[0] ?? 'the remaining details'
  }

  if (items.length === 2) {
    return `${items[0] ?? ''} and ${items[1] ?? ''}`
  }

  return `${items.slice(0, -1).join(', ')}, and ${items[items.length - 1] ?? ''}`
}

const mapChangedFieldLabel = (value: string): string => {
  switch (value.trim()) {
    case 'fullName':
      return 'full name'
    case 'email':
      return 'email'
    case 'phoneNumber':
      return 'phone number'
    case 'avatarUrl':
      return 'avatar'
    case 'password':
      return 'password'
    case 'status':
      return 'account status'
    default:
      return value
  }
}

const getSecurityActivityFallbackLabel = (activityType: string): string => {
  switch (activityType.trim().toUpperCase()) {
    case 'PROFILE_UPDATED':
      return 'Profile updated'
    case 'EMAIL_CHANGED':
      return 'Email changed'
    case 'PHONE_CHANGED':
      return 'Phone number updated'
    case 'PASSWORD_CHANGED':
      return 'Password changed'
    case 'AVATAR_UPDATED':
      return 'Avatar updated'
    case 'ACCOUNT_DEACTIVATED':
      return 'Account deactivated'
    default:
      return 'Account activity'
  }
}

const buildSecurityActivityDetail = (item: SecurityActivityItem): string => {
  if (!item.changedFields.length) {
    return ''
  }

  return `Changed: ${joinLabels(item.changedFields.map(mapChangedFieldLabel))}.`
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
  const currentRole = userStore.userRole

  if (!currentRole) {
    return
  }

  userStore.setUserInfo(
    {
      ...(userStore.userInfo ?? {
        id: profile.id,
        username: '',
        nickname: ''
      }),
      fullName: profile.fullName,
      email: profile.email,
      phoneNumber: profile.phoneNumber,
      avatar: profile.avatarUrl,
      nickname:
        profile.fullName ||
        userStore.userInfo?.nickname ||
        profile.email ||
        `user-${profile.id}`,
      username:
        profile.email ||
        userStore.userInfo?.username ||
        `user-${profile.id}`
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

const syncFormFieldFromProfile = (field: SyncFieldKey, profile: AccountProfile): void => {
  if (field === 'fullName') {
    profileForm.fullName = profile.fullName
    void profileFormRef.value?.clearValidate('fullName')
    return
  }

  if (field === 'email') {
    profileForm.email = profile.email
    void profileFormRef.value?.clearValidate('email')
    return
  }

  const phoneParts = splitPhoneNumber(profile.phoneNumber)
  profileForm.countryCode =
    phoneParts.countryCode && (isCountryCode(phoneParts.countryCode) || COUNTRY_CODE_PATTERN.test(phoneParts.countryCode))
      ? phoneParts.countryCode
      : DEFAULT_COUNTRY_CODE
  profileForm.localPhoneNumber = phoneParts.localPhoneNumber
  void profileFormRef.value?.clearValidate('localPhoneNumber')
}

const applySavedProfilePatch = (
  patch: Partial<AccountProfile>,
  fieldsToSync: SyncFieldKey[] = []
): AccountProfile => {
  const updatedProfile = normalizeProfile({
    ...savedProfile.value,
    ...patch
  })

  originalProfile.value = updatedProfile
  fieldsToSync.forEach(field => syncFormFieldFromProfile(field, updatedProfile))
  syncUserStoreProfile(updatedProfile)
  lastProfileSavedAt.value = new Date()
  return updatedProfile
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

const clearEmailChangeCountdown = (): void => {
  if (emailChangeCountdownTimer.value) {
    window.clearInterval(emailChangeCountdownTimer.value)
    emailChangeCountdownTimer.value = null
  }

  emailChangeCountdown.value = 0
}

const resetEmailChangeFlow = (clearNotice = true): void => {
  emailChangeVerificationCode.value = ''
  emailChangeTargetEmail.value = ''
  clearEmailChangeCountdown()

  if (clearNotice) {
    emailChangeNotice.value = null
  }
}

const startEmailChangeCountdown = (): void => {
  clearEmailChangeCountdown()
  emailChangeCountdown.value = 60
  emailChangeCountdownTimer.value = window.setInterval(() => {
    if (emailChangeCountdown.value <= 1) {
      clearEmailChangeCountdown()
      return
    }

    emailChangeCountdown.value -= 1
  }, 1000)
}

const resetPendingActionState = (): void => {
  pendingAction.value = null
  pendingActionDialogError.value = ''
  reauthDialogError.value = ''
  reauthCurrentPassword.value = ''
  pendingAvatarFile.value = null
}

const closePendingActionDialogs = (): void => {
  if (isPendingActionBusy.value) {
    return
  }

  isDiffDialogOpen.value = false
  isReauthDialogOpen.value = false
  resetPendingActionState()
}

const handleDiffDialogClosed = (): void => {
  if (!isReauthDialogOpen.value && !isPendingActionBusy.value) {
    resetPendingActionState()
  }
}

const handleReauthDialogClosed = (): void => {
  if (!isDiffDialogOpen.value && !isPendingActionBusy.value) {
    resetPendingActionState()
  }
}

const resetSecurityActivityState = (): void => {
  securityActivityItems.value = []
  securityActivityNotice.value = null
  isLoadingSecurityActivity.value = false
}

const clearSensitiveAccountState = (): void => {
  originalProfile.value = null
  resetSecurityActivityState()
  lastProfileSavedAt.value = null
  lastPasswordUpdatedAt.value = null
  highlightedSection.value = null
  avatarNotice.value = null
  personalNotice.value = null
  contactNotice.value = null
  emailChangeNotice.value = null
  passwordNotice.value = null
  deactivationNotice.value = null
  isSendingEmailChangeCode.value = false
  isDiffDialogOpen.value = false
  isReauthDialogOpen.value = false
  resetPendingActionState()
  resetEmailChangeFlow(false)
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

const signOutAfterSensitiveAction = async (): Promise<void> => {
  shouldBypassUnsavedChangesPrompt.value = true
  clearSensitiveAccountState()
  await userStore.logout()
  await router.replace('/login').catch(() => undefined)
}

const validateOptionalFullName = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void => {
  const trimmedValue = typeof value === 'string' ? value.trim() : ''

  if (trimmedValue.length > 60) {
    callback(new Error('Full name must be 60 characters or fewer.'))
    return
  }

  callback()
}

const validateEmail = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void => {
  const trimmedValue = typeof value === 'string' ? value.trim() : ''

  if (!trimmedValue) {
    callback(new Error('Email address is required.'))
    return
  }

  if (!EMAIL_PATTERN.test(trimmedValue)) {
    callback(new Error('Please enter a valid email address.'))
    return
  }

  callback()
}

const validateOptionalPhoneNumber = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void => {
  const localPhoneNumber = typeof value === 'string' ? value.trim() : ''

  if (!localPhoneNumber) {
    callback()
    return
  }

  if (!COUNTRY_CODE_PATTERN.test(profileForm.countryCode)) {
    callback(new Error('Please enter a valid phone number.'))
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

const validateNewPassword = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void => {
  const newPassword = typeof value === 'string' ? value.trim() : ''

  if (!newPassword) {
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
      validator: validateOptionalFullName,
      trigger: 'blur'
    }
  ],
  email: [
    {
      validator: validateEmail,
      trigger: 'blur'
    }
  ],
  localPhoneNumber: [
    {
      validator: validateOptionalPhoneNumber,
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
    avatar: avatarSectionRef.value,
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

const sanitizeEmailChangeCodeInput = (value: string | number): void => {
  emailChangeVerificationCode.value = String(value ?? '')
    .replace(/\D+/g, '')
    .slice(0, 6)
}

const resetAvatarInput = (): void => {
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

const openPendingAction = (action: PendingAction): void => {
  pendingAction.value = action
  pendingActionDialogError.value = ''
  reauthDialogError.value = ''
  reauthCurrentPassword.value = ''
  isDiffDialogOpen.value = true
}

const executePendingAction = async (currentPassword?: string): Promise<void> => {
  if (!pendingAction.value) {
    return
  }

  const activeAction = pendingAction.value

  try {
    await activeAction.execute(currentPassword)
    isDiffDialogOpen.value = false
    isReauthDialogOpen.value = false
    resetPendingActionState()
  } catch (error) {
    if (isAuthFailureError(error)) {
      isDiffDialogOpen.value = false
      isReauthDialogOpen.value = false
      resetPendingActionState()
      handleAuthenticationLoss()
      return
    }

    const message = getErrorMessage(error, activeAction.fallbackErrorMessage)

    if (isReauthDialogOpen.value) {
      reauthDialogError.value = message
      return
    }

    pendingActionDialogError.value = message
  }
}

const continuePendingAction = async (): Promise<void> => {
  pendingActionDialogError.value = ''

  if (!pendingAction.value) {
    return
  }

  if (pendingAction.value.requiresPassword) {
    isReauthDialogOpen.value = true
    isDiffDialogOpen.value = false
    reauthDialogError.value = ''
    reauthCurrentPassword.value = ''
    return
  }

  await executePendingAction()
}

const verifyAndRunPendingAction = async (): Promise<void> => {
  if (!pendingAction.value) {
    return
  }

  const currentPassword = reauthCurrentPassword.value.trim()

  if (!currentPassword) {
    reauthDialogError.value = 'Current password is required.'
    return
  }

  if (pendingAction.value.kind === 'deactivate') {
    reauthDialogError.value = ''
    await executePendingAction(currentPassword)
    return
  }

  reauthDialogError.value = ''
  isVerifyingCurrentPassword.value = true

  try {
    const response = await verifyCurrentUserPassword({
      currentPassword
    })

    if (!response.valid) {
      throw new Error(CURRENT_PASSWORD_INCORRECT_MESSAGE)
    }
  } catch (error) {
    if (isAuthFailureError(error)) {
      isVerifyingCurrentPassword.value = false
      isDiffDialogOpen.value = false
      isReauthDialogOpen.value = false
      resetPendingActionState()
      handleAuthenticationLoss()
      return
    }

    reauthDialogError.value = getErrorMessage(error, CURRENT_PASSWORD_INCORRECT_MESSAGE)
    isVerifyingCurrentPassword.value = false
    return
  }

  isVerifyingCurrentPassword.value = false
  await executePendingAction(currentPassword)
}

const sendEmailChangeCode = async (): Promise<void> => {
  emailChangeNotice.value = null

  if (!hasPendingEmailChange.value) {
    emailChangeNotice.value = {
      tone: 'info',
      title: 'No new email to verify',
      message: 'Enter a different email address before requesting a verification code.'
    }
    return
  }

  const isEmailValid = profileFormRef.value
    ? await profileFormRef.value.validateField('email').then(() => true).catch(() => false)
    : false

  if (isEmailValid !== true) {
    emailChangeNotice.value = {
      tone: 'error',
      title: 'Invalid email address',
      message: 'Enter a valid new email address before requesting a verification code.'
    }
    return
  }

  if (!hasApiSession.value) {
    handleAuthenticationLoss()
    return
  }

  if (currentAccountStatus.value !== 'ACTIVE') {
    emailChangeNotice.value = {
      tone: 'warning',
      title: 'Email changes unavailable',
      message: 'Only active accounts can request a verification code for a new email address.'
    }
    return
  }

  isSendingEmailChangeCode.value = true

  try {
    const payload: SendEmailChangeCodePayload = {
      newEmail: trimmedDraftEmail.value
    }

    await sendCurrentUserEmailChangeCode(payload)
    emailChangeTargetEmail.value = normalizedDraftEmail.value
    emailChangeVerificationCode.value = ''
    startEmailChangeCountdown()
    emailChangeNotice.value = {
      tone: 'success',
      title: 'Verification code sent',
      message: `A 6-digit verification code was sent to ${trimmedDraftEmail.value}. Enter it below, then confirm the email change.`
    }
  } catch (error) {
    if (isAuthFailureError(error)) {
      handleAuthenticationLoss()
      return
    }

    emailChangeNotice.value = {
      tone: 'error',
      title: 'Unable to send verification code',
      message: getErrorMessage(error, 'Unable to send a verification code right now.')
    }
  } finally {
    isSendingEmailChangeCode.value = false
  }
}

const confirmEmailChange = async (): Promise<void> => {
  contactNotice.value = null
  emailChangeNotice.value = null

  if (!hasPendingEmailChange.value) {
    emailChangeNotice.value = {
      tone: 'info',
      title: 'No email change to confirm',
      message: 'Enter a different email address first, then send and enter a verification code.'
    }
    return
  }

  const isEmailValid = profileFormRef.value
    ? await profileFormRef.value.validateField('email').then(() => true).catch(() => false)
    : false

  if (isEmailValid !== true) {
    emailChangeNotice.value = {
      tone: 'error',
      title: 'Invalid email address',
      message: 'Enter a valid email address before confirming the change.'
    }
    return
  }

  if (!isEmailChangeCodeBoundToCurrentDraft.value) {
    emailChangeNotice.value = {
      tone: 'warning',
      title: 'Verification code required',
      message: 'Send a verification code to this email address before confirming the change.'
    }
    return
  }

  if (emailChangeVerificationCode.value.trim().length !== 6) {
    emailChangeNotice.value = {
      tone: 'warning',
      title: 'Enter the verification code',
      message: 'A 6-digit verification code is required before the email change can be confirmed.'
    }
    return
  }

  openPendingAction({
    kind: 'email',
    title: 'Confirm Email Change',
    confirmButtonText: 'Confirm changes',
    requiresPassword: true,
    fallbackErrorMessage: 'Unable to change your email right now.',
    passwordTitle: 'Re-enter current password',
    passwordDescription: 'For security, enter your current password to finish updating your email address.',
    diffItems: [
      buildDiffItem('email', 'Email', savedProfile.value.email, trimmedDraftEmail.value)
    ],
    execute: async (currentPassword?: string) => {
      if (!hasApiSession.value) {
        handleAuthenticationLoss()
        return
      }

      isSavingEmail.value = true

      try {
        const payload: ChangeCurrentUserEmailPayload = {
          currentPassword: currentPassword?.trim() || '',
          newEmail: trimmedDraftEmail.value,
          code: emailChangeVerificationCode.value.trim()
        }

        const updatedProfile = normalizeProfile(await changeCurrentUserEmail(payload))
        applySavedProfilePatch(updatedProfile, ['email'])
        resetEmailChangeFlow()
        emailChangeNotice.value = {
          tone: 'success',
          title: 'Email updated successfully',
          message: 'Your new email address was verified and saved. Please log in again.'
        }
        await signOutAfterSensitiveAction()
      } finally {
        isSavingEmail.value = false
      }
    }
  })
}

const saveName = (): void => {
  personalNotice.value = null

  if (!isNameDirty.value) {
    personalNotice.value = {
      tone: 'info',
      title: 'No name changes to save',
      message: 'Update your full name first, then confirm the change.'
    }
    return
  }

  openPendingAction({
    kind: 'name',
    title: nameSaveButtonText.value,
    confirmButtonText: nameSaveButtonText.value,
    requiresPassword: false,
    fallbackErrorMessage: 'Unable to save your name right now.',
    diffItems: [
      buildDiffItem('fullName', 'Full name', savedProfile.value.fullName, trimmedFullName.value)
    ],
    execute: async () => {
      if (!hasApiSession.value) {
        handleAuthenticationLoss()
        return
      }

      isSavingName.value = true

      try {
        const payload: UpdateUserProfilePayload = {
          fullName: trimmedFullName.value
        }

        await updateCurrentUserProfile(payload)
        applySavedProfilePatch({ fullName: trimmedFullName.value }, ['fullName'])
        personalNotice.value = {
          tone: 'success',
          title: 'Name updated successfully',
          message: 'Your full name was saved to your profile.'
        }
        void loadSecurityActivity(true)
      } finally {
        isSavingName.value = false
      }
    }
  })
}

const savePhone = async (): Promise<void> => {
  contactNotice.value = null

  if (!isPhoneDirty.value) {
    contactNotice.value = {
      tone: 'info',
      title: 'No phone changes to save',
      message: 'Update your phone number first, then save it here.'
    }
    return
  }

  const isPhoneValid = profileFormRef.value
    ? await profileFormRef.value.validateField('localPhoneNumber').then(() => true).catch(() => false)
    : false

  if (isPhoneValid !== true) {
    contactNotice.value = {
      tone: 'error',
      title: 'Please review your phone number',
      message: 'Correct the highlighted phone field before saving.'
    }
    return
  }

  openPendingAction({
    kind: 'phone',
    title: 'Save Phone',
    confirmButtonText: 'Save Phone',
    requiresPassword: false,
    fallbackErrorMessage: 'Unable to save your phone number right now.',
    diffItems: [
      buildDiffItem('phoneNumber', 'Phone', savedProfile.value.phoneNumber, composedPhonePreview.value)
    ],
    execute: async () => {
      if (!hasApiSession.value) {
        handleAuthenticationLoss()
        return
      }

      isSavingPhone.value = true

      try {
        const payload: UpdateUserProfilePayload = {
          phoneNumber: composedPhonePreview.value
        }

        await updateCurrentUserProfile(payload)
        applySavedProfilePatch({ phoneNumber: composedPhonePreview.value }, ['phoneNumber'])
        contactNotice.value = {
          tone: 'success',
          title: 'Phone updated successfully',
          message: 'Your phone number was saved to your profile.'
        }
        void loadSecurityActivity(true)
      } finally {
        isSavingPhone.value = false
      }
    }
  })
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

  pendingAvatarFile.value = selectedFile
  openPendingAction({
    kind: 'avatar',
    title: 'Confirm Avatar Update',
    confirmButtonText: 'Upload Avatar',
    requiresPassword: false,
    fallbackErrorMessage: 'Unable to upload your avatar right now.',
    diffItems: [
      buildDiffItem(
        'avatar',
        'Profile photo',
        currentAvatarUrl.value ? 'Current photo saved' : '',
        `New image selected (${selectedFile.name})`
      )
    ],
    execute: async () => {
      if (!pendingAvatarFile.value) {
        throw new Error('Please select an image to upload.')
      }

      isUploadingAvatar.value = true

      try {
        const response: AvatarUploadResponse = await uploadCurrentUserAvatar(pendingAvatarFile.value)

        if (!response.avatarUrl) {
          throw new Error('Avatar upload completed without a usable image URL.')
        }

        applySavedProfilePatch({ avatarUrl: response.avatarUrl })
        avatarNotice.value = {
          tone: 'success',
          title: 'Avatar updated successfully',
          message: 'Your new profile photo was uploaded and saved.'
        }
        void loadSecurityActivity(true)
      } finally {
        isUploadingAvatar.value = false
        resetAvatarInput()
      }
    }
  })
}

const loadSecurityActivity = async (preserveExisting = false): Promise<void> => {
  if (!hasApiSession.value) {
    return
  }

  const previousItems = preserveExisting ? [...securityActivityItems.value] : []

  if (!preserveExisting) {
    securityActivityItems.value = []
  }

  securityActivityNotice.value = null
  isLoadingSecurityActivity.value = true

  try {
    securityActivityItems.value = await getCurrentUserSecurityActivities()
  } catch (error) {
    if (isAuthFailureError(error)) {
      handleAuthenticationLoss()
      return
    }

    if (preserveExisting) {
      securityActivityItems.value = previousItems
    }

    securityActivityNotice.value = {
      tone: 'error',
      title: 'Unable to load recent activity',
      message: getErrorMessage(error, 'We could not load your recent security activity.')
    }
  } finally {
    isLoadingSecurityActivity.value = false
  }
}

const loadDashboard = async (): Promise<void> => {
  viewState.value = 'loading'
  loadErrorMessage.value = 'We could not load your account settings.'
  avatarNotice.value = null
  personalNotice.value = null
  contactNotice.value = null
  emailChangeNotice.value = null
  passwordNotice.value = null
  deactivationNotice.value = null
  resetPendingActionState()
  resetEmailChangeFlow()
  resetSecurityActivityState()

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
    void loadSecurityActivity()

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

  openPendingAction({
    kind: 'password',
    title: 'Confirm Password Update',
    confirmButtonText: 'Update Password',
    requiresPassword: false,
    fallbackErrorMessage: 'Unable to update your password right now.',
    diffItems: [
      buildDiffItem(
        'password',
        'Password',
        'Current password verified',
        'New password will replace the current password'
      )
    ],
    execute: async () => {
      if (!hasApiSession.value) {
        handleAuthenticationLoss()
        return
      }

      isSavingPassword.value = true

      try {
        const payload: ChangePasswordPayload = {
          currentPassword: passwordForm.currentPassword.trim(),
          newPassword: passwordForm.newPassword.trim(),
          confirmationPassword: passwordForm.confirmationPassword.trim()
        }

        await changeCurrentUserPassword(payload)
        clearRememberedCredentials()
        lastPasswordUpdatedAt.value = new Date()
        ElMessage.success('Password updated successfully. Please log in again.')
        await signOutAfterSensitiveAction()
      } finally {
        isSavingPassword.value = false
      }
    }
  })
}

const deactivateAccount = (): void => {
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

  openPendingAction({
    kind: 'deactivate',
    title: 'Confirm Account Deactivation',
    confirmButtonText: 'Continue',
    requiresPassword: true,
    fallbackErrorMessage: 'Unable to deactivate your account right now.',
    passwordTitle: 'Re-enter current password',
    passwordDescription: 'Current password confirmation is required before this account can be deactivated.',
    diffItems: [
      buildDiffItem('status', 'Account status', accountStatusLabel.value, 'Deactivated')
    ],
    execute: async (currentPassword?: string) => {
      if (!hasApiSession.value) {
        handleAuthenticationLoss()
        return
      }

      isDeactivatingAccount.value = true

      try {
        await deactivateCurrentUserAccount({
          currentPassword: currentPassword?.trim() || ''
        })

        applySavedProfilePatch({ status: 'DEACTIVATED' })
        ElMessage.success('Account deactivated successfully. Redirecting to login.')
        await signOutAfterSensitiveAction()
      } finally {
        isDeactivatingAccount.value = false
      }
    }
  })
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
  () => profileForm.email,
  newValue => {
    const normalizedEmail = normalizeEmail(newValue)

    if (emailChangeTargetEmail.value && normalizedEmail !== emailChangeTargetEmail.value) {
      resetEmailChangeFlow()
    }
  }
)

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

  clearEmailChangeCountdown()
})
</script>

<style scoped lang="scss">
.page-card {
  padding: var(--space-6);
  background:
    radial-gradient(circle at top right, rgba(var(--color-primary-rgb), 0.12), transparent 32%),
    var(--color-bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 12px 40px rgba(var(--color-shadow-rgb), 0.08);
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
  gap: var(--space-4);
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
  gap: var(--space-3);
  width: min(100%, 420px);
}

.page-meta__text {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.95rem;
  line-height: 1.6;
}

.completeness-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-primary-rgb), 0.16);
  border-radius: var(--radius-md);
  background:
    linear-gradient(135deg, rgba(var(--color-primary-rgb), 0.08), rgba(var(--color-bg-surface-rgb), 0.98)),
    var(--color-bg-page);
}

.completeness-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  color: var(--color-text-secondary);
}

.completeness-panel__header strong {
  color: var(--color-text-primary);
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
  padding: var(--space-5);
  border: 1px solid rgba(var(--color-primary-rgb), 0.18);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(var(--color-primary-rgb), 0.12), rgba(var(--color-bg-surface-rgb), 0.95)),
    var(--color-bg-surface);
  box-shadow: 0 12px 30px rgba(var(--color-shadow-rgb), 0.07);
}

.skeleton-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.skeleton-card__title {
  width: 40%;
}

.skeleton-card__text {
  width: 100%;
}

.skeleton-card__text--short {
  width: 70%;
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
  box-shadow: 0 8px 24px rgba(var(--color-shadow-rgb), 0.05);
  transition:
    border-color var(--transition-base),
    box-shadow var(--transition-base),
    transform var(--transition-base);
}

.settings-card--highlighted {
  border-color: rgba(var(--color-primary-rgb), 0.45);
  box-shadow: 0 14px 32px rgba(var(--color-primary-rgb), 0.18);
  transform: translateY(-2px);
}

.settings-card--danger {
  border-color: rgba(var(--color-danger-rgb), 0.24);
  background:
    linear-gradient(180deg, rgba(var(--color-danger-rgb), 0.06), transparent 46%),
    var(--color-bg-surface);
}

.settings-card--avatar {
  background:
    linear-gradient(180deg, rgba(var(--color-primary-rgb), 0.08), transparent 54%),
    var(--color-bg-surface);
}

.settings-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-5) var(--space-4);
  border-bottom: 1px solid rgba(var(--color-border-rgb), 0.85);
}

.settings-card__eyebrow {
  margin: 0;
  color: var(--color-primary);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
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

.remember-credentials-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
}

.remember-credentials-panel__info {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.remember-credentials-panel__title {
  color: var(--color-text-primary);
  font-size: 1rem;
}

.remember-credentials-panel__text {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
  font-size: 0.9rem;
}

.field-grid {
  display: grid;
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
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
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
  background: linear-gradient(135deg, rgba(var(--color-primary-rgb), 0.18), rgba(var(--color-primary-rgb), 0.34));
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

.contact-block {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.contact-block__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.contact-block__title {
  color: var(--color-text-primary);
  font-size: 1rem;
}

.contact-block__text {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.phone-field {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  gap: var(--space-3);
}

.email-verification-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-primary-rgb), 0.2);
  border-radius: var(--radius-md);
  background:
    linear-gradient(135deg, rgba(var(--color-primary-rgb), 0.08), rgba(var(--color-bg-surface-rgb), 0.96)),
    var(--color-bg-page);
}

.email-verification-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.email-verification-panel__label {
  display: block;
  margin-bottom: var(--space-2);
  color: var(--color-primary);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.email-verification-panel__text,
.email-verification-panel__hint {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.email-verification-panel__body {
  display: grid;
  gap: var(--space-3);
}

.module-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.module-actions--inline {
  padding: 0;
  border: none;
  background: transparent;
}

.module-actions__hint {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.module-actions__buttons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.contact-summary,
.status-overview {
  display: grid;
  gap: var(--space-3);
}

.contact-summary__item,
.status-overview__item {
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
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
  line-height: 1.5;
}

.strength-panel {
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
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
  background: rgba(var(--color-border-rgb), 0.9);
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
  box-shadow: 0 0 0 4px rgba(var(--color-success-rgb), 0.14);
}

.activity-loading,
.activity-empty,
.activity-error {
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.activity-empty__title {
  color: var(--color-text-primary);
}

.activity-empty__text {
  margin: var(--space-2) 0 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.activity-actions {
  display: flex;
  margin-top: var(--space-4);
}

.activity-groups {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.activity-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.activity-group__title {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 1rem;
}

.activity-timeline {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.activity-timeline__item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.activity-timeline__marker {
  width: 12px;
  height: 12px;
  margin-top: 6px;
  border-radius: 999px;
  background: var(--color-success);
  box-shadow: 0 0 0 6px rgba(var(--color-success-rgb), 0.12);
}

.activity-timeline__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.activity-timeline__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.activity-timeline__title {
  color: var(--color-text-primary);
}

.activity-timeline__time {
  color: var(--color-text-secondary);
  font-size: 0.92rem;
  white-space: nowrap;
}

.activity-timeline__summary {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.appearance-panel {
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
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

.change-summary-dialog {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.change-summary-dialog__text {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.change-summary-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.diff-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.diff-list__item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.diff-list__bullet {
  color: var(--color-primary);
  font-weight: 700;
  line-height: 1.5;
}

.diff-list__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.diff-list__content strong {
  color: var(--color-text-primary);
}

.diff-list__arrow {
  margin: 0 6px;
  color: var(--color-primary);
  font-weight: 700;
}

.dialog-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.dialog-field__label {
  color: var(--color-text-primary);
  font-weight: 600;
}

.status-overview {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.danger-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid rgba(var(--color-danger-rgb), 0.18);
  border-radius: var(--radius-md);
  background: rgba(var(--color-danger-rgb), 0.05);
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
  background: rgba(var(--color-danger-rgb), 0.12);
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
  border: 1px solid rgba(var(--color-border-rgb), 0.9);
  border-left-width: 4px;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
}

.status-banner--success {
  border-color: rgba(var(--color-success-rgb), 0.22);
  border-left-color: var(--color-success);
  background: rgba(var(--color-success-rgb), 0.07);
}

.status-banner--error {
  border-color: rgba(var(--color-danger-rgb), 0.2);
  border-left-color: var(--color-danger);
  background: rgba(var(--color-danger-rgb), 0.08);
}

.status-banner--info {
  border-color: rgba(var(--color-info-rgb), 0.18);
  border-left-color: var(--color-info);
  background: rgba(var(--color-info-rgb), 0.08);
}

.status-banner--warning {
  border-color: rgba(var(--color-warning-rgb), 0.2);
  border-left-color: var(--color-warning);
  background: rgba(var(--color-warning-rgb), 0.1);
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
  box-shadow: 0 8px 30px rgba(var(--color-shadow-rgb), 0.08);
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

@media (min-width: 900px) {
  .page-header {
    flex-direction: row;
    align-items: flex-start;
    justify-content: space-between;
  }

  .contact-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .page-card {
    padding: var(--space-4);
  }

  .settings-card__header,
  .email-verification-panel__header,
  .module-actions,
  .danger-item,
  .strength-panel__header,
  .activity-timeline__header,
  .change-summary-dialog__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .avatar-panel,
  .avatar-panel__preview,
  .phone-field {
    grid-template-columns: minmax(0, 1fr);
    flex-direction: column;
    align-items: flex-start;
  }

  .status-overview {
    grid-template-columns: minmax(0, 1fr);
  }

  .module-actions__buttons {
    width: 100%;
  }
}
</style>
