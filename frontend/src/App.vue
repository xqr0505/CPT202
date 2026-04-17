<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DevFooter from '@/components/common/DevFooter.vue'
import { logout, getAuthToken, getRefreshToken, refreshAuthToken } from '@/api/request'

const router = useRouter()
const isDev = Boolean(import.meta.env.DEV)
const globalError = ref<{ message: string | null; stack?: string | null } | null>(null)

const WARNING_DELAY = 29 * 60 * 1000
const AUTO_LOGOUT_DELAY = 30 * 60 * 1000
const ACTIVITY_EVENT_KEY = 'session-activity-event'
const LOGOUT_EVENT_KEY = 'logout-event'

let warningTimer: number | null = null
let logoutTimer: number | null = null
let warningActive = false

function showError(message: string, stack?: string) {
  globalError.value = { message, stack }
}

function clearSessionTimers() {
  if (warningTimer) {
    window.clearTimeout(warningTimer)
    warningTimer = null
  }

  if (logoutTimer) {
    window.clearTimeout(logoutTimer)
    logoutTimer = null
  }

  if (warningActive) {
    try {
      ElMessageBox.close()
    } catch {
      // ignore
    }
    warningActive = false
  }
}

async function expireSession() {
  clearSessionTimers()
  await logout()
  ElMessage.warning('会话已过期，请重新登录')
}

async function showSessionWarning() {
  if (!getAuthToken() && !getRefreshToken()) {
    return
  }

  warningActive = true

  try {
    await ElMessageBox.confirm(
      '因无操作，您的会话将在 1 分钟后过期，是否保持登录？',
      '会话即将过期',
      {
        confirmButtonText: '保持登录',
        cancelButtonText: '立即登出',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        distinguishCancelAndClose: true,
        type: 'warning'
      }
    )

    await refreshAuthToken()
    resetSessionTimers()
    ElMessage.success('已保持登录，登录状态已续期')
  } catch (error) {
    await expireSession()
  } finally {
    warningActive = false
  }
}

function resetSessionTimers() {
  clearSessionTimers()

  if (!getAuthToken() && !getRefreshToken()) {
    return
  }

  warningTimer = window.setTimeout(showSessionWarning, WARNING_DELAY)
  logoutTimer = window.setTimeout(async () => {
    await expireSession()
  }, AUTO_LOGOUT_DELAY)
}

function handleUserActivity() {
  if (!getAuthToken() && !getRefreshToken()) {
    return
  }

  if (warningActive) {
    try {
      ElMessageBox.close()
    } catch {
      // ignore
    }
    warningActive = false
  }

  resetSessionTimers()
  window.dispatchEvent(new CustomEvent('session-activity'))
  localStorage.setItem(ACTIVITY_EVENT_KEY, Date.now().toString())
  setTimeout(() => localStorage.removeItem(ACTIVITY_EVENT_KEY), 100)
}

function handleStorageEvent(event: StorageEvent) {
  if (event.key === LOGOUT_EVENT_KEY && event.newValue) {
    logout()
    clearSessionTimers()
    return
  }

  if (event.key === ACTIVITY_EVENT_KEY && event.newValue) {
    if (warningActive) {
      try {
        ElMessageBox.close()
      } catch {
        // ignore
      }
      warningActive = false
    }
    resetSessionTimers()
    window.dispatchEvent(new CustomEvent('session-activity'))
  }
}

onMounted(() => {
  const onError = (event: ErrorEvent) => {
    try { event.preventDefault() } catch {}
    showError(event.message, (event.error && event.error.stack) || undefined)
  }
  const onRejection = (event: PromiseRejectionEvent) => {
    try { event.preventDefault() } catch {}
    const reason = (event.reason && (event.reason.message || String(event.reason))) || 'Unhandled promise rejection'
    showError(reason)
  }

  window.addEventListener('error', onError)
  window.addEventListener('unhandledrejection', onRejection)
  const onSessionActivity = () => {
    if (warningActive) {
      try {
        ElMessageBox.close()
      } catch {
        // ignore
      }
      warningActive = false
    }
    resetSessionTimers()
  }

  window.addEventListener('storage', handleStorageEvent)
  window.addEventListener('click', handleUserActivity)
  window.addEventListener('keydown', handleUserActivity)
  window.addEventListener('scroll', handleUserActivity)
  window.addEventListener('touchstart', handleUserActivity)
  window.addEventListener('session-activity', onSessionActivity)

  ;(window as any).__triggerGlobalError = (msg = 'Manual test error') => showError(msg)
  resetSessionTimers()

  onBeforeUnmount(() => {
    window.removeEventListener('error', onError)
    window.removeEventListener('unhandledrejection', onRejection)
    window.removeEventListener('storage', handleStorageEvent)
    window.removeEventListener('click', handleUserActivity)
    window.removeEventListener('keydown', handleUserActivity)
    window.removeEventListener('scroll', handleUserActivity)
    window.removeEventListener('touchstart', handleUserActivity)
    window.removeEventListener('session-activity', onSessionActivity)
  })
})

function goToErrorPage(code: string) {
  router.push(`/error/${code}`)
}

function dismiss() {
  globalError.value = null
}

function reloadApp() {
  window.location.reload()
}
</script>

<template>
  <router-view />
  <DevFooter v-if="isDev" />
</template>
