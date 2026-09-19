import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuth } from '../hooks/useAuth'
import { useRoomSocket } from '../hooks/useRoomSocket'
import { usePresence } from '../hooks/usePresence'
import { useChat } from '../hooks/useChat'
import { useCodeSnapshot } from '../hooks/useCodeSnapshot'
import { useExecution } from '../hooks/useExecution'
import CollaborativeEditor, { DEFAULT_SOURCE } from '../components/CollaborativeEditor'
import ChatPanel from '../components/ChatPanel'
import OutputPanel from '../components/OutputPanel'
import RoomSettings from '../components/RoomSettings'
import {
  Badge,
  Button,
  HiveLogo,
  Modal,
  RoleBadge,
  Spinner,
} from '../components/ui'

const SAVE_LABELS = {
  loading: 'Loading…',
  idle: 'Saved',
  saved: 'Saved',
  unsaved: 'Unsaved changes',
  saving: 'Saving…',
  error: 'Save failed',
}

function fileNameFor(language) {
  const lang = (language || 'JAVA').toUpperCase()
  switch (lang) {
    case 'PYTHON':
      return 'main.py'
    case 'JAVASCRIPT':
      return 'index.js'
    case 'TYPESCRIPT':
      return 'index.ts'
    case 'CPP':
      return 'main.cpp'
    case 'CSHARP':
      return 'Program.cs'
    case 'GO':
      return 'main.go'
    case 'RUST':
      return 'main.rs'
    case 'JAVA':
    default:
      return 'Main.java'
  }
}

export default function Room() {
  const { roomId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [room, setRoom] = useState(null)
  const [members, setMembers] = useState([])
  const [loadError, setLoadError] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [copiedCode, setCopiedCode] = useState(false)
  const [confirmLeave, setConfirmLeave] = useState(false)

  // Navigation state: 'code' | 'settings'
  const [activeView, setActiveView] = useState('code')
  // Right sidebar tab: 'split' | 'chat' | 'members'
  const [sidebarTab, setSidebarTab] = useState('split')
  const copyTimer = useRef(null)

  const { client, connected, error: socketError } = useRoomSocket()
  const { users: presentUsers } = usePresence({ client: connected ? client : null, roomId })

  const role = room?.role
  const canEdit = role === 'OWNER' || role === 'EDITOR'

  const chat = useChat({ client, connected, roomId })
  const snapshot = useCodeSnapshot({ roomId, canEdit })
  const execution = useExecution({ roomId, canRun: canEdit })

  const codeRef = useRef('')

  const fetchRoomData = useCallback(async () => {
    try {
      const [loadedRoom, loadedMembers] = await Promise.all([
        api.room(roomId),
        api.members(roomId),
      ])
      setRoom(loadedRoom)
      setMembers(loadedMembers)
    } catch (error) {
      setLoadError(
        error.status === 403 || error.status === 404
          ? 'This room does not exist or you do not have permission to access it.'
          : error.message,
      )
    }
  }, [roomId])

  useEffect(() => {
    fetchRoomData()
  }, [fetchRoomData])

  useEffect(() => () => clearTimeout(copyTimer.current), [])

  const presentUserIds = useMemo(
    () => new Set(presentUsers.map((presence) => presence.userId)),
    [presentUsers],
  )

  const handleContentChange = useCallback(
    (content) => {
      codeRef.current = content
      snapshot.handleContentChange(content)
    },
    [snapshot],
  )

  const handleSave = useCallback(async () => {
    const saved = await snapshot.save()
    if (!saved && snapshot.status === 'error') setActionError(snapshot.error)
  }, [snapshot])

  const handleRun = useCallback(() => {
    execution.run(codeRef.current || DEFAULT_SOURCE)
  }, [execution])

  const handleCopyInvite = useCallback(() => {
    if (!room?.inviteCode) return
    navigator.clipboard?.writeText(room.inviteCode)
    setCopiedCode(true)
    clearTimeout(copyTimer.current)
    copyTimer.current = setTimeout(() => setCopiedCode(false), 1800)
  }, [room])

  const handleLeave = useCallback(async () => {
    setActionError(null)
    try {
      await api.leaveRoom(roomId)
      navigate('/rooms', { replace: true })
    } catch (error) {
      setActionError(error.message)
    }
  }, [roomId, navigate])

  const handleDelete = useCallback(async () => {
    setActionError(null)
    try {
      await api.deleteRoom(roomId)
      navigate('/rooms', { replace: true })
    } catch (error) {
      setActionError(error.message)
    }
  }, [roomId, navigate])

  if (loadError) {
    return (
      <main className="grid min-h-screen place-items-center bg-void-black px-4">
        <div className="max-w-md text-center">
          <HiveLogo size="lg" className="justify-center mb-4" />
          <p className="text-base font-medium text-pure-white">Room Unavailable</p>
          <p className="mt-1.5 text-xs text-muted-steel">{loadError}</p>
          <Button className="mt-6" onClick={() => navigate('/rooms')}>
            Back to Rooms
          </Button>
        </div>
      </main>
    )
  }

  if (!room || !user) {
    return (
      <main className="grid min-h-screen place-items-center bg-void-black">
        <Spinner className="h-8 w-8 text-periwinkle-glow" />
      </main>
    )
  }

  return (
    <div className="flex h-screen flex-col bg-void-black text-frost overflow-hidden">
      {/* ─── Top Header Bar matching Screen 6 ─────────────────────────── */}
      <header className="flex h-13 shrink-0 items-center justify-between border-b border-gunmetal bg-void-black px-4 select-none">
        {/* Left info: Logo, Room Title, Language, Status, Member count, Invite Code */}
        <div className="flex items-center gap-3 min-w-0">
          <Link to="/rooms" className="transition-opacity hover:opacity-85 shrink-0">
            <HiveLogo size="sm" />
          </Link>

          <span className="text-gunmetal font-mono">/</span>

          <h1 className="truncate text-xs font-semibold text-pure-white max-w-[140px] sm:max-w-[200px]" title={room.name}>
            {room.name}
          </h1>

          <Badge tone="zinc" className="hidden sm:inline-flex">{room.language}</Badge>

          {/* Connection Status Pill */}
          <div className="flex items-center gap-1.5 rounded-full border border-gunmetal bg-carbon-surface/80 px-2.5 py-0.5 text-[11px] text-frost">
            <span
              className={`h-1.5 w-1.5 rounded-full ${
                connected ? 'bg-emerald-400 animate-pulse' : 'bg-red-400'
              }`}
            />
            <span className="hidden md:inline">{connected ? 'Connected' : 'Connecting…'}</span>
          </div>

          {/* Members count pill */}
          <div
            className="flex items-center gap-1 rounded-full border border-gunmetal bg-carbon-surface/80 px-2.5 py-0.5 text-[11px] text-frost cursor-pointer hover:border-steel-border/50"
            onClick={() => {
              setActiveView('code')
              setSidebarTab('split')
            }}
            title="Active collaborators"
          >
            <svg className="h-3 w-3 text-periwinkle-glow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            <span>{presentUserIds.size || 1}</span>
          </div>

          {/* Invite code pill with copy action */}
          {room.inviteCode && (
            <div className="flex items-center gap-1.5 rounded-full border border-gunmetal bg-carbon-surface/80 pl-2.5 pr-1.5 py-0.5 text-[11px]">
              <span className="font-mono text-periwinkle-glow font-medium">{room.inviteCode}</span>
              <button
                type="button"
                onClick={handleCopyInvite}
                className="text-muted-steel hover:text-frost p-0.5 transition-colors"
                title="Copy Invite Code"
              >
                {copiedCode ? (
                  <span className="text-[10px] text-emerald-400">✓</span>
                ) : (
                  <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                  </svg>
                )}
              </button>
            </div>
          )}
        </div>

        {/* Right actions: Leave room button */}
        <div className="flex items-center gap-2 shrink-0">
          <Button
            variant="ghost"
            size="xs"
            className="border border-red-500/20 text-red-400 hover:bg-red-500/10 hover:border-red-500/40 text-xs px-3"
            onClick={() => setConfirmLeave(true)}
          >
            Leave Room
          </Button>
        </div>
      </header>

      {(actionError || socketError) && (
        <div className="shrink-0 px-4 py-1.5 bg-red-500/10 border-b border-red-500/20 text-xs text-red-400">
          {actionError ?? socketError}
        </div>
      )}

      {/* ─── Main Workspace: Left Rail + Center Work Area + Right Sidebar ── */}
      <div className="flex min-h-0 flex-1 overflow-hidden">
        {/* Left Slim Icon Rail matching Screen 6 */}
        <nav className="flex w-12 shrink-0 flex-col items-center gap-3 border-r border-gunmetal bg-[#0c0d15] py-3">
          <button
            type="button"
            onClick={() => setActiveView('code')}
            title="Code Editor"
            className={`flex h-8 w-8 items-center justify-center rounded-lg transition-colors ${
              activeView === 'code'
                ? 'bg-carbon-surface text-periwinkle-glow border border-gunmetal inset-rim'
                : 'text-muted-steel hover:text-frost hover:bg-carbon-surface/50'
            }`}
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
            </svg>
          </button>

          <button
            type="button"
            onClick={() => {
              setActiveView('code')
              setSidebarTab(sidebarTab === 'chat' ? 'split' : 'chat')
            }}
            title="Chat"
            className={`relative flex h-8 w-8 items-center justify-center rounded-lg transition-colors ${
              sidebarTab === 'chat'
                ? 'bg-carbon-surface text-periwinkle-glow border border-gunmetal inset-rim'
                : 'text-muted-steel hover:text-frost hover:bg-carbon-surface/50'
            }`}
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
          </button>

          <button
            type="button"
            onClick={() => {
              setActiveView('code')
              setSidebarTab(sidebarTab === 'members' ? 'split' : 'members')
            }}
            title="Online Members"
            className={`flex h-8 w-8 items-center justify-center rounded-lg transition-colors ${
              sidebarTab === 'members'
                ? 'bg-carbon-surface text-periwinkle-glow border border-gunmetal inset-rim'
                : 'text-muted-steel hover:text-frost hover:bg-carbon-surface/50'
            }`}
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          </button>

          <button
            type="button"
            onClick={() => setActiveView('settings')}
            title="Room Settings"
            className={`flex h-8 w-8 items-center justify-center rounded-lg transition-colors ${
              activeView === 'settings'
                ? 'bg-carbon-surface text-periwinkle-glow border border-gunmetal inset-rim'
                : 'text-muted-steel hover:text-frost hover:bg-carbon-surface/50'
            }`}
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </button>
        </nav>

        {/* View 1: Collaborative Code Workspace */}
        {activeView === 'code' && (
          <div className="flex min-h-0 flex-1 flex-col lg:flex-row overflow-hidden">
            {/* Center: Monaco Editor & Output Terminal */}
            <section className="flex min-h-0 flex-1 flex-col border-r border-gunmetal overflow-hidden">
              {/* Editor Tabs bar */}
              <div className="flex h-9 shrink-0 items-center justify-between border-b border-gunmetal bg-[#10121c] px-3 select-none">
                <div className="flex items-center gap-1.5">
                  <div className="flex items-center gap-2 rounded-t-md bg-void-black px-3 py-1.5 text-xs text-pure-white border-t border-x border-gunmetal">
                    <span>{fileNameFor(room?.language)}</span>
                    <span className="text-muted-steel hover:text-pure-white cursor-pointer text-[10px]">✕</span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span className="text-[11px] text-muted-steel">
                    {SAVE_LABELS[snapshot.status] ?? ''}
                  </span>
                  {!canEdit && <Badge tone="zinc">Read-Only</Badge>}
                </div>
              </div>

              {/* Monaco Editor Container */}
              <div className="min-h-0 flex-1 bg-void-black relative">
                {snapshot.status === 'loading' ? (
                  <div className="grid h-full place-items-center">
                    <Spinner className="h-6 w-6 text-periwinkle-glow" />
                  </div>
                ) : (
                  <CollaborativeEditor
                    client={client}
                    connected={connected}
                    roomId={roomId}
                    user={user}
                    canEdit={canEdit}
                    language={room?.language}
                    initialContent={snapshot.snapshot?.content || DEFAULT_SOURCE}
                    onContentChange={handleContentChange}
                  />
                )}
              </div>

              {/* Action Bar (Save & Run) matching Screen 6 */}
              <div className="flex shrink-0 items-center justify-between border-t border-gunmetal bg-[#0c0e18] px-4 py-2">
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleSave}
                    disabled={!canEdit || snapshot.status === 'saving'}
                    className="gap-1.5 text-xs"
                  >
                    <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" />
                    </svg>
                    <span>{snapshot.status === 'saving' ? 'Saving…' : 'Save'}</span>
                  </Button>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    variant="success"
                    size="sm"
                    onClick={handleRun}
                    disabled={!canEdit || execution.running}
                    className="gap-1.5 text-xs px-5"
                  >
                    <svg className="h-3.5 w-3.5" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clipRule="evenodd" />
                    </svg>
                    <span>Run</span>
                  </Button>
                </div>
              </div>

              {/* Output Terminal Console */}
              <div className="h-56 shrink-0 border-t border-gunmetal bg-void-black p-3 lg:h-64">
                <OutputPanel
                  result={execution.result}
                  running={execution.running}
                  error={execution.error}
                  canRun={canEdit}
                  onClear={execution.clear}
                />
              </div>
            </section>

            {/* Right Panel: Split (Online Members + Chat) matching Screen 6 */}
            <aside className="flex h-80 shrink-0 flex-col border-t border-gunmetal bg-[#0d0e17] lg:h-auto lg:w-80 lg:border-t-0 select-none">
              {/* Online Members Section (Top Half) */}
              {(sidebarTab === 'split' || sidebarTab === 'members') && (
                <div className={`flex flex-col border-b border-gunmetal ${sidebarTab === 'split' ? 'h-48' : 'flex-1'}`}>
                  <div className="flex shrink-0 items-center justify-between border-b border-gunmetal px-3.5 py-2 bg-carbon-surface/50">
                    <span className="text-xs font-semibold text-pure-white">Online Members</span>
                    <span className="text-[11px] text-periwinkle-glow font-mono">
                      {presentUserIds.size}/{members.length}
                    </span>
                  </div>

                  <div className="min-h-0 flex-1 overflow-y-auto p-2 space-y-1">
                    {members.map((member) => {
                      const isOnline = presentUserIds.has(member.userId)

                      return (
                        <div
                          key={member.userId}
                          className="flex items-center justify-between rounded-lg px-2.5 py-1.5 transition-colors hover:bg-carbon-surface"
                        >
                          <div className="flex items-center gap-2 min-w-0">
                            <div className="relative">
                              <span
                                className="flex h-6 w-6 items-center justify-center rounded-full text-[10px] font-bold text-white shadow-inner"
                                style={{ background: `hsl(${(member.userId * 53) % 360} 40% 48%)` }}
                              >
                                {member.username.slice(0, 1).toUpperCase()}
                              </span>
                              {isOnline && (
                                <span className="absolute -bottom-0.5 -right-0.5 h-2 w-2 rounded-full bg-emerald-400 ring-1 ring-void-black" />
                              )}
                            </div>
                            <span className="truncate text-xs text-pure-white">
                              {member.username}
                            </span>
                          </div>
                          <RoleBadge role={member.role} />
                        </div>
                      )
                    })}
                  </div>
                </div>
              )}

              {/* Chat Section (Bottom Half) */}
              {(sidebarTab === 'split' || sidebarTab === 'chat') && (
                <div className="flex min-h-0 flex-1 flex-col">
                  <div className="flex shrink-0 items-center justify-between border-b border-gunmetal px-3.5 py-2 bg-carbon-surface/50">
                    <span className="text-xs font-semibold text-pure-white">Chat</span>
                    <span className="text-[10px] text-muted-steel">Real-time</span>
                  </div>

                  <div className="min-h-0 flex-1">
                    <ChatPanel
                      messages={chat.messages}
                      status={chat.status}
                      error={chat.error}
                      hasMore={chat.hasMore}
                      onLoadOlder={chat.loadOlder}
                      onSend={chat.send}
                      currentUserId={user.id}
                      connected={connected}
                    />
                  </div>
                </div>
              )}
            </aside>
          </div>
        )}

        {/* View 2: Room Settings matching Screen 7 */}
        {activeView === 'settings' && (
          <RoomSettings
            room={room}
            members={members}
            presentUserIds={presentUserIds}
            currentUserId={user.id}
            onUpdateRoom={fetchRoomData}
            onLeave={handleLeave}
            onDelete={handleDelete}
            onBackToEditor={() => setActiveView('code')}
          />
        )}
      </div>

      {/* Leave Room Modal */}
      {confirmLeave && (
        <Modal
          title="Leave Room?"
          onClose={() => setConfirmLeave(false)}
          footer={
            <>
              <Button variant="secondary" size="sm" onClick={() => setConfirmLeave(false)}>
                Cancel
              </Button>
              <Button variant="danger" size="sm" onClick={handleLeave}>
                Leave Room
              </Button>
            </>
          }
        >
          <p className="text-xs text-frost">
            Are you sure you want to leave <strong className="text-pure-white">{room.name}</strong>?
            You will need an invite code to rejoin later.
          </p>
        </Modal>
      )}
    </div>
  )
}
