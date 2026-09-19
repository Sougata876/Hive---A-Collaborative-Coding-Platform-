import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../lib/api'
import { parseFrame, roomDestinations, roomTopics } from '../lib/stomp'

const PAGE_SIZE = 50
const MAX_BUFFERED_MESSAGES = 300

/**
 * Room chat: loads the newest history over REST and keeps the list live over the room's STOMP
 * connection. History pages backwards through `beforeId` cursors so a busy room never reloads the
 * whole transcript.
 */
export function useChat({ client, connected, roomId }) {
  const [messages, setMessages] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)
  const [cursor, setCursor] = useState(null)
  const [hasMore, setHasMore] = useState(false)
  const bufferRef = useRef([])

  useEffect(() => {
    if (!roomId) return undefined
    let cancelled = false

    setStatus('loading')
    api
      .chatHistory(roomId, { size: PAGE_SIZE })
      .then((page) => {
        if (cancelled) return
        setMessages(page.messages)
        setHasMore(page.hasMore)
        setCursor(page.nextBeforeId)
        setError(null)
        setStatus('ready')
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

  // Messages that arrive while history is still loading are replayed after it lands.
  useEffect(() => {
    if (!client || !connected || !roomId) return undefined

    const subscription = client.subscribe(roomTopics.chat(roomId), (frame) => {
      const message = parseFrame(frame.body)
      if (!message?.id) return
      if (status !== 'ready') {
        bufferRef.current.push(message)
        return
      }
      setMessages((current) => appendMessage(current, message))
    })

    return () => subscription.unsubscribe()
  }, [client, connected, roomId, status])

  useEffect(() => {
    if (status !== 'ready' || bufferRef.current.length === 0) return
    const buffered = bufferRef.current
    bufferRef.current = []
    setMessages((current) => buffered.reduce((all, message) => appendMessage(all, message), current))
  }, [status])

  const loadOlder = useCallback(async () => {
    if (!hasMore || !cursor || status === 'loadingOlder') return
    setStatus('loadingOlder')
    try {
      const page = await api.chatHistory(roomId, { size: PAGE_SIZE, beforeId: cursor })
      setMessages((current) => mergeById([...page.messages, ...current]))
      setHasMore(page.hasMore)
      setCursor(page.nextBeforeId)
      setStatus('ready')
    } catch (loadError) {
      setError(loadError.message)
      setStatus('ready')
    }
  }, [hasMore, cursor, roomId, status])

  const send = useCallback(
    async (content) => {
      const trimmed = content.trim()
      if (!trimmed || !client?.connected) return false
      client.publish({ destination: roomDestinations.chat(roomId), body: JSON.stringify({ content: trimmed }) })
      return true
    },
    [client, roomId],
  )

  return { messages, status, error, hasMore, loadOlder, send }
}

function appendMessage(current, message) {
  if (current.some((existing) => existing.id === message.id)) return current
  const next = mergeById([...current, message])
  return next.length > MAX_BUFFERED_MESSAGES ? next.slice(-MAX_BUFFERED_MESSAGES) : next
}

function mergeById(list) {
  const byId = new Map()
  list.forEach((message) => byId.set(message.id, message))
  return [...byId.values()].sort((left, right) =>
    left.sentAt === right.sentAt ? left.id - right.id : left.sentAt < right.sentAt ? -1 : 1,
  )
}
