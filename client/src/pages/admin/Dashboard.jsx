import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api.js'
import { rs } from '../../format.js'
import { Badge, Card } from './Ui.jsx'

function Stat({ title, value, tone = 'text-ink', to }) {
  return (
    <Link to={to} className="rounded-2xl bg-white p-4 shadow-sm hover:shadow">
      <p className="text-sm text-muted">{title}</p>
      <p className={`mt-1 text-3xl font-semibold ${tone}`}>{value}</p>
    </Link>
  )
}

export default function Dashboard() {
  const [alerts, setAlerts] = useState(null)
  const [orders, setOrders] = useState(null)
  const [pending, setPending] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([
      api('/admin/inventory/alerts?days=60'),
      api('/admin/orders?limit=8'),
      api('/admin/orders?status=PENDING_WHATSAPP&limit=1'),
    ])
      .then(([a, o, p]) => { setAlerts(a); setOrders(o); setPending(p) })
      .catch((e) => setError(e.message))
  }, [])

  if (error) return <p className="text-rose">{error}</p>
  if (!alerts) return <p className="text-muted">Loading...</p>

  const s = alerts.summary
  return (
    <div className="space-y-6">
      <h1 className="font-display text-3xl font-semibold">Dashboard</h1>
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
        <Stat title="WhatsApp orders to confirm" value={pending.pagination.total} tone="text-rose" to="/admin/orders" />
        <Stat title="Out of stock" value={s.outOfStock} tone={s.outOfStock ? 'text-red-600' : ''} to="/admin/inventory" />
        <Stat title="Low stock" value={s.lowStock} tone={s.lowStock ? 'text-amber-600' : ''} to="/admin/inventory" />
        <Stat title="Near expiry (60d)" value={s.nearExpiry} tone={s.nearExpiry ? 'text-amber-600' : ''} to="/admin/inventory" />
        <Stat title="Expired batches" value={s.expired} tone={s.expired ? 'text-red-600' : ''} to="/admin/inventory" />
      </div>

      <Card title="Recent orders" action={<Link to="/admin/orders" className="text-sm text-rose">View all</Link>}>
        {orders.items.length === 0 && <p className="text-sm text-muted">No orders yet.</p>}
        <ul className="divide-y divide-gold/20">
          {orders.items.map((o) => (
            <li key={o.id}>
              <Link to={`/admin/orders/${o.id}`} className="flex flex-wrap items-center justify-between gap-2 py-2.5 text-sm hover:bg-cream">
                <span><b>{o.orderCode}</b> <span className="text-muted">· {o.customerName}</span></span>
                <span className="flex items-center gap-3"><Badge value={o.status} /><span>{rs(o.totalCents)}</span></span>
              </Link>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  )
}
