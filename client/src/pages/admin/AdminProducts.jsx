import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../../api.js'
import { rs } from '../../format.js'
import { Badge, BackLink, Card, Errors, Pager, btnGhost, btnPrimary, errMessages, inputClass } from './ui.jsx'

export function ProductList() {
  const [q, setQ] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('all')
  const [page, setPage] = useState(1)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const params = new URLSearchParams({ status, page, limit: 15 })
    if (search) params.set('q', search)
    api(`/admin/products?${params}`).then(setData).catch((e) => setError(e.message))
  }, [search, status, page])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-2">
        <h1 className="font-display text-3xl font-semibold">Products</h1>
        <Link to="/admin/products/new" className={btnPrimary}>+ New product</Link>
      </div>
      <form onSubmit={(e) => { e.preventDefault(); setPage(1); setSearch(q.trim()) }} className="flex flex-col gap-2 sm:flex-row">
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search name or brand" className={inputClass} />
        <select value={status} onChange={(e) => { setPage(1); setStatus(e.target.value) }} className={`${inputClass} sm:w-44`}>
          <option value="all">All</option>
          <option value="active">Active</option>
          <option value="inactive">Inactive</option>
        </select>
        <button className={btnPrimary}>Search</button>
      </form>
      {error && <p className="text-rose">{error}</p>}
      {!data && !error && <p className="text-muted">Loading...</p>}
      {data?.items.length === 0 && <p className="text-muted">No products found.</p>}
      <ul className="space-y-3">
        {data?.items.map((p) => (
          <li key={p.id}>
            <Link to={`/admin/products/${p.id}`} className="flex items-center gap-3 rounded-2xl bg-white p-3 shadow-sm hover:shadow">
              {p.imageUrl
                ? <img src={p.imageUrl} alt="" className="h-14 w-14 rounded-lg object-cover" />
                : <div className="h-14 w-14 rounded-lg bg-blush" />}
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">{p.name}</p>
                <p className="truncate text-xs text-muted">{p.brand ?? 'No brand'} · {p.category.name} · {p.variants.length} variant(s)</p>
              </div>
              <div className="flex flex-col items-end gap-1 text-xs">
                <Badge value={p.isActive ? 'ACTIVE' : 'INACTIVE'} />
                <span className="text-muted">{p.totalAvailable} in stock</span>
              </div>
            </Link>
          </li>
        ))}
      </ul>
      <Pager pagination={data?.pagination} onPage={setPage} />
    </div>
  )
}

const EMPTY = { name: '', brand: '', categoryId: '', description: '', ingredients: '', howToUse: '', imageUrl: '', skinTypeIds: [], concernIds: [], isActive: true }

function Checks({ title, options, value, onChange }) {
  const toggle = (id) => onChange(value.includes(id) ? value.filter((v) => v !== id) : [...value, id])
  return (
    <div>
      <p className="mb-1 text-sm font-medium">{title}</p>
      <div className="flex flex-wrap gap-2">
        {options.map((o) => (
          <label key={o.id} className={`cursor-pointer rounded-full border px-3 py-1 text-xs ${value.includes(o.id) ? 'border-rose bg-rose text-white' : 'border-gold/50 bg-white'}`}>
            <input type="checkbox" className="hidden" checked={value.includes(o.id)} onChange={() => toggle(o.id)} />
            {o.name}
          </label>
        ))}
      </div>
    </div>
  )
}

function ProductForm({ filters, product, onSaved }) {
  const [v, setV] = useState(() =>
    product
      ? {
          name: product.name, brand: product.brand ?? '', categoryId: product.category.id,
          description: product.description, ingredients: product.ingredients ?? '', howToUse: product.howToUse ?? '',
          imageUrl: product.imageUrl ?? '', skinTypeIds: product.skinTypes.map((s) => s.id),
          concernIds: product.concerns.map((c) => c.id), isActive: product.isActive,
        }
      : EMPTY,
  )
  const [errors, setErrors] = useState([])
  const [busy, setBusy] = useState(false)
  const [saved, setSaved] = useState(false)
  const set = (k) => (e) => { setSaved(false); setV({ ...v, [k]: e.target.value }) }

  const submit = async (e) => {
    e.preventDefault()
    if (!v.categoryId) return setErrors(['Choose a category'])
    setBusy(true)
    setErrors([])
    try {
      const body = { ...v, categoryId: Number(v.categoryId) }
      const res = product
        ? await api(`/admin/products/${product.id}`, { method: 'PATCH', body })
        : await api('/admin/products', { method: 'POST', body })
      setSaved(true)
      onSaved(res)
    } catch (err) {
      setErrors(errMessages(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <div className="grid gap-3 sm:grid-cols-2">
        <input required placeholder="Product name" value={v.name} onChange={set('name')} className={inputClass} />
        <input placeholder="Brand" value={v.brand} onChange={set('brand')} className={inputClass} />
        <select required value={v.categoryId} onChange={set('categoryId')} className={inputClass}>
          <option value="">Category...</option>
          {filters.categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input placeholder="Image: /images/serum.jpg or https://..." value={v.imageUrl} onChange={set('imageUrl')} className={inputClass} />
      </div>
      <textarea required rows={3} placeholder="Description (min 10 characters)" value={v.description} onChange={set('description')} className={inputClass} />
      <textarea rows={2} placeholder="Ingredients" value={v.ingredients} onChange={set('ingredients')} className={inputClass} />
      <textarea rows={2} placeholder="How to use" value={v.howToUse} onChange={set('howToUse')} className={inputClass} />
      <Checks title="Skin types" options={filters.skinTypes} value={v.skinTypeIds} onChange={(skinTypeIds) => setV({ ...v, skinTypeIds })} />
      <Checks title="Concerns" options={filters.concerns} value={v.concernIds} onChange={(concernIds) => setV({ ...v, concernIds })} />
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={v.isActive} onChange={(e) => setV({ ...v, isActive: e.target.checked })} />
        Visible in the store
      </label>
      <Errors list={errors} />
      <div className="flex items-center gap-3">
        <button disabled={busy} className={btnPrimary}>{busy ? 'Saving...' : product ? 'Save changes' : 'Create product'}</button>
        {saved && <span className="text-sm text-emerald-700">Saved</span>}
      </div>
    </form>
  )
}

function BatchForm({ variantId, onDone }) {
  const [f, setF] = useState({ batchCode: '', quantity: '', expiryDate: '' })
  const [errors, setErrors] = useState([])
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setErrors([])
    try {
      await api(`/admin/variants/${variantId}/batches`, {
        method: 'POST',
        body: { batchCode: f.batchCode, quantity: Number(f.quantity), expiryDate: f.expiryDate },
      })
      setF({ batchCode: '', quantity: '', expiryDate: '' })
      onDone()
    } catch (err) {
      setErrors(errMessages(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="mt-3 space-y-2 rounded-lg bg-cream p-3">
      <p className="text-sm font-medium">Receive new stock</p>
      <div className="grid gap-2 sm:grid-cols-4">
        <input required placeholder="Batch code" value={f.batchCode} onChange={(e) => setF({ ...f, batchCode: e.target.value })} className={inputClass} />
        <input required type="number" min="1" placeholder="Quantity" value={f.quantity} onChange={(e) => setF({ ...f, quantity: e.target.value })} className={inputClass} />
        <input required type="date" value={f.expiryDate} onChange={(e) => setF({ ...f, expiryDate: e.target.value })} className={inputClass} />
        <button disabled={busy} className={btnPrimary}>Add batch</button>
      </div>
      <Errors list={errors} />
    </form>
  )
}

function VariantCard({ variant, reload }) {
  const [price, setPrice] = useState(String(variant.priceCents / 100))
  const [threshold, setThreshold] = useState(String(variant.lowStockThreshold))
  const [errors, setErrors] = useState([])
  const [busy, setBusy] = useState(false)
  const [showBatch, setShowBatch] = useState(false)

  const patch = async (body) => {
    setBusy(true)
    setErrors([])
    try {
      await api(`/admin/variants/${variant.id}`, { method: 'PATCH', body })
      await reload()
    } catch (err) {
      setErrors(errMessages(err))
    } finally {
      setBusy(false)
    }
  }

  const writeOff = async (b) => {
    if (!window.confirm(`Write off ${b.quantity} unit(s) from batch ${b.batchCode}?`)) return
    setErrors([])
    try {
      await api(`/admin/batches/${b.id}/write-off`, { method: 'POST' })
      await reload()
    } catch (err) {
      setErrors(errMessages(err))
    }
  }

  return (
    <div className={`rounded-xl border border-gold/30 p-3 ${variant.isActive ? '' : 'opacity-60'}`}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-medium">{variant.name} <span className="text-xs font-normal text-muted">{variant.sku}</span></p>
        <div className="flex items-center gap-2 text-xs">
          <Badge value={variant.stockStatus} />
          <span>{variant.available} available ({variant.stockQty} on hand, {variant.reservedQty} reserved)</span>
        </div>
      </div>

      <div className="mt-3 grid gap-2 sm:grid-cols-4">
        <label className="text-xs text-muted">Price (Rs.)
          <input type="number" min="1" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} className={inputClass} />
        </label>
        <label className="text-xs text-muted">Low stock alert at
          <input type="number" min="0" value={threshold} onChange={(e) => setThreshold(e.target.value)} className={inputClass} />
        </label>
        <button disabled={busy} className={`${btnPrimary} self-end`}
          onClick={() => patch({ priceCents: Math.round(Number(price) * 100), lowStockThreshold: Number(threshold) })}>
          Save variant
        </button>
        <button disabled={busy} className={`${btnGhost} self-end`} onClick={() => patch({ isActive: !variant.isActive })}>
          {variant.isActive ? 'Deactivate' : 'Activate'}
        </button>
      </div>
      <p className="mt-1 text-xs text-muted">Current price {rs(variant.priceCents)}</p>

      {variant.batches.length > 0 && (
        <div className="mt-3 overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="text-muted"><tr><th>Batch</th><th>Qty</th><th>Expiry</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {variant.batches.map((b) => (
                <tr key={b.id} className="border-t border-gold/10">
                  <td className="py-1.5">{b.batchCode}</td>
                  <td>{b.quantity}</td>
                  <td>{b.expiryDate}</td>
                  <td><Badge value={b.expiryStatus} /></td>
                  <td className="text-right">
                    {b.quantity > 0 && <button onClick={() => writeOff(b)} className="text-rose hover:underline">Write off</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <button className="mt-3 text-sm text-rose hover:underline" onClick={() => setShowBatch(!showBatch)}>
        {showBatch ? 'Hide' : '+ Receive stock'}
      </button>
      {showBatch && <BatchForm variantId={variant.id} onDone={reload} />}
      <div className="mt-2"><Errors list={errors} /></div>
    </div>
  )
}

function NewVariant({ productId, onDone }) {
  const [f, setF] = useState({ sku: '', name: '', price: '' })
  const [errors, setErrors] = useState([])
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setErrors([])
    try {
      await api(`/admin/products/${productId}/variants`, {
        method: 'POST',
        body: { sku: f.sku, name: f.name, priceCents: Math.round(Number(f.price) * 100) },
      })
      setF({ sku: '', name: '', price: '' })
      onDone()
    } catch (err) {
      setErrors(errMessages(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-2">
      <div className="grid gap-2 sm:grid-cols-4">
        <input required placeholder="SKU, e.g. GLW-CRM-50" value={f.sku} onChange={(e) => setF({ ...f, sku: e.target.value })} className={inputClass} />
        <input required placeholder="Name, e.g. 50ml" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} className={inputClass} />
        <input required type="number" min="1" step="0.01" placeholder="Price (Rs.)" value={f.price} onChange={(e) => setF({ ...f, price: e.target.value })} className={inputClass} />
        <button disabled={busy} className={btnPrimary}>Add variant</button>
      </div>
      <Errors list={errors} />
    </form>
  )
}

export function ProductEditor() {
  const { id } = useParams()
  const isNew = id === 'new'
  const navigate = useNavigate()
  const [filters, setFilters] = useState(null)
  const [product, setProduct] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(() => api(`/admin/products/${id}`).then(setProduct).catch((e) => setError(e.message)), [id])

  useEffect(() => {
    api('/catalog/filters').then(setFilters).catch((e) => setError(e.message))
    if (!isNew) load()
  }, [isNew, load])

  if (error) return <p className="text-rose">{error}</p>
  if (!filters || (!isNew && !product)) return <p className="text-muted">Loading...</p>

  return (
    <div className="space-y-4">
      <BackLink to="/admin/products">Products</BackLink>
      <h1 className="font-display text-3xl font-semibold">{isNew ? 'New product' : product.name}</h1>

      <Card title="Details">
        <ProductForm
          key={product?.updatedAt ?? 'new'}
          filters={filters}
          product={isNew ? null : product}
          onSaved={(p) => (isNew ? navigate(`/admin/products/${p.id}`, { replace: true }) : setProduct(p))}
        />
      </Card>

      {isNew ? (
        <p className="text-sm text-muted">Save the product first, then add variants and stock.</p>
      ) : (
        <Card title="Variants & stock">
          <div className="space-y-3">
            {product.variants.length === 0 && <p className="text-sm text-muted">No variants yet. Add one below.</p>}
            {product.variants.map((variant) => <VariantCard key={variant.id} variant={variant} reload={load} />)}
            <div className="border-t border-gold/30 pt-3">
              <p className="mb-2 text-sm font-medium">Add a variant</p>
              <NewVariant productId={product.id} onDone={load} />
            </div>
          </div>
        </Card>
      )}
    </div>
  )
}
