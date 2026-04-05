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
        <el-form ref="profileFormRef" :model="form" :rules="profileRules" label-position="top" class="profile-form">
          <div class="form-grid">
            <el-form-item label="Name" prop="fullName">
              <el-input
                v-model="form.fullName"
                placeholder="Enter your full name"
              />
            </el-form-item>

            <el-form-item label="Email" prop="email">
              <el-input
                v-model="form.email"
                placeholder="Enter your email address"
              />
            </el-form-item>

            <el-form-item label="Phone" prop="localPhoneNumber">
              <div class="phone-field-group">
                <el-select
                  v-model="form.countryCode"
                  filterable
                  class="phone-field-group__code"
                  placeholder="Code"
                >
                  <el-option
                    v-for="option in countryCodeOptions"
                    :key="option.label"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>

                <el-input
                  v-model="form.localPhoneNumber"
                  class="phone-field-group__number"
                  placeholder="Enter local number"
                />
              </div>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import CustomButton from '@/components/common/CustomButton.vue'

defineOptions({ name: 'CustomerProfileEdit' })

type ViewState = 'loading' | 'ready' | 'error'

interface UpdateUserProfilePayload {
  fullName: string
  email: string
  phoneNumber: string
}

interface ProfileEditForm {
  fullName: string
  email: string
  countryCode: string
  localPhoneNumber: string
}

interface CountryCodeOption {
  label: string
  value: string
}

const USER_PROFILE_STORAGE_KEY = 'mock-user-profile'
const DEFAULT_COUNTRY_CODE = '+86'
const COUNTRY_CODE_OPTIONS: ReadonlyArray<CountryCodeOption> = [
  { label: '🇦🇫 Afghanistan +93', value: '+93' },
  { label: '🇦🇱 Albania +355', value: '+355' },
  { label: '🇩🇿 Algeria +213', value: '+213' },
  { label: '🇦🇷 Argentina +54', value: '+54' },
  { label: '🇦🇺 Australia +61', value: '+61' },
  { label: '🇦🇹 Austria +43', value: '+43' },
  { label: '🇧🇭 Bahrain +973', value: '+973' },
  { label: '🇧🇩 Bangladesh +880', value: '+880' },
  { label: '🇧🇾 Belarus +375', value: '+375' },
  { label: '🇧🇪 Belgium +32', value: '+32' },
  { label: '🇧🇴 Bolivia +591', value: '+591' },
  { label: '🇧🇷 Brazil +55', value: '+55' },
  { label: '🇧🇬 Bulgaria +359', value: '+359' },
  { label: '🇰🇭 Cambodia +855', value: '+855' },
  { label: '🇨🇲 Cameroon +237', value: '+237' },
  { label: '🇨🇦 Canada +1', value: '+1' },
  { label: '🇨🇱 Chile +56', value: '+56' },
  { label: '🇨🇳 China +86', value: '+86' },
  { label: '🇨🇴 Colombia +57', value: '+57' },
  { label: '🇨🇷 Costa Rica +506', value: '+506' },
  { label: '🇭🇷 Croatia +385', value: '+385' },
  { label: '🇨🇾 Cyprus +357', value: '+357' },
  { label: '🇨🇿 Czech Republic +420', value: '+420' },
  { label: '🇩🇰 Denmark +45', value: '+45' },
  { label: '🇩🇴 Dominican Republic +1', value: '+1' },
  { label: '🇪🇨 Ecuador +593', value: '+593' },
  { label: '🇪🇬 Egypt +20', value: '+20' },
  { label: '🇪🇪 Estonia +372', value: '+372' },
  { label: '🇪🇹 Ethiopia +251', value: '+251' },
  { label: '🇫🇮 Finland +358', value: '+358' },
  { label: '🇫🇷 France +33', value: '+33' },
  { label: '🇬🇪 Georgia +995', value: '+995' },
  { label: '🇩🇪 Germany +49', value: '+49' },
  { label: '🇬🇭 Ghana +233', value: '+233' },
  { label: '🇬🇷 Greece +30', value: '+30' },
  { label: '🇬🇹 Guatemala +502', value: '+502' },
  { label: '🇭🇰 Hong Kong SAR +852', value: '+852' },
  { label: '🇭🇺 Hungary +36', value: '+36' },
  { label: '🇮🇸 Iceland +354', value: '+354' },
  { label: '🇮🇳 India +91', value: '+91' },
  { label: '🇮🇩 Indonesia +62', value: '+62' },
  { label: '🇮🇷 Iran +98', value: '+98' },
  { label: '🇮🇶 Iraq +964', value: '+964' },
  { label: '🇮🇪 Ireland +353', value: '+353' },
  { label: '🇮🇱 Israel +972', value: '+972' },
  { label: '🇮🇹 Italy +39', value: '+39' },
  { label: '🇯🇲 Jamaica +1', value: '+1' },
  { label: '🇯🇵 Japan +81', value: '+81' },
  { label: '🇯🇴 Jordan +962', value: '+962' },
  { label: '🇰🇿 Kazakhstan +7', value: '+7' },
  { label: '🇰🇪 Kenya +254', value: '+254' },
  { label: '🇰🇼 Kuwait +965', value: '+965' },
  { label: '🇱🇦 Laos +856', value: '+856' },
  { label: '🇱🇻 Latvia +371', value: '+371' },
  { label: '🇱🇧 Lebanon +961', value: '+961' },
  { label: '🇱🇹 Lithuania +370', value: '+370' },
  { label: '🇱🇺 Luxembourg +352', value: '+352' },
  { label: '🇲🇴 Macao SAR +853', value: '+853' },
  { label: '🇲🇰 North Macedonia +389', value: '+389' },
  { label: '🇲🇾 Malaysia +60', value: '+60' },
  { label: '🇲🇹 Malta +356', value: '+356' },
  { label: '🇲🇽 Mexico +52', value: '+52' },
  { label: '🇲🇩 Moldova +373', value: '+373' },
  { label: '🇲🇳 Mongolia +976', value: '+976' },
  { label: '🇲🇦 Morocco +212', value: '+212' },
  { label: '🇲🇲 Myanmar +95', value: '+95' },
  { label: '🇳🇵 Nepal +977', value: '+977' },
  { label: '🇳🇱 Netherlands +31', value: '+31' },
  { label: '🇳🇿 New Zealand +64', value: '+64' },
  { label: '🇳🇬 Nigeria +234', value: '+234' },
  { label: '🇳🇴 Norway +47', value: '+47' },
  { label: '🇴🇲 Oman +968', value: '+968' },
  { label: '🇵🇰 Pakistan +92', value: '+92' },
  { label: '🇵🇦 Panama +507', value: '+507' },
  { label: '🇵🇾 Paraguay +595', value: '+595' },
  { label: '🇵🇪 Peru +51', value: '+51' },
  { label: '🇵🇭 Philippines +63', value: '+63' },
  { label: '🇵🇱 Poland +48', value: '+48' },
  { label: '🇵🇹 Portugal +351', value: '+351' },
  { label: '🇶🇦 Qatar +974', value: '+974' },
  { label: '🇷🇴 Romania +40', value: '+40' },
  { label: '🇷🇺 Russia +7', value: '+7' },
  { label: '🇸🇦 Saudi Arabia +966', value: '+966' },
  { label: '🇷🇸 Serbia +381', value: '+381' },
  { label: '🇸🇬 Singapore +65', value: '+65' },
  { label: '🇸🇰 Slovakia +421', value: '+421' },
  { label: '🇸🇮 Slovenia +386', value: '+386' },
  { label: '🇿🇦 South Africa +27', value: '+27' },
  { label: '🇰🇷 South Korea +82', value: '+82' },
  { label: '🇪🇸 Spain +34', value: '+34' },
  { label: '🇱🇰 Sri Lanka +94', value: '+94' },
  { label: '🇸🇪 Sweden +46', value: '+46' },
  { label: '🇨🇭 Switzerland +41', value: '+41' },
  { label: '🇸🇾 Syria +963', value: '+963' },
  { label: '🇹🇼 Taiwan +886', value: '+886' },
  { label: '🇹🇯 Tajikistan +992', value: '+992' },
  { label: '🇹🇿 Tanzania +255', value: '+255' },
  { label: '🇹🇭 Thailand +66', value: '+66' },
  { label: '🇹🇳 Tunisia +216', value: '+216' },
  { label: '🇹🇷 Turkey +90', value: '+90' },
  { label: '🇹🇲 Turkmenistan +993', value: '+993' },
  { label: '🇺🇬 Uganda +256', value: '+256' },
  { label: '🇺🇦 Ukraine +380', value: '+380' },
  { label: '🇦🇪 United Arab Emirates +971', value: '+971' },
  { label: '🇬🇧 United Kingdom +44', value: '+44' },
  { label: '🇺🇸 United States +1', value: '+1' },
  { label: '🇺🇾 Uruguay +598', value: '+598' },
  { label: '🇺🇿 Uzbekistan +998', value: '+998' },
  { label: '🇻🇪 Venezuela +58', value: '+58' },
  { label: '🇻🇳 Vietnam +84', value: '+84' },
  { label: '🇾🇪 Yemen +967', value: '+967' },
  { label: '🇿🇲 Zambia +260', value: '+260' },
  { label: '🇿🇼 Zimbabwe +263', value: '+263' }
]

const DEFAULT_PROFILE: UpdateUserProfilePayload = {
  fullName: 'Emma Chen',
  email: 'emma.chen@example.com',
  phoneNumber: '+86 13812345678'
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

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

const isCountryCode = (value: string): boolean => {
  return COUNTRY_CODE_OPTIONS.some(option => option.value === value)
}

const createCustomCountryCodeOption = (countryCode: string): CountryCodeOption => ({
  label: `Current ${countryCode}`,
  value: countryCode
})

const splitPhoneNumber = (phoneNumber: string): Pick<ProfileEditForm, 'countryCode' | 'localPhoneNumber'> => {
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
      countryCode: matchedPhoneNumber[1],
      localPhoneNumber: matchedPhoneNumber[2]
    }
  }

  return {
    countryCode: DEFAULT_COUNTRY_CODE,
    localPhoneNumber: normalizedPhoneNumber
  }
}

const buildPhoneNumber = (countryCode: string, localPhoneNumber: string): string => {
  return `${countryCode} ${localPhoneNumber.trim()}`
}

const countryCodeOptions = computed<CountryCodeOption[]>(() => {
  if (!form.countryCode || isCountryCode(form.countryCode)) {
    return [...COUNTRY_CODE_OPTIONS]
  }

  return [createCustomCountryCodeOption(form.countryCode), ...COUNTRY_CODE_OPTIONS]
})

const validatePhoneNumber = (
  rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void => {
  if (!form.countryCode) {
    callback(new Error('Please select a country code'))
    return
  }

  if (!/^\+\d{1,3}$/.test(form.countryCode)) {
    callback(new Error('Please select a valid country or region calling code'))
    return
  }

  const localPhoneNumber = typeof value === 'string' ? value.trim() : ''

  if (!localPhoneNumber) {
    callback(new Error('Phone number is required'))
    return
  }

  if (!/^\d+$/.test(localPhoneNumber)) {
    callback(new Error('Phone number must contain digits only'))
    return
  }

  callback()
}

const profileRules: FormRules<ProfileEditForm> = {
  fullName: [{ required: true, message: 'Name is required.', trigger: 'blur' }],
  email: [
    { required: true, message: 'Email is required.', trigger: 'blur' },
    {
      pattern: emailPattern,
      message: 'Enter a valid email address.',
      trigger: 'blur'
    }
  ],
  localPhoneNumber: [
    {
      validator: validatePhoneNumber,
      trigger: 'submit'
    }
  ]
}

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

const router = useRouter()

const viewState = ref<ViewState>('loading')
const isSaving = ref(false)
const loadErrorMessage = ref('We could not load your profile details.')
const saveErrorMessage = ref('')
const profileFormRef = ref<FormInstance>()

const form = reactive<ProfileEditForm>({
  fullName: '',
  email: '',
  countryCode: DEFAULT_COUNTRY_CODE,
  localPhoneNumber: ''
})

const resetForm = (profile: UpdateUserProfilePayload): void => {
  const phoneDetails = splitPhoneNumber(profile.phoneNumber)

  form.fullName = profile.fullName
  form.email = profile.email
  form.countryCode = phoneDetails.countryCode
  form.localPhoneNumber = phoneDetails.localPhoneNumber
  saveErrorMessage.value = ''
  profileFormRef.value?.clearValidate()
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

const saveProfile = async (): Promise<void> => {
  saveErrorMessage.value = ''

  if (!profileFormRef.value) {
    return
  }

  const valid = await new Promise<boolean>(resolve => {
    profileFormRef.value?.validate((isValid, invalidFields) => {
      if (!isValid) {
        saveErrorMessage.value = invalidFields?.localPhoneNumber
          ? 'Please fix phone number format'
          : 'Please correct the highlighted fields and try again.'
        ElMessage.error(saveErrorMessage.value)
        resolve(false)
        return
      }

      resolve(true)
    })
  })

  if (!valid) {
    return
  }

  isSaving.value = true

  try {
    await wait(250)

    const updatedProfile: UpdateUserProfilePayload = {
      fullName: form.fullName,
      email: form.email,
      phoneNumber: buildPhoneNumber(form.countryCode, form.localPhoneNumber)
    }

    writeStoredProfile({
      fullName: updatedProfile.fullName.trim(),
      email: updatedProfile.email.trim(),
      phoneNumber: updatedProfile.phoneNumber.trim()
    })
    ElMessage.success('Personal information updated successfully.')
    await router.push('/customer/profile')
  } catch (error) {
    saveErrorMessage.value = getErrorMessage(error, 'Unable to save your profile right now.')
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

.phone-field-group {
  display: grid;
  grid-template-columns: minmax(180px, 240px) minmax(0, 1fr);
  gap: var(--space-3);
  width: 100%;
}

.phone-field-group__code,
.phone-field-group__number {
  width: 100%;
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

  .phone-field-group {
    grid-template-columns: 1fr;
  }
}
</style>
