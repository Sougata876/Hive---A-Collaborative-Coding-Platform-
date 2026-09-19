import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../lib/api'
import { useAuth } from '../hooks/useAuth'
import { formatMonthYear } from '../lib/formatters'
import {
  Avatar,
  Button,
  Card,
  HiveLogo,
  ErrorText,
  Input,
  Modal,
  Spinner,
} from '../components/ui'

const USERNAME_RE = /^[A-Za-z0-9_]+$/
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const AVATAR_URL_RE = /^https?:\/\/\S+$/

function validate({ username, email, avatarUrl }) {
  const errors = {}

  if (!username) errors.username = 'Username is required.'
  else if (username.length < 3 || username.length > 50)
    errors.username = 'Username must be 3-50 characters.'
  else if (!USERNAME_RE.test(username))
    errors.username = 'Letters, numbers and underscores only.'

  if (!email) errors.email = 'Email is required.'
  else if (email.length > 254) errors.email = 'Email must be 254 characters or fewer.'
  else if (!EMAIL_RE.test(email)) errors.email = 'Enter a valid email address.'

  if (avatarUrl) {
    if (avatarUrl.length > 2048) errors.avatarUrl = 'Avatar URL must be 2048 characters or fewer.'
    else if (!AVATAR_URL_RE.test(avatarUrl)) errors.avatarUrl = 'Must be an http(s) URL.'
  }

  return errors
}

export default function Profile() {
  const { user, loading, updateProfile, logout } = useAuth()
  const navigate = useNavigate()

  const [isEditing, setIsEditing] = useState(false)
  const [form, setForm] = useState({
    username: user?.username ?? '',
    email: user?.email ?? '',
    avatarUrl: user?.avatarUrl ?? '',
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [saving, setSaving] = useState(false)

  function startEditing() {
    setForm({
      username: user?.username ?? '',
      email: user?.email ?? '',
      avatarUrl: user?.avatarUrl ?? '',
    })
    setFieldErrors({})
    setError('')
    setNotice('')
    setIsEditing(true)
  }

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
    if (fieldErrors[field]) {
      setFieldErrors((prev) => ({ ...prev, [field]: undefined }))
    }
  }

  async function handleSave(e) {
    e.preventDefault()
    if (saving || !user) return

    const trimmed = {
      username: form.username.trim(),
      email: form.email.trim(),
      avatarUrl: form.avatarUrl.trim(),
    }

    const invalid = validate(trimmed)
    if (Object.keys(invalid).length > 0) {
      setFieldErrors(invalid)
      return
    }

    const changes = {}
    if (trimmed.username !== (user.username ?? '')) changes.username = trimmed.username
    if (trimmed.email !== (user.email ?? '')) changes.email = trimmed.email
    if (trimmed.avatarUrl !== (user.avatarUrl ?? '')) changes.avatarUrl = trimmed.avatarUrl

    if (Object.keys(changes).length === 0) {
      setIsEditing(false)
      return
    }

    setSaving(true)
    setFieldErrors({})
    setError('')
    try {
      await updateProfile(changes)
      setNotice('Profile updated successfully.')
      setIsEditing(false)
      setTimeout(() => setNotice(''), 3000)
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldErrors ?? {})
        setError(
          err.status === 409 ? 'That username or email is already in use.' : err.message,
        )
      } else {
        setError('Could not update profile. Check your connection and try again.')
      }
    } finally {
      setSaving(false)
    }
  }

  function handleSignOut() {
    logout()
    navigate('/', { replace: true })
  }

  if (loading) {
    return (
      <main className="grid min-h-screen place-items-center bg-void-black">
        <Spinner className="h-8 w-8 text-periwinkle-glow" />
      </main>
    )
  }

  if (!user) {
    return (
      <main className="grid min-h-screen place-items-center bg-void-black px-4">
        <div className="text-center">
          <p className="text-sm text-frost">Your session has expired.</p>
          <Link to="/login" className="mt-2 inline-block text-xs text-periwinkle-glow hover:underline">
            Sign in again
          </Link>
        </div>
      </main>
    )
  }

  return (
    <div className="min-h-screen bg-void-black text-frost">
      {/* ─── Top Navigation Bar matching Screen 4 / Screen 8 ───────────── */}
      <header className="sticky top-0 z-40 border-b border-gunmetal bg-void-black/90 backdrop-blur-md">
        <div className="mx-auto flex h-14 w-full items-center justify-between gap-4 px-4 sm:px-6">
          <Link to="/rooms" className="transition-opacity hover:opacity-90">
            <HiveLogo size="md" />
          </Link>

          {/* Search bar */}
          <div className="relative max-w-md flex-1 hidden sm:block">
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-muted-steel">
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              placeholder="Search rooms..."
              onFocus={() => navigate('/rooms')}
              readOnly
              className="w-full rounded-full border border-gunmetal bg-carbon-surface/80 py-1.5 pl-9 pr-4 text-xs text-pure-white placeholder-muted-steel outline-none cursor-pointer"
            />
          </div>

          <div className="flex items-center gap-3">
            <Link
              to="/profile"
              className="flex items-center gap-2 rounded-full border border-steel-border/30 bg-carbon-surface p-1 pr-3"
            >
              <Avatar user={user} size="sm" />
              <span className="text-xs font-medium text-pure-white hidden md:inline-block">
                {user.username}
              </span>
            </Link>
          </div>
        </div>
      </header>

      {/* ─── Main Two-Column Layout ────────────────────────────────────── */}
      <div className="mx-auto flex max-w-7xl px-4 py-8 sm:px-6 gap-8">
        {/* Left Sidebar Navigation */}
        <aside className="w-56 shrink-0 hidden md:block">
          <nav className="space-y-1">
            <Link
              to="/rooms"
              className="flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-medium text-muted-steel hover:bg-carbon-surface hover:text-pure-white transition-colors"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
              <span>My Rooms</span>
            </Link>

            <Link
              to="/rooms"
              className="flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-medium text-muted-steel hover:bg-carbon-surface hover:text-pure-white transition-colors"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 4v16m8-8H4" />
              </svg>
              <span>Create Room</span>
            </Link>

            <Link
              to="/rooms"
              className="flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-medium text-muted-steel hover:bg-carbon-surface hover:text-pure-white transition-colors"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
              </svg>
              <span>Join Room</span>
            </Link>

            <Link
              to="/profile"
              className="flex items-center gap-3 rounded-xl bg-carbon-surface px-3.5 py-2.5 text-xs font-medium text-pure-white border border-gunmetal inset-rim"
            >
              <svg className="h-4 w-4 text-periwinkle-glow" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <span>Profile</span>
            </Link>
          </nav>
        </aside>

        {/* Right Main Content matching Screen 8 */}
        <main className="flex-1 min-w-0 max-w-3xl space-y-6">
          {notice && (
            <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-2.5 text-xs text-emerald-400">
              {notice}
            </div>
          )}

          {/* Header Profile Card matching Screen 8 */}
          <Card className="p-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <Avatar user={user} size="xl" />
                <div>
                  <h1 className="text-lg font-medium text-pure-white">
                    {user.username}
                  </h1>
                  <p className="text-xs text-muted-steel">{user.email}</p>
                </div>
              </div>

              <Button
                variant="ghost"
                size="sm"
                onClick={startEditing}
                className="border border-gunmetal hover:border-steel-border/50 text-xs"
              >
                Edit Profile
              </Button>
            </div>
          </Card>

          {/* Profile Details Card matching Screen 8 */}
          <Card className="p-6">
            <h2 className="text-sm font-semibold text-pure-white border-b border-gunmetal pb-3">
              User Details
            </h2>

            <div className="mt-4 divide-y divide-gunmetal/60">
              <div className="flex items-center justify-between py-3">
                <span className="text-xs text-muted-steel">Username</span>
                <span className="text-xs font-medium text-pure-white font-mono">{user.username}</span>
              </div>

              <div className="flex items-center justify-between py-3">
                <span className="text-xs text-muted-steel">Email</span>
                <span className="text-xs font-medium text-pure-white">{user.email}</span>
              </div>

              <div className="flex items-center justify-between py-3">
                <span className="text-xs text-muted-steel">Member since</span>
                <span className="text-xs font-medium text-pure-white">
                  {formatMonthYear(user.createdAt)}
                </span>
              </div>
            </div>
          </Card>

          {/* Danger Zone */}
          <Card className="p-6 border-red-500/20">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-medium text-pure-white">Sign out</h3>
                <p className="text-xs text-muted-steel mt-0.5">
                  Sign out of Hive on this browser session.
                </p>
              </div>
              <Button variant="danger" size="sm" onClick={handleSignOut}>
                Sign Out
              </Button>
            </div>
          </Card>
        </main>
      </div>

      {/* Edit Profile Modal */}
      {isEditing && (
        <Modal title="Edit Profile" onClose={() => setIsEditing(false)}>
          <form onSubmit={handleSave} noValidate className="space-y-4">
            <Input
              label="Username"
              value={form.username}
              onChange={(e) => updateField('username', e.target.value)}
              error={fieldErrors.username}
              maxLength={50}
              required
            />

            <Input
              label="Email"
              type="email"
              value={form.email}
              onChange={(e) => updateField('email', e.target.value)}
              error={fieldErrors.email}
              maxLength={254}
              required
            />

            <Input
              label="Avatar URL (Optional)"
              type="url"
              placeholder="https://example.com/avatar.png"
              value={form.avatarUrl}
              onChange={(e) => updateField('avatarUrl', e.target.value)}
              error={fieldErrors.avatarUrl}
              hint="Provide an image URL or leave empty to use your initials."
            />

            <ErrorText>{error}</ErrorText>

            <div className="flex justify-end gap-2 pt-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => setIsEditing(false)}
                disabled={saving}
              >
                Cancel
              </Button>
              <Button type="submit" size="sm" disabled={saving}>
                {saving ? 'Saving…' : 'Save Changes'}
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
