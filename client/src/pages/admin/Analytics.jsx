import { useEffect, useState } from 'react'
import { api } from '../../api.js'
import { rs } from '../../format.js'
import { Badge, Card, btnGhost, btnPrimary } from './Ui.jsx'

const RANGES = [7, 30, 90]

// "+12%" / "-5%" compared with the previous period of the same length
function Change({ now, before }) {
  if (!before) return <span className="text-xs text-muted">{now ? 'New' : '-'}</span>
  const pct = Math.round(((now - before) / before) * 100)
  return <span className={`text-xs ${pct >= 0 ? 'text-emerald-700' : 'text-red-600'}`}>{pct >= 0 ? '+' : ''}{pct}% vs previous</span>
}

function Stat({ title, value, children }) {
  return (
    <div className="rounded-2xl bg-white p-4 shadow-sm">
      <p className="text-sm text-muted">{title}</p>
      <p className="mt-1 text-2xl font-semibold">{value}</p>
      {children}
    </div>
  )
}

function DailyChart({ daily }) {
  const max = Math.max(...daily.map((d) => d.revenueCents), 1)
  return (
    <div>
      <div className="flex h-44 items-end gap-px">
        {daily.map((d) => (
          <div key={d.date} className="flex h-full flex-1 items-end" title={`${d.date}: ${rs(d.revenueCents)} (${d.orders} orders)`}>
            <div
              className="w-full rounded-t bg-rose/80 hover:bg-rose"
              style={{ height: `${(d.revenueCents / max) * 100}%`, minHeight: d.revenueCents ? 2 : 0 }}
            />
          </div>
        ))}
      </div>
      <div className="mt-1 flex justify-between text-xs text-muted">
        <span>{daily[0]?.date}</span>
        <span>{daily[daily.length - 1]?.date}</span>
      </div>
    </div>
  )
}

export default function Analytics() {
  const [days, setDays] = useState(30)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    setError('')
    api(`/admin/analytics?days=${days}`).then(setData).catch((e) => setError(e.message))
  }, [days])

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-3xl font-semibold">Sales analytics</h1>
        <div className="flex gap-2">
          {RANGES.map((r) => (
            <button key={r} onClick={() => setDays(r)} className={days === r ? btnPrimary : btnGhost}>Last {r} days</button>
          ))}
        </div>
      </div>

      {error && <p className="text-rose">{error}</p>}
      {!data && !error && <p className="text-muted">Loading...</p>}

      {data && (
        <>
          <p className="text-sm text-muted">
            {data.from} to {data.to}. Revenue counts paid, processing, shipped and delivered orders (delivery included).
          </p>

          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <Stat title="Revenue" value={rs(data.revenueCents)}><Change now={data.revenueCents} before={data.prevRevenueCents} /></Stat>
            <Stat title="Orders" value={data.orders}><Change now={data.orders} before={data.prevOrders} /></Stat>
            <Stat title="Average order" value={rs(data.averageOrderCents)} />
            <Stat title="Discounts given" value={rs(data.discountCents)} />
          </div>

          <Card title="Revenue per day">
            {data.orders === 0 ? <p className="text-sm text-muted">No sales in this period yet.</p> : <DailyChart daily={data.daily} />}
          </Card>

          <div className="grid gap-6 md:grid-cols-2">
            <Card title="Top products">
              {data.topProducts.length === 0 && <p className="text-sm text-muted">No sales yet.</p>}
              <ul className="divide-y divide-gold/20 text-sm">
                {data.topProducts.map((p) => (
                  <li key={p.name} className="flex justify-between gap-2 py-2">
                    <span>{p.name} <span className="text-muted">· {p.units} sold</span></span>
                    <span>{rs(p.revenueCents)}</span>
                  </li>
                ))}
              </ul>
            </Card>

            <Card title="Orders by status (all orders placed)">
              {data.statuses.length === 0 && <p className="text-sm text-muted">No orders yet.</p>}
              <ul className="divide-y divide-gold/20 text-sm">
                {data.statuses.map((s) => (
                  <li key={s.status} className="flex items-center justify-between py-2">
                    <Badge value={s.status} /><span>{s.orders}</span>
                  </li>
                ))}
              </ul>
            </Card>

            <Card title="Payment method">
              {data.methods.length === 0 && <p className="text-sm text-muted">No sales yet.</p>}
              <ul className="divide-y divide-gold/20 text-sm">
                {data.methods.map((m) => (
                  <li key={m.method} className="flex justify-between py-2">
                    <span>{m.method === 'PAYHERE' ? 'PayHere (online)' : 'WhatsApp'} <span className="text-muted">· {m.orders} orders</span></span>
                    <span>{rs(m.revenueCents)}</span>
                  </li>
                ))}
              </ul>
            </Card>

            <Card title="Coupons used">
              {data.coupons.length === 0 && <p className="text-sm text-muted">No coupons used in this period.</p>}
              <ul className="divide-y divide-gold/20 text-sm">
                {data.coupons.map((c) => (
                  <li key={c.code} className="flex justify-between py-2">
                    <span><b>{c.code}</b> <span className="text-muted">· {c.orders} orders</span></span>
                    <span>-{rs(c.discountCents)}</span>
                  </li>
                ))}
              </ul>
            </Card>
          </div>
        </>
      )}
    </div>
  )
}
