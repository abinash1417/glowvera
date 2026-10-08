import { createContext, useContext, useEffect, useState } from 'react'
import { api } from './api.js'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // The login cookie is httpOnly, so the page asks the server who is logged in.
  // A 401 here just means "nobody yet".
  useEffect(() => {
    api('/auth/me')
      .then((d) => setUser(d.user))
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  const login = async (body) => {
    const d = await api('/auth/login', { method: 'POST', body })
    setUser(d.user)
    return d.user
  }
  const register = async (body) => {
    const d = await api('/auth/register', { method: 'POST', body })
    setUser(d.user)
    return d.user
  }
  const logout = async () => {
    await api('/auth/logout', { method: 'POST' }).catch(() => {})
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, loading, login, register, logout }}>{children}</AuthContext.Provider>
}
