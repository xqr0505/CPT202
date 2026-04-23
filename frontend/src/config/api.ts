const trimTrailingSlash = (value: string): string => value.replace(/\/+$/, '')

const readApiBaseUrlFromEnv = (): string => {
  const envBase = import.meta.env.VITE_API_BASE_URL?.trim() || import.meta.env.VITE_API_URL?.trim() || ''

  if (!envBase) {
    throw new Error('Missing VITE_API_BASE_URL (or legacy VITE_API_URL) environment variable.')
  }

  return trimTrailingSlash(envBase)
}

export const getApiBaseUrl = (): string => readApiBaseUrlFromEnv()

export const apiBaseUrl = readApiBaseUrlFromEnv()
