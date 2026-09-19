import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, apiFetch, clearTokens, getAccessToken, storeTokens } from '../lib/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(() => Boolean(getAccessToken()))

  useEffect(() => {
    if (!getAccessToken()) return

    let cancelled = false
    api
      .me()
      .then((profile) => {
        if (!cancelled) setUser(profile)
      })
      .catch(() => {
        if (!cancelled) clearTokens()
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async ({ identifier, password }) => {
    const response = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: { identifier, password },
      auth: false,
    })
    storeTokens(response)
    setUser(response.user)
    return response.user
  }, [])

  const register = useCallback(async ({ username, email, password }) => {
    const response = await apiFetch('/api/auth/register', {
      method: 'POST',
      body: { username, email, password },
      auth: false,
    })
    storeTokens(response)
    setUser(response.user)
    return response.user
  }, [])

  const updateProfile = useCallback(async (payload) => {
    const profile = await api.updateProfile(payload)
    setUser(profile)
    return profile
  }, [])

  const logout = useCallback(() => {
    clearTokens()
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, register, logout, updateProfile, setUser }),
    [user, loading, login, register, logout, updateProfile],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within an AuthProvider')
  return context
}
