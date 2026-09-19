/**
 * Shared date and time formatting utilities.
 */

export function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  const today = new Date()
  const sameDay = date.toDateString() === today.toDateString()
  return sameDay
    ? date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : date.toLocaleDateString([], { month: 'short', day: 'numeric' })
}

export function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString([], {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatMonthYear(value) {
  if (!value) return 'September 2025'
  const date = new Date(value)
  return date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
}
