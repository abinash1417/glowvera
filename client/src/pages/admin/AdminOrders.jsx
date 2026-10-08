import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api.js'
import { rs } from '../../format.js'
import { Badge, BackLink, Card, Errors, Pager, btnGhost, btnPrimary, errMessages, inputClass, label } from './ui.jsx'

const STATUSES = ['PENDING_PAYMENT', 'PENDING_WHATSAPP', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'FAILED']
const when = (d) => new Date(d).toLocaleString()

export function OrderList() {
  const [status, setStatus] = useState('')
  const [q, setQ] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const params = new URLSearchParams({ page, limit: 15 })
    if (status) params.set('status', status)
    if (search) params.set('q', search)
    api(`/admin/orders?${params}`).then(setData).catch((e) => setError(e.message))
  }, [status, search, page])

  const submit = (e) => { e.preventDefault(); setPage(1); setSearch(q.trim()) }

  return (
    <div className="space-y-4">
      <h1 className="font-display text-3xl font-semibold">Orders</h1>
      <form onSubmit={submit} className="flex flex-col gap-2 sm:flex-row">
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search code, name, phone or email" className={inputClass} />
        <select value={status} onChange={(e) => { setPage(1); setStatus(e.target.value) }} className={`${inputClass} sm:w-56`}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        <button className={btnPrimary}>Search</button>
      </form>
      {error && <p className="text-rose">{error}</p>}
      {!data && !error && <p className="text-muted">Loading...</p>}
      {data && (
        <>
          {data.items.length === 0 && <p className="text-muted">No orders found.</p>}
          <div className="hidden overflow-x-auto rounded-2xl bg-white shadow-sm md:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-gold/30 text-muted">
                <tr><th className="p-3">Order</th><th>Customer</th><th>Date</th><th>Method</th><th>Items</th><th>Total</th><th>Status</th></tr>
              </thead>
              <tbody>
                {data.items.map((o) => (
                  <tr key={o.id} className="border-b border-gold/10 hover:bg-cream">
                    <td className="p-3"><Link className="font-medium text-rose" to={`/admin/orders/${o.id}`}>{o.orderCode}</Link></td>
                    <td>{o.customerName}<br /><span className="text-xs text-muted">{o.customerPhone} · {o.district}</span></td>
                    <td className="text-xs">{when(o.createdAt)}</td>
                    <td>{o.paymentMethod}</td>
                    <td>{o.itemCount}</td>
                    <td>{rs(o.totalCents)}</td>
                    <td><Badge value={o.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <ul className="space-y-3 md:hidden">
            {data.items.map((o) => (
              <li key={o.id}>
                <Link to={`/admin/orders/${o.id}`} className="block rounded-2xl bg-white p-4 shadow-sm">
                  <p className="flex justify-between"><b>{o.orderCode}</b><span>{rs(o.totalCents)}</span></p>
                  <p className="text-sm">{o.customerName} · {o.paymentMethod}</p>
                  <p className="mt-1 flex items-center justify-between text-xs text-muted"><span>{when(o.createdAt)}</span><Badge value={o.status} /></p>
                </Link>
              </li>
            ))}
          </ul>
          <Pager pagination={data.pagination} onPage={setPage} />
        </>
      )}
    </div>
  )
}

export function OrderDetail() {
  const { id } = useParams()
  const [order, setOrder] = useState(null)
  const [error, setError] = useState('')
  const [note, setNote] = useState('')
  const [errors, setErrors] = useState([])
  const [busy, setBusy] = useState(false)

  const load = useCallback(() => api(`/admin/orders/${id}`).then(setOrder).catch((e) => setError(e.message)), [id])
  useEffect(() => { load() }, [load])

  const change = async (status) => {
    const harmful = status === 'CANCELLED'
    if (harmful && !window.confirm('Cancel this order? Stock will be released or restocked.')) return
    setBusy(true)
    setErrors([])
    try {
      setOrder(await api(`/admin/orders/${id}/status`, { method: 'POST', body: { status, ...(note.trim() ? { note: note.trim() } : {}) } }))
      setNote('')
    } catch (e) {
      setErrors(errMessages(e))
    } finally {
      setBusy(false)
    }
  }

  if (error) return <p className="text-rose">{error}</p>
  if (!order) return <p className="text-muted">Loading...</p>
  const c = order.customer

  return (
    <div className="space-y-4">
      <BackLink to="/admin/orders">Orders</BackLink>
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="font-display text-3xl font-semibold">{order.orderCode}</h1>
        <Badge value={order.status} />
        <span className="text-sm text-muted">{order.paymentMethod} · {when(order.createdAt)}</span>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Card title="Items">
            <ul className="divide-y divide-gold/20 text-sm">
              {order.items.map((i) => (
                <li key={i.id} className="py-2">
                  <p className="flex justify-between gap-2"><span>{i.productName} ({i.variantName}) × {i.quantity}</span><span>{rs(i.lineTotalCents)}</span></p>
                  {i.batches.length > 0 && (
                    <p className="text-xs text-muted">Batches: {i.batches.map((b) => `${b.batchCode} ×${b.quantity} (exp ${b.expiryDate})`).join(', ')}</p>
                  )}
                </li>
              ))}
            </ul>
            <div className="mt-3 space-y-1 border-t border-gold/30 pt-3 text-sm">
              <p className="flex justify-between"><span>Subtotal</span><span>{rs(order.subtotalCents)}</span></p>
              {order.discountCents > 0 && <p className="flex justify-between text-emerald-700"><span>Discount ({order.couponCode})</span><span>-{rs(order.discountCents)}</span></p>}
              <p className="flex justify-between"><span>Shipping</span><span>{rs(order.shippingCents)}</span></p>
              <p className="flex justify-between font-semibold"><span>Total</span><span>{rs(order.totalCents)}</span></p>
            </div>
          </Card>

          <Card title="Status history">
            <ul className="space-y-2 text-sm">
              {order.history.map((h, n) => (
                <li key={n}>
                  <b>{h.fromStatus ? `${label(h.fromStatus)} → ` : ''}{label(h.toStatus)}</b>
                  <span className="text-muted"> · {h.changedBy} · {when(h.createdAt)}</span>
                  {h.note && <p className="text-xs text-muted">{h.note}</p>}
                </li>
              ))}
            </ul>
          </Card>

          {order.payments.length > 0 && (
            <Card title="Payments">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-muted"><tr><th>PayHere ID</th><th>Status</th><th>Amount</th><th>Signature</th><th>Date</th></tr></thead>
                  <tbody>
                    {order.payments.map((p) => (
                      <tr key={p.id} className="border-t border-gold/10">
                        <td className="py-1.5">{p.payherePaymentId ?? '—'}</td>
                        <td>{p.status}</td>
                        <td>{rs(p.amountCents)}</td>
                        <td>{p.signatureValid ? 'Valid' : 'Invalid'}</td>
                        <td className="text-xs">{when(p.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
        </div>

        <div className="space-y-4">
          <Card title="Update status">
            {order.allowedNext.length === 0 ? (
              <p className="text-sm text-muted">This order is final. No further changes are possible.</p>
            ) : (
              <div className="space-y-3">
                <input value={note} maxLength={255} onChange={(e) => setNote(e.target.value)} placeholder="Note (optional)" className={inputClass} />
                <div className="flex flex-wrap gap-2">
                  {order.allowedNext.map((s) => (
                    <button key={s} disabled={busy} onClick={() => change(s)} className={s === 'CANCELLED' ? btnGhost : btnPrimary}>
                      {label(s)}
                    </button>
                  ))}
                </div>
                <Errors list={errors} />
              </div>
            )}
          </Card>

          <Card title="Customer">
            <div className="space-y-1 text-sm">
              <p className="font-medium">{c.name}</p>
              <p><a className="text-rose" href={`mailto:${c.email}`}>{c.email}</a></p>
              <p><a className="text-rose" href={`tel:${c.phone}`}>{c.phone}</a></p>
              <p className="text-muted">{c.addressLine}, {c.city}, {c.district}</p>
              <p className="text-xs text-muted">{c.userId ? `Registered customer #${c.userId}` : 'Guest checkout'}</p>
              {order.notes && <p className="mt-2 rounded bg-cream p-2">Note: {order.notes}</p>}
              {order.reservationExpiresAt && ['PENDING_PAYMENT', 'PENDING_WHATSAPP'].includes(order.status) && (
                <p className="text-xs text-amber-700">Stock reserved until {when(order.reservationExpiresAt)}</p>
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
