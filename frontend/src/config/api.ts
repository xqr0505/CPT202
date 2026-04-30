const trimTrailingSlash = (value: string): string => value.replace(/\/+$/, '')

const readApiBaseUrlFromEnv = (): string => {
  const envBase = import.meta.env.VITE_API_BASE_URL?.trim() || import.meta.env.VITE_API_URL?.trim() || ''

  if (!envBase) {
    return '/api'
  }

  const normalized = trimTrailingSlash(envBase)
  if (!/\/api$/i.test(normalized)) {
    throw new Error(
      `Invalid VITE_API_BASE_URL: "${normalized}". It must end with "/api" (for example "/api" or "http://localhost:8081/api").`
    )
  }

  return normalized
}

export const getApiBaseUrl = (): string => readApiBaseUrlFromEnv()

export const apiBaseUrl = readApiBaseUrlFromEnv()
