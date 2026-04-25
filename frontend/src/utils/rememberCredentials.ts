type RememberedCredentialsPayload = {
  email: string
  password: string
}

type RememberedCredentialsRecordV1 = {
  v: 1
  exp: number
  iv: string
  data: string
}

const ALLOW_KEY = 'rememberCredentialsAllowed'
const DATA_KEY = 'rememberedCredentials'
const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000

const textEncoder = new TextEncoder()
const textDecoder = new TextDecoder()

let derivedKeyPromise: Promise<CryptoKey> | null = null

const toBase64 = (buffer: ArrayBuffer): string => {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let index = 0; index < bytes.length; index += 1) {
    binary += String.fromCharCode(bytes[index] ?? 0)
  }
  return btoa(binary)
}

const fromBase64 = (base64: string): ArrayBuffer => {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes.buffer
}

export const isRememberCredentialsAllowed = (): boolean => {
  const stored = localStorage.getItem(ALLOW_KEY)
  if (stored === null) {
    return true
  }
  return stored === 'true'
}

export const clearRememberedCredentials = (): void => {
  localStorage.removeItem(DATA_KEY)
}

export const setRememberCredentialsAllowed = (allowed: boolean): void => {
  localStorage.setItem(ALLOW_KEY, allowed ? 'true' : 'false')
  if (!allowed) {
    clearRememberedCredentials()
  }
}

const getDeviceBindingSeed = (): string => {
  if (typeof navigator === 'undefined') {
    return 'unknown-device'
  }

  const parts = [
    navigator.userAgent || '',
    navigator.platform || '',
    navigator.language || '',
    typeof location !== 'undefined' ? location.origin : ''
  ]
  return parts.join('|')
}

const getDerivedKey = async (): Promise<CryptoKey> => {
  if (derivedKeyPromise) {
    return derivedKeyPromise
  }

  derivedKeyPromise = (async () => {
    if (!globalThis.crypto?.subtle) {
      throw new Error('WebCrypto is unavailable in this browser environment.')
    }

    const baseSecret = 'cpt202-remember-credentials-v1'
    const seed = `${baseSecret}|${getDeviceBindingSeed()}`
    const keyMaterial = await crypto.subtle.importKey(
      'raw',
      textEncoder.encode(seed),
      'PBKDF2',
      false,
      ['deriveKey']
    )

    const salt = textEncoder.encode(`remember-credentials-salt|${typeof location !== 'undefined' ? location.origin : ''}`)

    return crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        salt,
        iterations: 120_000,
        hash: 'SHA-256'
      },
      keyMaterial,
      {
        name: 'AES-GCM',
        length: 256
      },
      false,
      ['encrypt', 'decrypt']
    )
  })()

  return derivedKeyPromise
}

const encryptString = async (plaintext: string): Promise<{ iv: string; data: string }> => {
  const key = await getDerivedKey()
  const ivBytes = crypto.getRandomValues(new Uint8Array(12))
  const cipherBuffer = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv: ivBytes },
    key,
    textEncoder.encode(plaintext)
  )

  return {
    iv: toBase64(ivBytes.buffer),
    data: toBase64(cipherBuffer)
  }
}

const decryptString = async (record: { iv: string; data: string }): Promise<string> => {
  const key = await getDerivedKey()
  const iv = new Uint8Array(fromBase64(record.iv))
  const cipherBuffer = fromBase64(record.data)
  const plainBuffer = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, cipherBuffer)
  return textDecoder.decode(plainBuffer)
}

const parseStoredRecord = (raw: string | null): RememberedCredentialsRecordV1 | null => {
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as Partial<RememberedCredentialsRecordV1>
    if (parsed.v !== 1) return null
    if (typeof parsed.exp !== 'number' || !Number.isFinite(parsed.exp)) return null
    if (typeof parsed.iv !== 'string' || !parsed.iv) return null
    if (typeof parsed.data !== 'string' || !parsed.data) return null
    return parsed as RememberedCredentialsRecordV1
  } catch {
    return null
  }
}

export const saveRememberedCredentials = async (email: string, password: string): Promise<void> => {
  if (!isRememberCredentialsAllowed()) {
    return
  }

  const normalizedEmail = String(email || '').trim()
  const rawPassword = String(password || '')

  if (!normalizedEmail || !rawPassword) {
    clearRememberedCredentials()
    return
  }

  const now = Date.now()
  const exp = now + SEVEN_DAYS_MS
  const payload: RememberedCredentialsPayload = {
    email: normalizedEmail,
    password: rawPassword
  }

  const encrypted = await encryptString(JSON.stringify(payload))
  const record: RememberedCredentialsRecordV1 = { v: 1, exp, ...encrypted }
  localStorage.setItem(DATA_KEY, JSON.stringify(record))
}

export const loadRememberedCredentials = async (): Promise<RememberedCredentialsPayload | null> => {
  const record = parseStoredRecord(localStorage.getItem(DATA_KEY))
  if (!record) {
    if (localStorage.getItem(DATA_KEY)) {
      clearRememberedCredentials()
    }
    return null
  }

  if (Date.now() >= record.exp) {
    clearRememberedCredentials()
    return null
  }

  try {
    const decrypted = await decryptString(record)
    const payload = JSON.parse(decrypted) as Partial<RememberedCredentialsPayload>
    const email = typeof payload.email === 'string' ? payload.email.trim() : ''
    const password = typeof payload.password === 'string' ? payload.password : ''

    if (!email || !password) {
      clearRememberedCredentials()
      return null
    }

    return { email, password }
  } catch {
    clearRememberedCredentials()
    return null
  }
}

export const hasValidRememberedCredentials = async (): Promise<boolean> => {
  if (!isRememberCredentialsAllowed()) {
    return false
  }
  const payload = await loadRememberedCredentials()
  return Boolean(payload?.email && payload?.password)
}

