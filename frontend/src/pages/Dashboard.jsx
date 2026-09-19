import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../hooks/useAuth'
import {
  Avatar,
  Badge,
  Button,
  Card,
  HiveLogo,
  EmptyState,
  ErrorText,
  Input,
  Modal,
  RoleBadge,
  Select,
  Spinner,
} from '../components/ui'

const NETWORK_ERROR = 'Something went wrong. Check your connection and try again.'

function messageOf(error) {
  return error instanceof ApiError ? error.message : NETWORK_ERROR
}

/**
 * Room Card matching Screen 4:
 * Square icon, Title, Language badge, Activity, Role badge, and "Enter →" button.
 */
function RoomCard({ room, onOpen }) {
  const [copied, setCopied] = useState(false)
  const timerRef = useRef(null)

  useEffect(() => () => clearTimeout(timerRef.current), [])

  async function handleCopy(e) {
    e.stopPropagation()
    clearTimeout(timerRef.current)
    try {
      await navigator.clipboard.writeText(room.inviteCode)
      setCopied(true)
    } catch {
      setCopied(false)
    }
    timerRef.current = setTimeout(() => setCopied(false), 1800)
  }

  return (
    <Card
      onClick={onOpen}
      className="group relative cursor-pointer p-4 transition-all duration-200 hover:border-steel-border/40 hover:bg-[#181a27] focus:outline-none"
    >
      <div className="flex items-start justify-between gap-4">
        {/* Left: Icon and info */}
        <div className="flex items-start gap-3.5 min-w-0 flex-1">
          {/* Square indigo/purple icon matching screenshot */}
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-600/30 to-purple-600/20 border border-indigo-500/30 text-periwinkle-glow">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
            </svg>
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="truncate text-sm font-semibold text-pure-white group-hover:text-periwinkle-glow transition-colors">
                {room.name}
              </h3>
              <Badge tone="zinc">{room.language}</Badge>
            </div>

            <p className="mt-1 text-xs text-muted-steel">
              {room.memberCount || 1} members &bull; Active now
            </p>
          </div>
        </div>

        {/* Right: Role and Enter button */}
        <div className="flex items-center gap-3 shrink-0">
          <RoleBadge role={room.role} />
          <Button
            size="sm"
            variant="ghost"
            className="border border-gunmetal hover:border-steel-border/50 text-xs px-3 py-1"
            onClick={(e) => {
              e.stopPropagation()
              onOpen()
            }}
          >
            Enter <span className="text-xs">→</span>
          </Button>
        </div>
      </div>

      {room.inviteCode && (
        <div className="mt-3.5 flex items-center justify-between rounded-lg border border-gunmetal/60 bg-void-black/60 px-3 py-1.5 text-xs">
          <div className="flex items-center gap-2 font-mono">
            <span className="text-[10px] uppercase tracking-wider text-muted-steel">Invite:</span>
            <span className="text-periwinkle-glow font-medium">{room.inviteCode}</span>
          </div>
          <button
            type="button"
            onClick={handleCopy}
            className="text-[11px] text-muted-steel hover:text-frost transition-colors"
          >
            {copied ? 'Copied ✓' : 'Copy'}
          </button>
        </div>
      )}
    </Card>
  )
}

/**
 * Screen 5: Create Room Modal
 */
function CreateRoomModal({ onClose, onCreated }) {
  const [name, setName] = useState('')
  const [language, setLanguage] = useState('JAVA')
  const [autoInvite, setAutoInvite] = useState(true)
  const [nameError, setNameError] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function requestClose() {
    if (!submitting) onClose()
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (submitting) return

    const trimmed = name.trim()
    if (!trimmed) {
      setNameError('Room name is required.')
      return
    }
    if (trimmed.length > 100) {
      setNameError('Room name must be 100 characters or fewer.')
      return
    }

    setNameError('')
    setError('')
    setSubmitting(true)
    try {
      const room = await api.createRoom({ name: trimmed, language })
      onCreated(room)
    } catch (err) {
      setError(messageOf(err))
      if (err instanceof ApiError) setNameError(err.fieldErrors?.name ?? '')
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Create a New Room" onClose={requestClose}>
      <form id="create-room-form" onSubmit={handleSubmit} noValidate className="space-y-4">
        <Input
          label="Room Name"
          value={name}
          onChange={(event) => setName(event.target.value)}
          maxLength={100}
          placeholder="e.g. DSA Practice"
          autoFocus
          required
          error={nameError}
        />

        <Select label="Programming Language" value={language} onChange={(e) => setLanguage(e.target.value)}>
          <option value="JAVA">Java</option>
        </Select>

        <label className="flex items-center gap-2.5 pt-1 text-xs text-muted-steel cursor-pointer hover:text-frost select-none">
          <input
            type="checkbox"
            checked={autoInvite}
            onChange={(e) => setAutoInvite(e.target.checked)}
            className="h-3.5 w-3.5 rounded border-gunmetal bg-void-black text-iris-blue focus:ring-periwinkle-glow/30"
          />
          <span>Generate invite code automatically</span>
        </label>

        <ErrorText>{error}</ErrorText>

        <div className="pt-2">
          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting ? 'Creating Room…' : 'Create Room'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

/**
 * Join Room Modal
 */
function JoinRoomModal({ onClose, onJoined }) {
  const [code, setCode] = useState('')
  const [codeError, setCodeError] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function requestClose() {
    if (!submitting) onClose()
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (submitting) return

    const trimmed = code.trim().toUpperCase()
    if (!trimmed) {
      setCodeError('Invite code is required.')
      return
    }

    setCodeError('')
    setError('')
    setSubmitting(true)
    try {
      const room = await api.joinRoom(trimmed)
      onJoined(room)
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setError('No active room matches that invite code.')
      } else {
        setError(messageOf(err))
        if (err instanceof ApiError) setCodeError(err.fieldErrors?.inviteCode ?? '')
      }
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Join a Room" onClose={requestClose}>
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <Input
          label="Invite code"
          value={code}
          onChange={(event) => setCode(event.target.value.toUpperCase())}
          maxLength={32}
          placeholder="e.g. A7K9M2"
          autoFocus
          required
          autoComplete="off"
          spellCheck={false}
          error={codeError}
          hint="Enter the 6-8 character code provided by the room owner."
          className="font-mono tracking-widest text-center text-base"
        />
        <ErrorText>{error}</ErrorText>

        <div className="pt-2">
          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting ? 'Joining…' : 'Join Room'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

/**
 * Screen 4: Dashboard / My Rooms Page
 */
export default function Dashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const [rooms, setRooms] = useState([])
  const [status, setStatus] = useState('loading')
  const [loadError, setLoadError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [modal, setModal] = useState(null)

  // Filters and search
  const [searchQuery, setSearchQuery] = useState('')
  const [activeTab, setActiveTab] = useState('all') // 'all' | 'owned' | 'shared'

  // Quick join input
  const [quickCode, setQuickCode] = useState('')
  const [quickJoinLoading, setQuickJoinLoading] = useState(false)
  const [quickJoinError, setQuickJoinError] = useState('')

  useEffect(() => {
    let cancelled = false
    setStatus('loading')
    setLoadError('')

    api
      .rooms()
      .then((data) => {
        if (cancelled) return
        setRooms(Array.isArray(data) ? data : [])
        setStatus('ready')
      })
      .catch((err) => {
        if (cancelled) return
        setLoadError(messageOf(err))
        setStatus('error')
      })

    return () => {
      cancelled = true
    }
  }, [reloadKey])

  function openRoom(roomId) {
    navigate(`/rooms/${roomId}`)
  }

  async function handleQuickJoin(e) {
    e.preventDefault()
    const trimmed = quickCode.trim().toUpperCase()
    if (!trimmed) return

    setQuickJoinLoading(true)
    setQuickJoinError('')
    try {
      const room = await api.joinRoom(trimmed)
      openRoom(room.id)
    } catch (err) {
      setQuickJoinError(
        err instanceof ApiError && err.status === 404
          ? 'Invalid invite code.'
          : err instanceof ApiError
          ? err.message
          : 'Could not join room.',
      )
    } finally {
      setQuickJoinLoading(false)
    }
  }

  // Filtered rooms
  const filteredRooms = useMemo(() => {
    return rooms.filter((room) => {
      const matchesSearch =
        !searchQuery.trim() ||
        room.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        room.inviteCode?.toLowerCase().includes(searchQuery.toLowerCase())

      if (!matchesSearch) return false

      if (activeTab === 'owned') return room.role === 'OWNER'
      if (activeTab === 'shared') return room.role !== 'OWNER'
      return true
    })
  }, [rooms, searchQuery, activeTab])

  return (
    <div className="min-h-screen bg-void-black text-frost">
      {/* ─── Top Navigation Bar matching Screen 4 ──────────────────────── */}
      <header className="sticky top-0 z-40 border-b border-gunmetal bg-void-black/90 backdrop-blur-md">
        <div className="mx-auto flex h-14 w-full items-center justify-between gap-4 px-4 sm:px-6">
          <Link to="/rooms" className="transition-opacity hover:opacity-90">
            <HiveLogo size="md" />
          </Link>

          {/* Search bar matching screenshot */}
          <div className="relative max-w-md flex-1 hidden sm:block">
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-muted-steel">
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              placeholder="Search rooms..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-full border border-gunmetal bg-carbon-surface/80 py-1.5 pl-9 pr-4 text-xs text-pure-white placeholder-muted-steel outline-none transition-colors focus:border-periwinkle-glow focus:ring-1 focus:ring-periwinkle-glow/30"
            />
          </div>

          {/* Right actions: Bell + User profile */}
          <div className="flex items-center gap-3">
            <button
              type="button"
              className="flex h-8 w-8 items-center justify-center rounded-full text-muted-steel hover:bg-carbon-surface hover:text-pure-white transition-colors"
              title="Notifications"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </button>

            <Link
              to="/profile"
              className="flex items-center gap-2 rounded-full border border-gunmetal bg-carbon-surface/60 p-1 pr-3 transition-colors hover:border-steel-border/50 hover:bg-carbon-surface"
            >
              <Avatar user={user} size="sm" />
              <span className="text-xs font-medium text-pure-white hidden md:inline-block">
                {user?.username}
              </span>
            </Link>
          </div>
        </div>
      </header>

      {/* ─── Main Two-Column Layout ────────────────────────────────────── */}
      <div className="mx-auto flex max-w-7xl px-4 py-8 sm:px-6 gap-8">
        {/* Left Sidebar Navigation matching Screen 4 */}
        <aside className="w-56 shrink-0 hidden md:block">
          <nav className="space-y-1">
            <Link
              to="/rooms"
              className="flex items-center gap-3 rounded-xl bg-carbon-surface px-3.5 py-2.5 text-xs font-medium text-pure-white border border-gunmetal inset-rim"
            >
              <svg className="h-4 w-4 text-periwinkle-glow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
              <span>My Rooms</span>
            </Link>

            <button
              type="button"
              onClick={() => setModal('create')}
              className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-medium text-muted-steel hover:bg-carbon-surface hover:text-pure-white transition-colors"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 4v16m8-8H4" />
              </svg>
              <span>Create Room</span>
            </button>

            <button
              type="button"
              onClick={() => setModal('join')}
              className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-medium text-muted-steel hover:bg-carbon-surface hover:text-pure-white transition-colors"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
              </svg>
              <span>Join Room</span>
            </button>

            <Link
              to="/profile"
              className="flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-medium text-muted-steel hover:bg-carbon-surface hover:text-pure-white transition-colors"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <span>Profile</span>
            </Link>
          </nav>
        </aside>

        {/* Right Main Content */}
        <main className="flex-1 min-w-0">
          {/* Header Row */}
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <h1 className="text-2xl font-medium text-pure-white">My Rooms</h1>
              <p className="mt-1 text-xs text-muted-steel">Your collaborative coding spaces.</p>
            </div>
            <Button onClick={() => setModal('create')} size="sm">
              <span className="font-bold">+</span> Create Room
            </Button>
          </div>

          {/* Quick Join Bar matching Screen 4 */}
          <Card className="mt-6 p-4">
            <form onSubmit={handleQuickJoin} className="flex flex-wrap items-center gap-3">
              <div className="relative flex-1 min-w-[200px]">
                <input
                  type="text"
                  placeholder="Enter invite code"
                  value={quickCode}
                  onChange={(e) => setQuickCode(e.target.value.toUpperCase())}
                  className="w-full rounded-[10px] border border-gunmetal bg-void-black px-3.5 py-2 font-mono text-xs text-pure-white placeholder-muted-steel uppercase tracking-wider outline-none focus:border-periwinkle-glow"
                />
              </div>
              <Button type="submit" size="sm" disabled={quickJoinLoading || !quickCode.trim()}>
                {quickJoinLoading ? 'Joining…' : 'Join Room'}
              </Button>
            </form>
            {quickJoinError && <ErrorText>{quickJoinError}</ErrorText>}
          </Card>

          {/* Tabs row: All Rooms, Owned, Shared */}
          <div className="mt-6 flex items-center gap-2 border-b border-gunmetal pb-3">
            <button
              type="button"
              onClick={() => setActiveTab('all')}
              className={`rounded-full px-3.5 py-1 text-xs font-medium transition-colors ${
                activeTab === 'all'
                  ? 'bg-carbon-surface text-pure-white border border-gunmetal inset-rim'
                  : 'text-muted-steel hover:text-frost'
              }`}
            >
              All Rooms ({rooms.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('owned')}
              className={`rounded-full px-3.5 py-1 text-xs font-medium transition-colors ${
                activeTab === 'owned'
                  ? 'bg-carbon-surface text-pure-white border border-gunmetal inset-rim'
                  : 'text-muted-steel hover:text-frost'
              }`}
            >
              Owned ({rooms.filter((r) => r.role === 'OWNER').length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('shared')}
              className={`rounded-full px-3.5 py-1 text-xs font-medium transition-colors ${
                activeTab === 'shared'
                  ? 'bg-carbon-surface text-pure-white border border-gunmetal inset-rim'
                  : 'text-muted-steel hover:text-frost'
              }`}
            >
              Shared ({rooms.filter((r) => r.role !== 'OWNER').length})
            </button>
          </div>

          {/* Rooms Grid / States */}
          <div className="mt-6">
            {status === 'loading' && (
              <div className="grid place-items-center py-20">
                <Spinner className="h-7 w-7 text-periwinkle-glow" />
              </div>
            )}

            {status === 'error' && (
              <Card className="p-6">
                <p className="text-sm font-medium text-pure-white">Could not load your rooms.</p>
                <ErrorText>{loadError}</ErrorText>
                <div className="mt-4">
                  <Button variant="outline" size="sm" onClick={() => setReloadKey((k) => k + 1)}>
                    Try again
                  </Button>
                </div>
              </Card>
            )}

            {status === 'ready' && filteredRooms.length === 0 && (
              <Card>
                <EmptyState
                  title={searchQuery ? 'No rooms match your search' : 'No rooms found'}
                  action={
                    <Button onClick={() => setModal('create')} size="sm">
                      + Create a Room
                    </Button>
                  }
                >
                  {searchQuery
                    ? 'Try searching with a different term or clear the search input.'
                    : 'Create a room to start coding together, or enter an invite code to join an existing workspace.'}
                </EmptyState>
              </Card>
            )}

            {status === 'ready' && filteredRooms.length > 0 && (
              <div className="grid grid-cols-1 gap-3.5">
                {filteredRooms.map((room) => (
                  <RoomCard key={room.id} room={room} onOpen={() => openRoom(room.id)} />
                ))}
              </div>
            )}
          </div>
        </main>
      </div>

      {/* Modals */}
      {modal === 'create' && (
        <CreateRoomModal onClose={() => setModal(null)} onCreated={(room) => openRoom(room.id)} />
      )}
      {modal === 'join' && (
        <JoinRoomModal onClose={() => setModal(null)} onJoined={(room) => openRoom(room.id)} />
      )}
    </div>
  )
}
