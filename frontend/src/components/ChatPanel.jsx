import { useCallback, useLayoutEffect, useRef, useState } from 'react'
import { Avatar, Button, ErrorText, Spinner } from './ui'
import { formatTime } from '../lib/formatters'

const MAX_LENGTH = 2000
const NEAR_BOTTOM_PX = 80

function textOf(error) {
  if (!error) return ''
  return typeof error === 'string' ? error : error.message ?? 'Something went wrong.'
}

export default function ChatPanel({
  messages = [],
  status = 'ready',
  error,
  hasMore = false,
  onLoadOlder,
  onSend,
  currentUserId,
  connected = false,
}) {
  const [content, setContent] = useState('')
  const [sending, setSending] = useState(false)
  const [sendError, setSendError] = useState('')

  const listRef = useRef(null)
  const atBottomRef = useRef(true)
  const didInitialScrollRef = useRef(false)
  const restoreRef = useRef(null)
  const prevRef = useRef({ firstId: null, lastId: null, count: 0 })

  const scrollToBottom = useCallback((behavior) => {
    const el = listRef.current
    if (!el) return
    atBottomRef.current = true
    if (behavior === 'smooth') el.scrollTo({ top: el.scrollHeight, behavior })
    else el.scrollTop = el.scrollHeight
  }, [])

  const handleScroll = useCallback(() => {
    const el = listRef.current
    if (!el) return
    const near = el.scrollHeight - el.scrollTop - el.clientHeight <= NEAR_BOTTOM_PX
    atBottomRef.current = near
  }, [])

  const handleLoadOlder = useCallback(() => {
    const el = listRef.current
    restoreRef.current = el ? { scrollTop: el.scrollTop, scrollHeight: el.scrollHeight } : null
    onLoadOlder?.()
  }, [onLoadOlder])

  useLayoutEffect(() => {
    const el = listRef.current
    const prev = prevRef.current
    const first = messages.length ? messages[0].id : null
    const last = messages.length ? messages[messages.length - 1].id : null
    prevRef.current = { firstId: first, lastId: last, count: messages.length }

    if (!el || messages.length === 0) {
      didInitialScrollRef.current = false
      return
    }

    if (!didInitialScrollRef.current) {
      didInitialScrollRef.current = true
      el.scrollTop = el.scrollHeight
      return
    }

    if (restoreRef.current && prev.firstId !== null && first !== prev.firstId) {
      const snapshot = restoreRef.current
      restoreRef.current = null
      el.scrollTop = snapshot.scrollTop + (el.scrollHeight - snapshot.scrollHeight)
      return
    }

    const appended = messages.length > prev.count || last !== prev.lastId
    if (!appended) return
    if (atBottomRef.current) el.scrollTop = el.scrollHeight
  }, [messages])

  async function handleSubmit(event) {
    event?.preventDefault()
    const trimmed = content.trim()
    if (!trimmed || sending) return

    setSending(true)
    setSendError('')
    try {
      await onSend?.(trimmed)
      setContent('')
      requestAnimationFrame(() => scrollToBottom('smooth'))
    } catch (err) {
      setSendError(textOf(err))
    } finally {
      setSending(false)
    }
  }

  function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      handleSubmit()
    }
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      {/* Messages Scroll Area */}
      <div
        ref={listRef}
        onScroll={handleScroll}
        className="min-h-0 flex-1 overflow-y-auto space-y-3.5 p-3"
      >
        {hasMore && (
          <div className="text-center">
            <Button
              variant="ghost"
              size="xs"
              onClick={handleLoadOlder}
              disabled={status === 'loading'}
            >
              {status === 'loading' ? <Spinner className="h-3 w-3" /> : 'Load earlier messages'}
            </Button>
          </div>
        )}

        {messages.length === 0 && (
          <div className="grid place-items-center py-10 text-center">
            <p className="text-xs text-muted-steel">No messages yet.</p>
            <p className="text-[11px] text-muted-steel/80">Start the conversation with your team!</p>
          </div>
        )}

        {messages.map((message) => {
          const isMe = message.userId === currentUserId

          return (
            <div key={message.id} className="flex items-start gap-2.5">
              <Avatar user={{ id: message.userId, username: message.username }} size="sm" />
              <div className="min-w-0 flex-1">
                <div className="flex items-baseline gap-2">
                  <span className={`text-xs font-semibold ${isMe ? 'text-periwinkle-glow' : 'text-pure-white'}`}>
                    {message.username}
                  </span>
                  <span className="text-[10px] text-muted-steel">
                    {formatTime(message.sentAt)}
                  </span>
                </div>
                <p className="mt-0.5 text-xs text-frost whitespace-pre-wrap break-words leading-relaxed">
                  {message.content}
                </p>
              </div>
            </div>
          )
        })}
      </div>

      {/* Message Composer matching Screen 6 */}
      <div className="border-t border-gunmetal p-2.5 bg-void-black/70">
        {(sendError || error) && <ErrorText>{sendError || error}</ErrorText>}
        <form onSubmit={handleSubmit} className="flex items-center gap-2">
          <input
            type="text"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            onKeyDown={handleKeyDown}
            maxLength={MAX_LENGTH}
            placeholder={connected ? 'Type a message…' : 'Connecting to chat…'}
            disabled={!connected || sending}
            className="flex-1 rounded-[10px] border border-gunmetal bg-void-black px-3 py-2 text-xs text-pure-white placeholder-muted-steel outline-none focus:border-periwinkle-glow"
          />
          <button
            type="submit"
            disabled={!connected || sending || !content.trim()}
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-[10px] bg-gradient-to-r from-[#5b63d3] to-[#7c87f7] text-white shadow-sm transition-transform hover:brightness-110 active:scale-95 disabled:opacity-40 disabled:pointer-events-none"
            title="Send"
          >
            {sending ? (
              <Spinner className="h-3.5 w-3.5 text-white" />
            ) : (
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            )}
          </button>
        </form>
      </div>
    </div>
  )
}
