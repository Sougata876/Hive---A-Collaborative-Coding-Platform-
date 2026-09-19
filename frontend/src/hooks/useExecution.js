import { useCallback, useRef, useState } from 'react'
import { api } from '../lib/api'

/**
 * Runs the current buffer in the server-side Docker sandbox.
 *
 * Execution is deliberately REST rather than WebSocket: it is a long, request/response-shaped
 * operation with a hard server timeout, and the backend rate-limits and queues it per room. Only one
 * run may be in flight per client, so a mashing user cannot pile up containers.
 */
export function useExecution({ roomId, canRun }) {
  const [result, setResult] = useState(null)
  const [running, setRunning] = useState(false)
  const [error, setError] = useState(null)
  const inFlightRef = useRef(false)

  const run = useCallback(
    async (code) => {
      if (!canRun || inFlightRef.current) return null
      if (!code.trim()) {
        setError('Nothing to run — the editor is empty.')
        return null
      }

      inFlightRef.current = true
      setRunning(true)
      setError(null)
      try {
        const response = await api.execute(roomId, code)
        setResult(response)
        return response
      } catch (runError) {
        setError(runError.message)
        setResult(null)
        return null
      } finally {
        setRunning(false)
        inFlightRef.current = false
      }
    },
    [roomId, canRun],
  )

  const clear = useCallback(() => {
    setResult(null)
    setError(null)
  }, [])

  return { result, running, error, run, clear }
}
