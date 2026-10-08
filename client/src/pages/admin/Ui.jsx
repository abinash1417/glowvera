import { Link } from 'react-router-dom'

export const inputClass = 'w-full rounded-lg border border-gold/50 bg-white px-3 py-2 text-sm'
export const btnPrimary = 'rounded-full bg-rose px-5 py-2 text-sm text-white hover:bg-rose-dark disabled:opacity-40'
export const btnGhost = 'rounded-full border border-gold/60 bg-white px-4 py-2 text-sm hover:bg-blush disabled:opacity-40'

export const errMessages = (err) =>
  Array.isArray(err.details) && err.details.length ? err.details.map((d) => d.message) : [err.message]

export const label = (s) => s.replace(/_/g, ' ')

const COLORS = {
  PENDING_PAYMENT: 'bg-amber-100 text-amber-800',
  PENDING_WHATSAPP: 'bg-amber-100 text-amber-800',
  PAID: 'bg-emerald-100 text-emerald-800',
  PROCESSING: 'bg-sky-100 text-sky-800',
  SHIPPED: 'bg-indigo-100 text-indigo-800',
  DELIVERED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-gray-200 text-gray-700',
  FAILED: 'bg-red-100 text-red-800',
  OK: 'bg-emerald-100 text-emerald-800',
  LOW_STOCK: 'bg-amber-100 text-amber-800',
  OUT_OF_STOCK: 'bg-red-100 text-red-800',
  NEAR_EXPIRY: 'bg-amber-100 text-amber-800',
  EXPIRED: 'bg-red-100 text-red-800',
  EMPTY: 'bg-gray-200 text-gray-700',
  ACTIVE: 'bg-emerald-100 text-emerald-800',
  INACTIVE: 'bg-gray-200 text-gray-700',
  SCHEDULED: 'bg-sky-100 text-sky-800',
  EXHAUSTED: 'bg-gray-200 text-gray-700',
}

export function Badge({ value }) {
  return (
    <span className={`inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium ${COLORS[value] ?? 'bg-gray-100 text-gray-700'}`}>
      {label(value)}
    </span>
  )
}

export function Errors({ list }) {
  if (!list?.length) return null
  return (
    <div className="space-y-1 rounded-lg bg-red-50 p-3">
      {list.map((m) => <p key={m} className="text-sm text-rose">{m}</p>)}
    </div>
  )
}

export function Pager({ pagination, onPage }) {
  if (!pagination || pagination.totalPages <= 1) return null
  const { page, totalPages, total } = pagination
  return (
    <div className="mt-4 flex items-center justify-between text-sm">
      <span className="text-muted">{total} total</span>
      <div className="flex items-center gap-2">
        <button className={btnGhost} disabled={page <= 1} onClick={() => onPage(page - 1)}>Prev</button>
        <span>{page} / {totalPages}</span>
        <button className={btnGhost} disabled={page >= totalPages} onClick={() => onPage(page + 1)}>Next</button>
      </div>
    </div>
  )
}

export function Card({ title, children, action }) {
  return (
    <section className="rounded-2xl bg-white p-4 shadow-sm sm:p-5">
      {(title || action) && (
        <div className="mb-3 flex items-center justify-between gap-2">
          {title && <h2 className="font-medium">{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </section>
  )
}

export function BackLink({ to, children }) {
  return <Link to={to} className="text-sm text-rose hover:underline">&larr; {children}</Link>
}
