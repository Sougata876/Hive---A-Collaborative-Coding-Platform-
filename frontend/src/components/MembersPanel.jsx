import { useEffect, useMemo, useRef, useState } from 'react'
import { Avatar, Badge, Button, RoleBadge, Select, Spinner, formatDateTime } from './ui'

const ASSIGNABLE_ROLES = ['EDITOR', 'VIEWER']
const CONFIRM_WINDOW_MS = 3000

/**
 * Room roster with live presence. Owners get inline role and removal controls; everyone else
 * sees a read-only list. Ownership is never offered as a target role — the server rejects it.
 */
export default function MembersPanel({
  members,
  presentUserIds,
  currentUserId,
  myRole,
  busyUserId,
  onChangeRole,
  onRemove,
}) {
  const [pendingRemoveId, setPendingRemoveId] = useState(null)
  const confirmTimer = useRef(null)

  useEffect(() => () => clearTimeout(confirmTimer.current), [])

  // The caller may hand us a Set or a plain array, and ids arrive as numbers over REST but can
  // come back as strings over STOMP, so normalise both shape and type once per change.
  const onlineIds = useMemo(() => {
    const source = presentUserIds instanceof Set || Array.isArray(presentUserIds) ? presentUserIds : []
    return new Set(Array.from(source, Number))
  }, [presentUserIds])

  const roster = useMemo(() => {
    const list = Array.isArray(members) ? members.slice() : []
    return list.sort((a, b) => {
      const aOnline = onlineIds.has(Number(a.userId))
      const bOnline = onlineIds.has(Number(b.userId))
      if (aOnline !== bOnline) return aOnline ? -1 : 1
      return (a.username ?? '').localeCompare(b.username ?? '', undefined, { sensitivity: 'base' })
    })
  }, [members, onlineIds])

  const onlineCount = roster.reduce((total, member) => total + (onlineIds.has(Number(member.userId)) ? 1 : 0), 0)
  const isOwner = myRole === 'OWNER'

  function handleRemoveClick(userId) {
    clearTimeout(confirmTimer.current)
    if (pendingRemoveId === userId) {
      setPendingRemoveId(null)
      onRemove?.(userId)
      return
    }
    setPendingRemoveId(userId)
    confirmTimer.current = setTimeout(() => setPendingRemoveId(null), CONFIRM_WINDOW_MS)
  }

  function handleRoleChange(userId, role) {
    onChangeRole?.(userId, role)
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <header className="flex items-center justify-between gap-2 border-b border-border px-4 py-3">
        <div className="flex items-baseline gap-2">
          <h2 className="text-sm font-semibold text-ink">Members</h2>
          <span className="text-xs text-muted">{roster.length}</span>
        </div>
        <Badge tone="link">{onlineCount} online</Badge>
      </header>

      {roster.length === 0 ? (
        <p className="px-4 py-6 text-sm text-muted">No members yet.</p>
      ) : (
        <ul className="min-h-0 flex-1 overflow-y-auto px-2 py-2">
          {roster.map((member) => {
            const online = onlineIds.has(Number(member.userId))
            const isSelf = currentUserId != null && Number(member.userId) === Number(currentUserId)
            const busy = busyUserId != null && Number(busyUserId) === Number(member.userId)
            const canManage = isOwner && !isSelf && member.role !== 'OWNER'
            const confirming = pendingRemoveId === member.userId

            return (
              <li
                key={member.userId}
                className={`rounded-lg px-2 py-2 transition-colors hover:bg-canvas/40 ${online ? '' : 'opacity-55'}`}
              >
                <div className="flex items-start gap-3">
                  <span className="relative shrink-0">
                    <Avatar
                      user={{ id: member.userId, username: member.username, avatarUrl: member.avatarUrl }}
                      size="sm"
                    />
                    <span
                      aria-hidden="true"
                      className={`absolute -right-0.5 -bottom-0.5 h-2.5 w-2.5 rounded-full ring-2 ring-canvas ${
                        online ? 'bg-emerald-400' : 'bg-muted'
                      }`}
                    />
                  </span>

                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                      <span className="truncate text-sm text-ink">{member.username}</span>
                      {isSelf && <span className="text-xs text-muted">(you)</span>}
                      <RoleBadge role={member.role} />
                      <span className="sr-only">{online ? 'Online' : 'Offline'}</span>
                    </div>
                    <p className="mt-0.5 text-[11px] text-muted">Joined {formatDateTime(member.joinedAt)}</p>
                  </div>
                </div>

                {canManage && (
                  <div className="mt-2 flex items-center gap-2 pl-10">
                    <div className="w-24">
                      {/* Trailing `!` beats the primitive's own px-3/py-2/text-sm so the control
                          matches the sm Button next to it; Tailwind v4 orders conflicts by value. */}
                      <Select
                        aria-label={`Role for ${member.username}`}
                        value={member.role}
                        disabled={busy}
                        onChange={(event) => handleRoleChange(member.userId, event.target.value)}
                        className="px-2! py-1! text-xs!"
                      >
                        {ASSIGNABLE_ROLES.map((role) => (
                          <option key={role} value={role}>
                            {role}
                          </option>
                        ))}
                      </Select>
                    </div>

                    <Button
                      type="button"
                      variant="danger"
                      size="sm"
                      disabled={busy}
                      aria-label={
                        confirming ? `Confirm removing ${member.username}` : `Remove ${member.username} from this room`
                      }
                      onClick={() => handleRemoveClick(member.userId)}
                    >
                      {confirming ? 'Confirm?' : 'Remove'}
                    </Button>

                    {busy && <Spinner className="h-4 w-4" />}
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
