const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const ACCESS_TOKEN_KEY = 'hive.accessToken'
const REFRESH_TOKEN_KEY = 'hive.refreshToken'

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function storeTokens({ accessToken, refreshToken }) {
  if (accessToken) localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = body?.fieldErrors ?? {}
  }
}

async function parseBody(response) {
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}

/** Exchanges the stored refresh token for a new pair. Returns false when re-login is required. */
async function refreshSession() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false

  const response = await fetch(`${API_BASE}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!response.ok) {
    clearTokens()
    return false
  }
  storeTokens(await response.json())
  return true
}

/**
 * Calls the Hive API with the stored access token, retrying once after a silent refresh so an
 * expired token doesn't interrupt an active session.
 */
export async function apiFetch(path, { method = 'GET', body, auth = true, retry = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    const token = getAccessToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 401 && auth && retry && (await refreshSession())) {
    return apiFetch(path, { method, body, auth, retry: false })
  }
  if (!response.ok) {
    throw new ApiError(response.status, await parseBody(response))
  }
  return response.status === 204 ? null : parseBody(response)
}

/** Named API operations so pages never hand-build URLs. */
export const api = {
  register: (payload) =>
    apiFetch('/api/auth/register', { method: 'POST', body: payload, auth: false }),
  login: (payload) => apiFetch('/api/auth/login', { method: 'POST', body: payload, auth: false }),
  me: () => apiFetch('/api/users/me'),
  updateProfile: (payload) => apiFetch('/api/users/me', { method: 'PUT', body: payload }),

  rooms: () => apiFetch('/api/rooms'),
  createRoom: (payload) => apiFetch('/api/rooms', { method: 'POST', body: payload }),
  joinRoom: (inviteCode) => apiFetch('/api/rooms/join', { method: 'POST', body: { inviteCode } }),
  room: (roomId) => apiFetch(`/api/rooms/${roomId}`),
  members: (roomId) => apiFetch(`/api/rooms/${roomId}/members`),
  updateRole: (roomId, userId, role) =>
    apiFetch(`/api/rooms/${roomId}/members/${userId}/role`, { method: 'PATCH', body: { role } }),
  removeMember: (roomId, userId) =>
    apiFetch(`/api/rooms/${roomId}/members/${userId}`, { method: 'DELETE' }),
  leaveRoom: (roomId) => apiFetch(`/api/rooms/${roomId}/members/me`, { method: 'DELETE' }),
  deleteRoom: (roomId) => apiFetch(`/api/rooms/${roomId}`, { method: 'DELETE' }),

  chatHistory: (roomId, { size = 50, beforeId } = {}) => {
    const query = new URLSearchParams({ size: String(size) })
    if (beforeId) query.set('beforeId', String(beforeId))
    return apiFetch(`/api/rooms/${roomId}/messages?${query}`)
  },

  latestSnapshot: (roomId) => apiFetch(`/api/rooms/${roomId}/snapshots/latest`),
  saveSnapshot: (roomId, content) =>
    apiFetch(`/api/rooms/${roomId}/snapshots`, { method: 'POST', body: { content } }),

  execute: (roomId, code) =>
    apiFetch(`/api/rooms/${roomId}/executions`, { method: 'POST', body: { code } }),
}

export { API_BASE }
