export type ThemeMode = 'light' | 'dark'

export const THEME_MODE_STORAGE_KEY = 'profile-theme-mode'
export const THEME_MODE_EVENT_NAME = 'theme-mode-change'

const DEFAULT_THEME_MODE: ThemeMode = 'light'

const isThemeMode = (value: string | null | undefined): value is ThemeMode => {
  return value === 'light' || value === 'dark'
}

export const getStoredThemeMode = (): ThemeMode | null => {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    const rawValue = window.localStorage.getItem(THEME_MODE_STORAGE_KEY)
    return isThemeMode(rawValue) ? rawValue : null
  } catch {
    return null
  }
}

export const getCurrentThemeMode = (): ThemeMode => {
  if (typeof document === 'undefined') {
    return getStoredThemeMode() ?? DEFAULT_THEME_MODE
  }

  const html = document.documentElement
  const dataTheme = html.getAttribute('data-theme')

  if (isThemeMode(dataTheme)) {
    return dataTheme
  }

  if (html.classList.contains('dark')) {
    return 'dark'
  }

  return getStoredThemeMode() ?? DEFAULT_THEME_MODE
}

export const applyThemeMode = (mode: ThemeMode, options?: { persist?: boolean }): ThemeMode => {
  const persist = options?.persist ?? true

  if (typeof document !== 'undefined') {
    const html = document.documentElement
    const isDarkMode = mode === 'dark'

    html.setAttribute('data-theme', mode)
    html.classList.toggle('dark', isDarkMode)
    document.body?.classList.toggle('dark', isDarkMode)
  }

  if (persist && typeof window !== 'undefined') {
    try {
      window.localStorage.setItem(THEME_MODE_STORAGE_KEY, mode)
    } catch {
      // Ignore storage write failures and still apply the in-memory theme.
    }
  }

  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent<ThemeMode>(THEME_MODE_EVENT_NAME, { detail: mode }))
  }

  return mode
}

export const initializeThemeMode = (): ThemeMode => {
  const initialMode = getStoredThemeMode() ?? getCurrentThemeMode()
  return applyThemeMode(initialMode, { persist: false })
}
