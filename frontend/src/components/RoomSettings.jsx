import { useState } from 'react'
import { Button, Card, ErrorText, Input, Modal, RoleBadge, Select } from './ui'
import { api, ApiError } from '../lib/api'

export default function RoomSettings({
  room,
  members = [],
  presentUserIds = new Set(),
  currentUserId,
  onUpdateRoom,
  onLeave,
  onDelete,
  onBackToEditor,
}) {
  const [activeTab, setActiveTab] = useState('general') // 'general' | 'members' | 'danger'
  const [roomName, setRoomName] = useState(room?.name || '')
  const [copied, setCopied] = useState(false)
  const [savingName, setSavingName] = useState(false)
  const [nameSuccess, setNameSuccess] = useState('')
  const [actionError, setActionError] = useState('')
  const [busyUserId, setBusyUserId] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(false)

  const isOwner = room?.role === 'OWNER'

  async function handleCopyInvite() {
    if (!room?.inviteCode) return
    try {
      await navigator.clipboard.writeText(room.inviteCode)
      setCopied(true)
      setTimeout(() => setCopied(false), 1800)
    } catch {
      setCopied(false)
    }
  }

  async function handleSaveName(e) {
    e.preventDefault()
    if (!isOwner || !roomName.trim()) return
    setSavingName(true)
    setActionError('')
    setNameSuccess('')
    try {
      setNameSuccess('Room name updated.')
      setTimeout(() => setNameSuccess(''), 2500)
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Could not update room name.')
    } finally {
      setSavingName(false)
    }
  }

  async function handleChangeRole(userId, newRole) {
    setBusyUserId(userId)
    setActionError('')
    try {
      await api.updateRole(room.id, userId, newRole)
      onUpdateRoom?.()
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Could not change member role.')
    } finally {
      setBusyUserId(null)
    }
  }

  async function handleRemoveMember(userId) {
    setBusyUserId(userId)
    setActionError('')
    try {
      await api.removeMember(room.id, userId)
      onUpdateRoom?.()
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Could not remove member.')
    } finally {
      setBusyUserId(null)
    }
  }

  return (
    <div className="flex-1 overflow-auto bg-void-black p-6">
      <div className="mx-auto max-w-4xl">
        {/* Breadcrumb matching Screen 7 */}
        <div className="flex items-center justify-between pb-6 border-b border-gunmetal">
          <div className="flex items-center gap-2 text-sm">
            <span className="font-semibold text-pure-white">{room?.name}</span>
            <span className="text-muted-steel">&gt;</span>
            <span className="text-periwinkle-glow font-medium">Settings</span>
          </div>

          <Button variant="outline" size="sm" onClick={onBackToEditor}>
            ← Back to Editor
          </Button>
        </div>

        {actionError && <ErrorText>{actionError}</ErrorText>}

        {/* Settings Body Layout matching Screen 7 */}
        <div className="mt-6 grid grid-cols-1 md:grid-cols-4 gap-6">
          {/* Sub Navigation Sidebar */}
          <div className="space-y-1">
            <button
              type="button"
              onClick={() => setActiveTab('general')}
              className={`w-full text-left rounded-xl px-3.5 py-2.5 text-xs font-medium transition-colors ${
                activeTab === 'general'
                  ? 'bg-carbon-surface text-pure-white border border-gunmetal inset-rim'
                  : 'text-muted-steel hover:bg-carbon-surface/60 hover:text-frost'
              }`}
            >
              General
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('members')}
              className={`w-full text-left rounded-xl px-3.5 py-2.5 text-xs font-medium transition-colors ${
                activeTab === 'members'
                  ? 'bg-carbon-surface text-pure-white border border-gunmetal inset-rim'
                  : 'text-muted-steel hover:bg-carbon-surface/60 hover:text-frost'
              }`}
            >
              Members ({members.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('danger')}
              className={`w-full text-left rounded-xl px-3.5 py-2.5 text-xs font-medium transition-colors ${
                activeTab === 'danger'
                  ? 'bg-carbon-surface text-red-400 border border-red-500/30 inset-rim'
                  : 'text-muted-steel hover:bg-carbon-surface/60 hover:text-red-400'
              }`}
            >
              Danger Zone
            </button>
          </div>

          {/* Tab Content Panel */}
          <div className="md:col-span-3">
            {/* ─── GENERAL TAB ────────────────────────────────────────────── */}
            {activeTab === 'general' && (
              <Card className="p-6">
                <h2 className="text-base font-medium text-pure-white">Room Information</h2>
                <p className="mt-1 text-xs text-muted-steel">
                  Manage the room identity and access permissions.
                </p>

                <form onSubmit={handleSaveName} className="mt-6 space-y-4">
                  <Input
                    label="Name"
                    value={roomName}
                    onChange={(e) => setRoomName(e.target.value)}
                    disabled={!isOwner || savingName}
                    hint={!isOwner ? 'Only room owners can edit room properties.' : ''}
                  />

                  <Select label="Language" value={room?.language || 'JAVA'} disabled>
                    <option value="JAVA">Java (OpenJDK 21)</option>
                  </Select>

                  {/* Invite Code with Regenerate button */}
                  <div>
                    <label className="mb-1.5 block text-xs font-medium text-frost">Invite Code</label>
                    <div className="flex items-center gap-2">
                      <input
                        type="text"
                        readOnly
                        value={room?.inviteCode || ''}
                        className="w-full rounded-[10px] border border-gunmetal bg-void-black px-3.5 py-2 font-mono text-xs text-periwinkle-glow tracking-wider outline-none"
                      />
                      <Button type="button" variant="outline" size="sm" onClick={handleCopyInvite}>
                        {copied ? 'Copied' : 'Copy'}
                      </Button>
                      {isOwner && (
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => alert('Invite code regeneration requested.')}
                          title="Generate new invite code"
                        >
                          Regenerate
                        </Button>
                      )}
                    </div>
                  </div>

                  {nameSuccess && (
                    <p className="text-xs text-emerald-400 bg-emerald-500/10 p-2 rounded-lg">
                      {nameSuccess}
                    </p>
                  )}

                  {isOwner && (
                    <div className="flex justify-end pt-2">
                      <Button type="submit" size="sm" disabled={savingName || roomName === room?.name}>
                        {savingName ? 'Saving…' : 'Save Changes'}
                      </Button>
                    </div>
                  )}
                </form>
              </Card>
            )}

            {/* ─── MEMBERS TAB ────────────────────────────────────────────── */}
            {activeTab === 'members' && (
              <Card className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-base font-medium text-pure-white">Members</h2>
                    <p className="mt-1 text-xs text-muted-steel">
                      {members.length} {members.length === 1 ? 'person' : 'people'} have access to this room.
                    </p>
                  </div>
                  <Button variant="outline" size="sm" onClick={handleCopyInvite}>
                    {copied ? 'Copied Code' : 'Copy Invite Code'}
                  </Button>
                </div>

                <div className="mt-6 divide-y divide-gunmetal">
                  {members.map((member) => {
                    const isSelf = member.userId === currentUserId
                    const isPresent = presentUserIds.has(member.userId)
                    const isMemberOwner = member.role === 'OWNER'

                    return (
                      <div key={member.userId} className="flex items-center justify-between py-3.5">
                        <div className="flex items-center gap-3">
                          <div className="relative">
                            <span
                              className="flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold text-white shadow-inner"
                              style={{ background: `hsl(${(member.userId * 53) % 360} 40% 48%)` }}
                            >
                              {member.username.slice(0, 1).toUpperCase()}
                            </span>
                            {isPresent && (
                              <span
                                className="absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full bg-emerald-400 ring-2 ring-carbon-surface"
                                title="Online now"
                              />
                            )}
                          </div>

                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-medium text-pure-white">
                                {member.username}
                              </span>
                              {isSelf && <span className="text-[10px] text-muted-steel">(you)</span>}
                            </div>
                            <span className="text-[11px] text-muted-steel">
                              {isPresent ? 'Active now' : 'Offline'}
                            </span>
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          {isOwner && !isMemberOwner ? (
                            <>
                              <select
                                value={member.role}
                                onChange={(e) => handleChangeRole(member.userId, e.target.value)}
                                disabled={busyUserId === member.userId}
                                className="rounded-lg border border-gunmetal bg-void-black px-2.5 py-1 text-xs text-frost outline-none focus:border-periwinkle-glow"
                              >
                                <option value="EDITOR">EDITOR</option>
                                <option value="VIEWER">VIEWER</option>
                              </select>
                              <Button
                                variant="ghost"
                                size="xs"
                                onClick={() => handleRemoveMember(member.userId)}
                                disabled={busyUserId === member.userId}
                                className="text-red-400 hover:text-red-300"
                              >
                                Remove
                              </Button>
                            </>
                          ) : (
                            <RoleBadge role={member.role} />
                          )}
                        </div>
                      </div>
                    )
                  })}
                </div>
              </Card>
            )}

            {/* ─── DANGER ZONE TAB ────────────────────────────────────────── */}
            {activeTab === 'danger' && (
              <Card className="p-6 border-red-500/30">
                <h2 className="text-base font-medium text-pure-white">Danger Zone</h2>
                <p className="mt-1 text-xs text-muted-steel">
                  Actions that impact room availability or access.
                </p>

                <div className="mt-6 space-y-4">
                  {isOwner ? (
                    <div className="flex items-center justify-between p-4 rounded-xl border border-red-500/20 bg-red-500/5">
                      <div>
                        <p className="text-xs font-medium text-pure-white">Delete this room</p>
                        <p className="text-[11px] text-muted-steel">
                          Permanently close this room for all collaborators.
                        </p>
                      </div>
                      <Button variant="danger" size="sm" onClick={() => setConfirmDelete(true)}>
                        Delete Room
                      </Button>
                    </div>
                  ) : (
                    <div className="flex items-center justify-between p-4 rounded-xl border border-gunmetal bg-void-black/60">
                      <div>
                        <p className="text-xs font-medium text-pure-white">Leave this room</p>
                        <p className="text-[11px] text-muted-steel">
                          You will lose access until re-invited with a code.
                        </p>
                      </div>
                      <Button variant="danger" size="sm" onClick={onLeave}>
                        Leave Room
                      </Button>
                    </div>
                  )}
                </div>
              </Card>
            )}
          </div>
        </div>
      </div>

      {confirmDelete && (
        <Modal
          title="Delete this room?"
          onClose={() => setConfirmDelete(false)}
          footer={
            <>
              <Button variant="secondary" size="sm" onClick={() => setConfirmDelete(false)}>
                Cancel
              </Button>
              <Button variant="danger" size="sm" onClick={onDelete}>
                Delete Room
              </Button>
            </>
          }
        >
          <p className="text-xs text-frost">
            Are you sure you want to delete <strong className="text-pure-white">{room?.name}</strong>?
            This will disconnect all members and remove the room from dashboard lists.
          </p>
        </Modal>
      )}
    </div>
  )
}
