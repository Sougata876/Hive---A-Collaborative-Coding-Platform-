import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { createStompClient } from '../lib/stomp'

/**
 * Owns a single STOMP connection for a room screen.
 *
 * The client is created once per mount and shared by the editor, chat and presence hooks — opening
 * one socket per feature would multiply presence rows and reconnect storms. `connected` gates those
 * hooks so nothing subscribes before the CONNECT frame is authenticated.
 */
export function useRoomSocket() {
  const [client, setClient] = useState(null)
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState(null)
  const mountedRef = useRef(true)

  useEffect(() => {
    mountedRef.current = true
    const stomp = createStompClient({
      onConnect: () => {
        if (!mountedRef.current) return
        setConnected(true)
        setError(null)
      },
      onDisconnect: () => mountedRef.current && setConnected(false),
      onError: (message) => mountedRef.current && setError(message),
    })
    setClient(stomp)

    return () => {
      mountedRef.current = false
      stomp.deactivate()
    }
  }, [])

  const clearError = useCallback(() => setError(null), [])

  return useMemo(() => ({ client, connected, error, clearError }), [client, connected, error, clearError])
}
