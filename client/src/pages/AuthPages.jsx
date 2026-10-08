import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { rs } from '../format.js'
import { useAuth } from '../auth.jsx'

const inputClass = 'w-full rounded-lg border border-gold/50 bg-white px-3 py-2'
const messages = (err) => (Array.isArray(err.details) ? err.details.map((d) => d.message) : [err.message])

function AuthForm({ title, fields, submitLabel, onSubmit, footer }) {
  const [values, setValues] = useState({})
  const [errors, setErrors] = useState([])
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setErrors([])
    try {
      await onSubmit(values)
    } catch (err) {
      setErrors(messages(err))
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="max-w-sm space-y-3">
      <h1 className="font-display text-3xl font-semibold">{title}</h1>
      {fields.map((f) => (
        <input
          key={f.name}
          type={f.type ?? 'text'}
          required={!f.optional}
          placeholder={f.label}
          autoComplete={f.auto}
          onChange={(e) => setValues({ ...values, [f.name]: e.target.value })}
          className={inputClass}
        />
      ))}
      {errors.map((m) => <p key={m} className="text-sm text-rose">{m}</p>)}
      <button disabled={busy} className="rounded-full bg-rose px-6 py-3 text-white hover:bg-rose-dark disabled:opacity-40">
        {busy ? 'Please wait...' : submitLabel}
      </button>
      <p className="text-sm text-muted">{footer}</p>
    </form>
  )
}

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const from = useLocation().state?.from
  return (
    <AuthForm
      title="Log in"
      submitLabel="Log in"
      fields={[
        { name: 'email', label: 'Email', type: 'email', auto: 'email' },
        { name: 'password', label: 'Password', type: 'password', auto: 'current-password' },
      ]}
      onSubmit={async (v) => {
        const u = await login(v)
        navigate(u.role === 'ADMIN' ? '/admin' : from || '/account', { replace: true })
      }}
      footer={<>New here? <Link to="/register" state={{ from }} className="text-rose underline">Create an account</Link></>}
    />
  )
}

export function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const from = useLocation().state?.from
  return (
    <AuthForm
      title="Create your account"
      submitLabel="Create account"
      fields={[
        { name: 'name', label: 'Full name', auto: 'name' },
        { name: 'email', label: 'Email', type: 'email', auto: 'email' },
        { name: 'phone', label: 'Mobile (optional), e.g. 0771234567', optional: true, auto: 'tel' },
        { name: 'password', label: 'Password (8+ characters, with a letter and a number)', type: 'password', auto: 'new-password' },
      ]}
      onSubmit={async ({ phone, ...rest }) => {
        await register({ ...rest, ...(phone ? { phone } : {}) })
        navigate(from || '/account', { replace: true })
      }}
      footer={<>Already have an account? <Link to="/login" state={{ from }} className="text-rose underline">Log in</Link></>}
    />
  )
}

export function Account() {
  const { user, loading } = useAuth()
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (user) api('/orders/mine').then(setOrders).catch((e) => setError(e.message))
  }, [user])

  if (loading) return <p className="text-muted">Loading...</p>
  if (!user) return <Navigate to="/login" replace />

  return (
    <div className="max-w-2xl">
      <h1 className="font-display text-3xl font-semibold">Hello, {user.name}</h1>
      <p className="text-sm text-muted">{user.email}</p>
      <h2 className="mt-6 font-medium">Your orders</h2>
      {error && <p className="text-rose">{error}</p>}
      {orders?.length === 0 && <p className="mt-2 text-muted">No orders yet. Orders you place while logged in appear here.</p>}
      <ul className="mt-3 space-y-3">
        {orders?.map((o) => (
          <li key={o.orderCode} className="rounded-2xl bg-white p-4 shadow-sm">
            <p className="flex justify-between"><b>{o.orderCode}</b><span>{rs(o.totalCents)}</span></p>
            <p className="text-sm text-muted">{o.status.replace(/_/g, ' ')} · {new Date(o.createdAt).toLocaleDateString()}</p>
            <p className="mt-1 text-sm">{o.items.map((i) => `${i.productName} (${i.variantName}) × ${i.quantity}`).join(', ')}</p>
          </li>
        ))}
      </ul>
    </div>
  )
}
