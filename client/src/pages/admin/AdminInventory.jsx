import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api.js'
import { Badge, Card, inputClass } from './Ui.jsx'

function Table({ rows, columns }) {
  if (!rows.length) return <p className="text-sm text-muted">Nothing here. All good.</p>
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="text-muted">
          <tr>{columns.map((c) => <th key={c.h} className="pb-2 pr-3">{c.h}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i} className="border-t border-gold/10">
              {columns.map((c) => <td key={c.h} className="py-1.5 pr-3">{c.v(r)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

const product = (r) => <Link className="text-rose hover:underline" to={`/admin/products/${r.productId}`}>{r.productName} ({r.variantName})</Link>

export default function AdminInventory() {
  const [days, setDays] = useState(60)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api(`/admin/inventory/alerts?days=${days}`).then(setData).catch((e) => setError(e.message))
  }, [days])

  if (error) return <p className="text-rose">{error}</p>
  if (!data) return <p className="text-muted">Loading...</p>

  const stockCols = [
    { h: 'Product', v: product },
    { h: 'SKU', v: (r) => r.sku },
    { h: 'Available', v: (r) => r.available },
    { h: 'Alert at', v: (r) => r.lowStockThreshold },
    { h: 'Status', v: (r) => <Badge value={r.stockStatus} /> },
  ]
  const batchCols = [
    { h: 'Product', v: product },
    { h: 'Batch', v: (r) => r.batchCode },
    { h: 'Qty', v: (r) => r.quantity },
    { h: 'Expiry', v: (r) => r.expiryDate },
    { h: 'Days', v: (r) => (r.daysToExpiry < 0 ? `${-r.daysToExpiry} ago` : r.daysToExpiry) },
  ]

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="font-display text-3xl font-semibold">Inventory alerts</h1>
        <label className="flex items-center gap-2 text-sm">
          Expiring within
          <select value={days} onChange={(e) => setDays(Number(e.target.value))} className={`${inputClass} w-28`}>
            {[30, 60, 90, 180].map((d) => <option key={d} value={d}>{d} days</option>)}
          </select>
        </label>
      </div>
      <Card title={`Out of stock (${data.outOfStock.length})`}><Table rows={data.outOfStock} columns={stockCols} /></Card>
      <Card title={`Low stock (${data.lowStock.length})`}><Table rows={data.lowStock} columns={stockCols} /></Card>
      <Card title={`Expired batches (${data.expired.length})`}><Table rows={data.expired} columns={batchCols} /></Card>
      <Card title={`Near expiry (${data.nearExpiry.length})`}><Table rows={data.nearExpiry} columns={batchCols} /></Card>
    </div>
  )
}
