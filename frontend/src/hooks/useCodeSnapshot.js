import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../lib/api'

const AUTOSAVE_DELAY_MS = 2500

/**
 * Loads the latest snapshot on join and persists changes: manually via {@link save}, or
 * automatically once edits go quiet for {@link AUTOSAVE_DELAY_MS}.
 *
 * Only OWNER/EDITOR clients autosave — the server enforces this too, so a VIEWER that skipped the
 * check would simply be rejected.
 */
export function useCodeSnapshot({ roomId, canEdit }) {
  const [snapshot, setSnapshot] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)

  const contentRef = useRef('')
  const savedContentRef = useRef(null)
  const timerRef = useRef(null)
  const inFlightRef = useRef(false)

  useEffect(() => {
    if (!roomId) return undefined
    let cancelled = false

    api
      .latestSnapshot(roomId)
      .then((latest) => {
        if (cancelled) return
        setSnapshot(latest)
        savedContentRef.current = latest.content
        contentRef.current = latest.content
        setStatus('idle')
      })
      .catch((loadError) => {
        if (cancelled) return
        setError(loadError.message)
        setStatus('error')
      })

    return () => {
      cancelled = true
    }
  }, [roomId])

  const save = useCallback(async () => {
    if (!canEdit || inFlightRef.current) return null
    const content = contentRef.current
    if (content === savedContentRef.current) return null

    inFlightRef.current = true
    setStatus('saving')
    try {
      const saved = await api.saveSnapshot(roomId, content)
      savedContentRef.current = content
      setSnapshot(saved)
      setError(null)
      // Edits that landed mid-request leave us dirty again; the next autosave tick picks them up.
      setStatus(contentRef.current === content ? 'saved' : 'unsaved')
      return saved
    } catch (saveError) {
      setError(saveError.message)
      setStatus('error')
      return null
    } finally {
      inFlightRef.current = false
    }
  }, [roomId, canEdit])

  const handleContentChange = useCallback(
    (content) => {
      contentRef.current = content
      if (!canEdit || content === savedContentRef.current) return

      setStatus('unsaved')
      clearTimeout(timerRef.current)
      timerRef.current = setTimeout(save, AUTOSAVE_DELAY_MS)
    },
    [canEdit, save],
  )

  // Flush a pending autosave rather than losing it when the editor unmounts.
  useEffect(
    () => () => {
      clearTimeout(timerRef.current)
      if (canEdit && contentRef.current !== savedContentRef.current) save()
    },
    [canEdit, save],
  )

  return { snapshot, status, error, save, handleContentChange }
}
