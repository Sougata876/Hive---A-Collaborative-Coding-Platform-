import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { AuthLayout, Button, ErrorText, Input } from '../components/ui'
import { useAuth } from '../hooks/useAuth'

const USERNAME_PATTERN = /^[A-Za-z0-9_]+$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const FIELD_VALIDATORS = {
  username: (value) => {
    if (!value) return 'Username is required.'
    if (value.length < 3 || value.length > 50) return 'Username must be 3-50 characters.'
    if (!USERNAME_PATTERN.test(value)) return 'Letters, numbers and underscores only.'
    return ''
  },
  email: (value) => {
    if (!value) return 'Email is required.'
    if (value.length > 254 || !EMAIL_PATTERN.test(value)) return 'Enter a valid email address.'
    return ''
  },
  password: (value) => {
    if (!value) return 'Password is required.'
    if (value.length < 8) return 'At least 8 characters.'
    if (value.length > 72) return 'Password must be at most 72 characters.'
    return ''
  },
  confirmPassword: (value, form) => {
    if (!value) return 'Confirm your password.'
    if (value !== form.password) return 'Passwords do not match.'
    return ''
  },
}

function passwordStrength(password) {
  if (!password) return -1
  const classes = [/[a-z]/, /[A-Z]/, /[0-9]/, /[^A-Za-z0-9]/].filter((p) => p.test(password)).length
  if (password.length < 8) return 0
  if (password.length >= 12 && classes >= 3) return 2
  if (classes >= 2 && password.length >= 10) return 2
  return classes >= 2 || password.length >= 10 ? 1 : 0
}

const STRENGTH = [
  { label: 'Weak', bar: 'bg-red-500', text: 'text-red-400' },
  { label: 'Fair', bar: 'bg-amber-400', text: 'text-amber-400' },
  { label: 'Strong', bar: 'bg-emerald-500', text: 'text-emerald-400' },
]

export default function Register() {
  const { user, loading, register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({ username: '', email: '', password: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [touched, setTouched] = useState({})
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (!loading && user) return <Navigate to="/rooms" replace />

  const setField = (field) => (event) => {
    const value = event.target.value
    const nextForm = { ...form, [field]: value }
    setForm(nextForm)
    if (field === 'password' && touched.confirmPassword) {
      setErrors((prev) => ({
        ...prev,
        confirmPassword: FIELD_VALIDATORS.confirmPassword(nextForm.confirmPassword, nextForm),
      }))
    }
  }

  const validateField = (field, value = form[field]) => {
    const message = FIELD_VALIDATORS[field](value, form)
    setErrors((prev) => ({ ...prev, [field]: message }))
    return message
  }

  const handleBlur = (field) => () => {
    setTouched((prev) => ({ ...prev, [field]: true }))
    validateField(field)
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setFormError('')

    const nextErrors = {}
    for (const field of Object.keys(FIELD_VALIDATORS)) {
      const message = FIELD_VALIDATORS[field](form[field], form)
      if (message) nextErrors[field] = message
    }
    setErrors(nextErrors)
    setTouched({ username: true, email: true, password: true, confirmPassword: true })
    if (Object.keys(nextErrors).length > 0) return

    setSubmitting(true)
    try {
      await register({ username: form.username, email: form.email, password: form.password })
      navigate('/rooms', { replace: true })
    } catch (err) {
      if (err.fieldErrors) {
        setErrors((prev) => ({ ...prev, ...err.fieldErrors }))
      }
      setFormError(
        err.status === 409
          ? 'That username or email is already taken.'
          : err.message || 'Could not create your account. Please try again.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  const strength = passwordStrength(form.password)

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Join a community of builders."
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-periwinkle-glow hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <Input
          label="Username"
          name="username"
          type="text"
          autoComplete="username"
          placeholder="choose a username"
          value={form.username}
          onChange={setField('username')}
          onBlur={handleBlur('username')}
          error={errors.username}
          disabled={submitting}
          required
          autoFocus
        />

        <Input
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          value={form.email}
          onChange={setField('email')}
          onBlur={handleBlur('email')}
          error={errors.email}
          disabled={submitting}
          required
        />

        <div>
          <Input
            label="Password"
            name="password"
            type="password"
            autoComplete="new-password"
            placeholder="••••••••"
            value={form.password}
            onChange={setField('password')}
            onBlur={handleBlur('password')}
            error={errors.password}
            disabled={submitting}
            required
          />
          {form.password && (
            <div className="mt-2" aria-live="polite">
              <div className="flex gap-1.5">
                {[0, 1, 2].map((segment) => (
                  <div
                    key={segment}
                    className={`h-1 flex-1 rounded-full transition-colors ${
                      strength >= segment ? STRENGTH[strength]?.bar : 'bg-gunmetal'
                    }`}
                  />
                ))}
              </div>
              <p className={`mt-1 text-[11px] ${STRENGTH[strength]?.text ?? 'text-muted-steel'}`}>
                {strength >= 0 ? `Password strength: ${STRENGTH[strength].label}` : ''}
              </p>
            </div>
          )}
        </div>

        <Input
          label="Confirm Password"
          name="confirmPassword"
          type="password"
          autoComplete="new-password"
          placeholder="••••••••"
          value={form.confirmPassword}
          onChange={setField('confirmPassword')}
          onBlur={handleBlur('confirmPassword')}
          error={errors.confirmPassword}
          disabled={submitting}
          required
        />

        <ErrorText>{formError}</ErrorText>

        <Button type="submit" className="mt-2 w-full" disabled={submitting}>
          {submitting ? 'Creating Account…' : 'Create Account'}
        </Button>
      </form>
    </AuthLayout>
  )
}
