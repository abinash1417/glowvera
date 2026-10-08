import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'
import { rs } from '../format.js'

function Pick({ label, value, onChange, options }) {
  return (
    <select
      aria-label={label}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="rounded-full border border-gold/50 bg-white px-3 py-2 text-sm"
    >
      <option value="">{label}</option>
      {options.map((o) => (
        <option key={o.value} value={o.value}>{o.label}</option>
      ))}
    </select>
  )
}

export default function Shop() {
  const [params, setParams] = useSearchParams()
  const [filters, setFilters] = useState({ categories: [], skinTypes: [], concerns: [] })
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const query = params.toString()

  useEffect(() => {
    api('/catalog/filters').then(setFilters).catch(() => {})
  }, [])

  useEffect(() => {
    setData(null)
    setError('')
    api('/products' + (query ? `?${query}` : '')).then(setData).catch((e) => setError(e.message))
  }, [query])

  const get = (key) => params.get(key) ?? ''
  const set = (key, value) => {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key !== 'page') next.delete('page')
    setParams(next)
  }
  const byName = (list) => list.map((x) => ({ value: x.name, label: x.name }))

  return (
    <div>
      {!query && (
        <section className="mb-8">
          <div className="flex flex-col items-center justify-between gap-4 rounded-2xl bg-blush px-6 py-6 text-center sm:flex-row sm:text-left">
            <div>
              <p className="text-xs font-medium uppercase tracking-widest text-gold">Pure beauty, naturally yours</p>
              <h1 className="mt-1 font-display text-2xl font-semibold sm:text-3xl">Skincare, hair and body care</h1>
              <p className="mt-1 text-sm text-muted">Gentle, effective products picked for your skin type and concerns.</p>
            </div>
            <a href="#products" className="shrink-0 rounded-full bg-rose px-5 py-2 text-sm font-medium text-white hover:bg-rose-dark">
              Shop now
            </a>
          </div>
          <div className="mt-3 grid grid-cols-1 gap-3 text-center text-sm sm:grid-cols-3">
            {[
              ['Gentle formulas', 'Made for every skin type'],
              ['Secure checkout', 'Pay online or order on WhatsApp'],
              ['Track your order', 'See every step until delivery'],
            ].map(([title, text]) => (
              <div key={title} className="rounded-xl bg-white px-4 py-3 shadow-sm">
                <p className="font-medium">{title}</p>
                <p className="text-xs text-muted">{text}</p>
              </div>
            ))}
          </div>
        </section>
      )}

      <h2 id="products" className="scroll-mt-4 font-display text-2xl font-semibold">
        {query ? 'Shop skincare, hair and body care' : 'Our products'}
      </h2>

      <div className="mt-5 flex flex-wrap gap-2">
        <form
          key={get('q')}
          onSubmit={(e) => {
            e.preventDefault()
            set('q', new FormData(e.currentTarget).get('q').trim())
          }}
        >
          <input
            name="q"
            defaultValue={get('q')}
            placeholder="Search products"
            className="rounded-full border border-gold/50 bg-white px-4 py-2 text-sm"
          />
        </form>
        <Pick label="All categories" value={get('category')} onChange={(v) => set('category', v)}
          options={filters.categories.map((c) => ({ value: c.slug, label: c.name }))} />
        <Pick label="Any skin type" value={get('skinType')} onChange={(v) => set('skinType', v)} options={byName(filters.skinTypes)} />
        <Pick label="Any concern" value={get('concern')} onChange={(v) => set('concern', v)} options={byName(filters.concerns)} />
        <Pick label="Newest" value={get('sort')} onChange={(v) => set('sort', v)}
          options={[
            { value: 'price_asc', label: 'Price: low to high' },
            { value: 'price_desc', label: 'Price: high to low' },
            { value: 'name', label: 'Name' },
          ]} />
        {query && (
          <button onClick={() => setParams({})} className="px-2 text-sm text-rose underline">Clear filters</button>
        )}
      </div>

      {error && <p className="mt-6 text-rose">{error}</p>}
      {!data && !error && <p className="mt-6 text-muted">Loading products...</p>}
      {data && data.items.length === 0 && (
        <p className="mt-6 text-muted">No products match these filters. Clear a filter to see more.</p>
      )}

      {data && (
        <>
          <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
            {data.items.map((p) => (
              <Link key={p.id} to={`/product/${p.slug}`} className="rounded-2xl bg-white p-3 shadow-sm">
                <div className="flex aspect-square items-center justify-center overflow-hidden rounded-xl bg-blush">
                  {p.imageUrl ? (
                    <img src={p.imageUrl} alt={p.name} className="h-full w-full object-cover" />
                  ) : (
                    <span className="font-display text-5xl text-rose/60">{p.name[0]}</span>
                  )}
                </div>
                <h3 className="mt-3 font-medium">{p.name}</h3>
                <p className="text-sm text-muted">{p.category.name}</p>
                <p className="mt-1 font-semibold text-rose">{p.variantCount > 1 ? 'From ' : ''}{rs(p.priceFromCents)}</p>
                {!p.inStock && <p className="text-xs text-muted">Out of stock</p>}
              </Link>
            ))}
          </div>
          {data.pagination.totalPages > 1 && (
            <div className="mt-8 flex items-center justify-center gap-4 text-sm">
              <button disabled={data.pagination.page <= 1} onClick={() => set('page', String(data.pagination.page - 1))} className="disabled:opacity-40">Previous</button>
              <span>Page {data.pagination.page} of {data.pagination.totalPages}</span>
              <button disabled={data.pagination.page >= data.pagination.totalPages} onClick={() => set('page', String(data.pagination.page + 1))} className="disabled:opacity-40">Next</button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
