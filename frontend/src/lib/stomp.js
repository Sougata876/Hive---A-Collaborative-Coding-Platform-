import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { API_BASE, getAccessToken } from './api'

/**
 * Creates a STOMP client authenticated by the JWT in the CONNECT frame, matching the backend's
 * ChannelInterceptor. The handshake alone is not trusted by the server.
 */
export function createStompClient({ onConnect, onDisconnect, onError } = {}) {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${API_BASE}/ws`),
    connectHeaders: { Authorization: `Bearer ${getAccessToken() ?? ''}` },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {},
    // Refresh the token on every reconnect attempt so a rotated token is picked up.
    beforeConnect: () => {
      client.connectHeaders = { Authorization: `Bearer ${getAccessToken() ?? ''}` }
    },
    onConnect: () => onConnect?.(client),
    onWebSocketClose: () => onDisconnect?.(),
    onStompError: (frame) => onError?.(frame.headers?.message ?? 'Connection error'),
  })

  client.activate()
  return client
}

export const roomTopics = {
  code: (roomId) => `/topic/room/${roomId}/code`,
  chat: (roomId) => `/topic/room/${roomId}/chat`,
  presence: (roomId) => `/topic/room/${roomId}/presence`,
}

export const roomDestinations = {
  edit: (roomId) => `/app/room/${roomId}/edit`,
  sync: (roomId) => `/app/room/${roomId}/sync`,
  chat: (roomId) => `/app/room/${roomId}/chat`,
  presence: (roomId) => `/app/room/${roomId}/presence`,
}

/** Safely parses a STOMP frame body, returning null for anything that isn't the expected JSON. */
export function parseFrame(body) {
  try {
    return JSON.parse(body)
  } catch {
    return null
  }
}
