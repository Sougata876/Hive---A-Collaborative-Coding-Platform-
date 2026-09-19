import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { ApiError } from '../lib/api'
import { AuthLayout, Button, ErrorText, Input } from '../components/ui'

export default function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  if (user) return <Navigate to="/rooms" replace />

  async function handleSubmit(e) {
    e.preventDefault()
    if (!identifier.trim() || !password) return
    setSubmitting(true)
    setError('')
    try {
      await login({ identifier: identifier.trim(), password })
      const dest = location.state?.from ?? '/rooms'
      navigate(dest, { replace: true })
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 401
          ? 'Incorrect username or password.'
          : err instanceof ApiError
          ? err.message
          : 'Something went wrong. Please check your credentials and connection.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Sign in to continue coding together."
      footer={
        <>
          Don&apos;t have an account?{' '}
          <Link to="/register" className="font-medium text-periwinkle-glow hover:underline">
            Create one
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Email or Username"
          autoComplete="username"
          value={identifier}
          onChange={(e) => setIdentifier(e.target.value)}
          placeholder="you@example.com"
          required
          autoFocus
        />
        <Input
          label="Password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          required
        />

        <div className="flex items-center justify-between pt-0.5 text-xs">
          <label className="flex items-center gap-2 text-muted-steel cursor-pointer hover:text-frost select-none">
            <input
              type="checkbox"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
              className="h-3.5 w-3.5 rounded border-gunmetal bg-void-black text-iris-blue focus:ring-periwinkle-glow/30"
            />
            <span>Remember me</span>
          </label>
          <a
            href="#forgot"
            onClick={(e) => {
              e.preventDefault()
              alert('Password reset link has been dispatched or contact your administrator.')
            }}
            className="text-muted-steel hover:text-periwinkle-glow transition-colors"
          >
            Forgot password?
          </a>
        </div>

        <ErrorText>{error}</ErrorText>

        <Button
          type="submit"
          className="mt-2 w-full"
          disabled={submitting || !identifier.trim() || !password}
        >
          {submitting ? 'Signing In…' : 'Sign In'}
        </Button>
      </form>
    </AuthLayout>
  )
}