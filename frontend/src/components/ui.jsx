import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

/**
 * Brand Logo — "</>" icon paired with "Hive".
 */
export function HiveLogo({ size = 'md', className = '' }) {
  const iconSizes = {
    sm: 'text-xs',
    md: 'text-sm',
    lg: 'text-lg',
  }
  const textSizes = {
    sm: 'text-sm',
    md: 'text-base',
    lg: 'text-xl',
  }

  return (
    <div className={`flex items-center gap-2 font-semibold tracking-tight text-white select-none ${className}`}>
      <span className={`flex items-center justify-center font-mono font-bold text-periwinkle-glow ${iconSizes[size]}`}>
        &lt;/&gt;
      </span>
      <span className={`${textSizes[size]} font-semibold tracking-tight text-pure-white`}>
        Hive
      </span>
    </div>
  )
}

/** Legacy alias for backwards compatibility */
export const CodeTogetherLogo = HiveLogo

/** Legacy HiveMark export for backwards compatibility */
export function HiveMark({ className = 'h-6 w-6' }) {
  return (
    <span className={`flex items-center justify-center font-mono font-bold text-periwinkle-glow ${className}`}>
      &lt;/&gt;
    </span>
  )
}

export function BrandHeader({ subtitle }) {
  return (
    <div className="flex items-center gap-3">
      <HiveLogo size="lg" />
      {subtitle && <p className="text-xs text-muted-steel">{subtitle}</p>}
    </div>
  )
}

/**
 * Better Stack Button styles:
 * Pill shape (9999px radius), inset highlight on primary.
 */
export function Button({ variant = 'primary', size = 'md', className = '', ...props }) {
  const base =
    'inline-flex items-center justify-center gap-2 rounded-full font-medium transition-all duration-150 disabled:cursor-not-allowed disabled:opacity-45 select-none'

  const variants = {
    primary:
      'bg-gradient-to-r from-[#5b63d3] to-[#7c87f7] text-white shadow-[rgba(255,255,255,0.25)_0px_1px_3px_0px_inset] hover:brightness-110 active:scale-[0.98]',
    secondary:
      'bg-carbon-surface text-pure-white border border-gunmetal hover:border-steel-border/50 hover:bg-[#191b29] active:scale-[0.98]',
    ghost:
      'text-frost hover:bg-carbon-surface hover:text-pure-white active:scale-[0.98]',
    danger:
      'bg-red-500/15 text-red-400 border border-red-500/30 hover:bg-red-500/25 active:scale-[0.98]',
    outline:
      'border border-gunmetal text-frost hover:border-steel-border/50 hover:text-pure-white hover:bg-carbon-surface/50 active:scale-[0.98]',
    success:
      'bg-emerald-600 hover:bg-emerald-500 text-white shadow-[rgba(255,255,255,0.25)_0px_1px_3px_0px_inset] active:scale-[0.98]',
  }

  const sizes = {
    xs: 'px-2.5 py-1 text-xs',
    sm: 'px-3.5 py-1.5 text-xs',
    md: 'px-4.5 py-2 text-sm',
    lg: 'px-6 py-2.5 text-base',
  }

  return <button className={`${base} ${variants[variant]} ${sizes[size]} ${className}`} {...props} />
}

/**
 * Enhanced Input with optional password show/hide eye toggle.
 */
export function Input({
  label,
  error,
  hint,
  type = 'text',
  className = '',
  rightAction,
  ...props
}) {
  const [showPassword, setShowPassword] = useState(false)
  const isPassword = type === 'password'
  const computedType = isPassword ? (showPassword ? 'text' : 'password') : type

  return (
    <label className="block w-full">
      {label && <span className="mb-1.5 block text-xs font-medium text-frost">{label}</span>}
      <div className="relative flex items-center">
        <input
          type={computedType}
          className={`w-full rounded-[10px] border bg-void-black px-3.5 py-2 text-sm text-pure-white placeholder-muted-steel outline-none transition-colors focus:border-periwinkle-glow focus:ring-1 focus:ring-periwinkle-glow/30 ${
            error ? 'border-red-500' : 'border-gunmetal'
          } ${isPassword || rightAction ? 'pr-10' : ''} ${className}`}
          {...props}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 text-muted-steel hover:text-frost focus:outline-none"
            title={showPassword ? 'Hide password' : 'Show password'}
            tabIndex={-1}
          >
            {showPassword ? (
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18" />
              </svg>
            ) : (
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            )}
          </button>
        )}
        {rightAction && <div className="absolute right-2.5">{rightAction}</div>}
      </div>
      {error && <span className="mt-1 block text-xs text-red-400">{error}</span>}
      {hint && !error && <span className="mt-1 block text-xs text-muted-steel">{hint}</span>}
    </label>
  )
}

export function Select({ label, children, className = '', ...props }) {
  return (
    <label className="block w-full">
      {label && <span className="mb-1.5 block text-xs font-medium text-frost">{label}</span>}
      <div className="relative">
        <select
          className={`w-full appearance-none rounded-[10px] border border-gunmetal bg-void-black px-3.5 py-2 text-sm text-pure-white outline-none transition-colors focus:border-periwinkle-glow ${className}`}
          {...props}
        >
          {children}
        </select>
        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 text-muted-steel">
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </div>
    </label>
  )
}

export function Card({ className = '', ...props }) {
  return (
    <div
      className={`rounded-2xl border border-gunmetal bg-carbon-surface shadow-[rgba(255,255,255,0.25)_0_1px_3px_0_inset] ${className}`}
      {...props}
    />
  )
}

export function Badge({ children, tone = 'zinc', className = '' }) {
  const tones = {
    zinc: 'bg-void-black text-muted-steel border border-gunmetal',
    link: 'bg-periwinkle-glow/15 text-periwinkle-glow border border-periwinkle-glow/30',
    green: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30',
    blue: 'bg-iris-blue/15 text-periwinkle-glow border border-iris-blue/30',
    red: 'bg-red-500/15 text-red-400 border border-red-500/30',
    purple: 'bg-purple-500/15 text-purple-300 border border-purple-500/30',
  }
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-medium ${tones[tone]} ${className}`}
    >
      {children}
    </span>
  )
}

export function RoleBadge({ role }) {
  if (role === 'OWNER') {
    return <Badge tone="green">OWNER</Badge>
  }
  if (role === 'EDITOR') {
    return <Badge tone="blue">EDITOR</Badge>
  }
  return <Badge tone="purple">VIEWER</Badge>
}

export function Avatar({ user, size = 'md', className = '' }) {
  const sizes = {
    xs: 'h-6 w-6 text-[10px]',
    sm: 'h-7 w-7 text-[11px]',
    md: 'h-9 w-9 text-xs',
    lg: 'h-14 w-14 text-xl',
    xl: 'h-16 w-16 text-2xl',
  }
  const name = user?.username ?? user?.name ?? '?'
  const initial = name.slice(0, 1).toUpperCase()
  const color = `hsl(${(Number(user?.id) || 4) * 53} 40% 48%)`

  if (user?.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt={name}
        title={name}
        className={`${sizes[size]} shrink-0 rounded-full object-cover ring-1 ring-gunmetal ${className}`}
      />
    )
  }
  return (
    <span
      title={name}
      className={`${sizes[size]} grid shrink-0 place-items-center rounded-full font-semibold text-white shadow-inner ${className}`}
      style={{ background: color }}
    >
      {initial}
    </span>
  )
}

export function Spinner({ className = 'h-5 w-5' }) {
  return (
    <svg
      className={`animate-spin text-muted-steel ${className}`}
      viewBox="0 0 24 24"
      fill="none"
      aria-label="Loading"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
    </svg>
  )
}

export function ErrorText({ children }) {
  if (!children) return null
  return (
    <p className="mt-2 rounded-[10px] border border-red-500/20 bg-red-500/10 px-3 py-2 text-xs text-red-400">
      {children}
    </p>
  )
}

export function Modal({ title, onClose, children, footer }) {
  useEffect(() => {
    const onKey = (event) => event.key === 'Escape' && onClose?.()
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/80 backdrop-blur-xs p-4" onClick={onClose}>
      <div
        className="w-full max-w-lg rounded-2xl border border-gunmetal bg-carbon-surface shadow-[rgba(255,255,255,0.25)_0_1px_3px_0_inset]"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="flex items-center justify-between border-b border-gunmetal px-5 py-4">
          <h2 className="text-base font-medium text-pure-white">{title}</h2>
          <button
            onClick={onClose}
            className="flex h-7 w-7 items-center justify-center rounded-full text-muted-steel transition-colors hover:bg-gunmetal hover:text-pure-white"
            aria-label="Close"
          >
            ✕
          </button>
        </header>
        <div className="px-5 py-5">{children}</div>
        {footer && (
          <footer className="flex justify-end gap-2 border-t border-gunmetal px-5 py-4">{footer}</footer>
        )}
      </div>
    </div>
  )
}

export function EmptyState({ title, children, action }) {
  return (
    <div className="grid place-items-center px-6 py-16 text-center">
      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full border border-gunmetal bg-void-black text-muted-steel">
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      </div>
      <p className="text-base font-medium text-frost">{title}</p>
      {children && <p className="mt-1 max-w-sm text-xs text-muted-steel">{children}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export function AuthLayout({ title, subtitle, children, footer }) {
  return (
    <main className="grid min-h-screen place-items-center bg-void-black px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 flex justify-center">
          <Link to="/" className="inline-block transition-transform hover:scale-105">
            <HiveLogo size="lg" />
          </Link>
        </div>
        <Card className="p-8">
          <h1 className="text-xl font-medium text-pure-white">{title}</h1>
          {subtitle && <p className="mt-1.5 text-xs text-muted-steel">{subtitle}</p>}
          <div className="mt-6">{children}</div>
        </Card>
        {footer && <div className="mt-5 text-center text-xs text-muted-steel">{footer}</div>}
      </div>
    </main>
  )
}
