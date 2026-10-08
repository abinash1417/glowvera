import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { rs } from '../format.js'
import { useCart } from '../cart.jsx'

export default function Product() {
  const { slug } = useParams()
  const { add } = useCart()
  const [p, setP] = useState(null)
  const [error, setError] = useState('')
  const [variantId, setVariantId] = useState(null)
  const [qty, setQty] = useState(1)
  const [added, setAdded] = useState(false)

  useEffect(() => {
    api(`/products/${slug}`)
      .then((d) => {
        setP(d)
        setVariantId((d.variants.find((v) => v.available > 0) ?? d.variants[0])?.id)
      })
      .catch((e) => setError(e.message))
  }, [slug])

  if (error) return <p className="text-rose">{error}</p>
  if (!p) return <p className="text-muted">Loading...</p>

  const v = p.variants.find((x) => x.id === variantId)
  const maxQty = Math.min(v?.available ?? 0, 50)

  const onAdd = () => {
    add({ variantId: v.id, productName: p.name, variantName: v.name, slug: p.slug }, qty)
    setAdded(true)
  }

  return (
    <div className="grid gap-8 md:grid-cols-2">
      <div className="flex aspect-square items-center justify-center overflow-hidden rounded-2xl bg-blush">
        {p.imageUrl ? (
          <img src={p.imageUrl} alt={p.name} className="h-full w-full object-cover" />
        ) : (
          <span className="font-display text-8xl text-rose/60">{p.name[0]}</span>
        )}
      </div>

      <div>
        <p className="text-sm text-muted">{p.category.name}</p>
        <h1 className="font-display text-3xl font-semibold">{p.name}</h1>
        <p className="mt-2 text-2xl font-semibold text-rose">{v ? rs(v.priceCents) : ''}</p>

        <div className="mt-4 flex flex-wrap gap-2">
          {p.variants.map((x) => (
            <button
              key={x.id}
              onClick={() => { setVariantId(x.id); setQty(1); setAdded(false) }}
              className={`rounded-full border px-4 py-2 text-sm ${x.id === variantId ? 'border-rose bg-rose text-white' : 'border-gold/50 bg-white'} ${x.available === 0 ? 'opacity-50' : ''}`}
            >
              {x.name}
            </button>
          ))}
        </div>

        <p className="mt-3 text-sm text-muted">
          {v?.available === 0 ? 'Out of stock' : v?.available <= 5 ? `Only ${v.available} left` : 'In stock'}
        </p>

        <div className="mt-4 flex items-center gap-3">
          <input
            type="number" min="1" max={maxQty || 1} value={qty}
            onChange={(e) => setQty(Math.max(1, Math.min(maxQty || 1, Number(e.target.value) || 1)))}
            className="w-20 rounded-lg border border-gold/50 bg-white px-3 py-2"
            aria-label="Quantity"
          />
          <button
            onClick={onAdd}
            disabled={!v || v.available === 0}
            className="rounded-full bg-rose px-6 py-3 text-white hover:bg-rose-dark disabled:opacity-40"
          >
            Add to cart
          </button>
        </div>
        {added && (
          <div className="mt-4 rounded-2xl bg-blush p-4">
            <p className="text-sm font-medium">Added to your cart.</p>
            <div className="mt-3 flex flex-wrap gap-3">
              <Link to="/cart" className="rounded-full bg-rose px-5 py-2 text-sm text-white hover:bg-rose-dark">View cart</Link>
              <Link to="/" className="rounded-full border border-rose px-5 py-2 text-sm text-rose hover:bg-white">Back to browsing products</Link>
            </div>
          </div>
        )}

        <p className="mt-6 leading-relaxed">{p.description}</p>
        {p.skinTypes.length > 0 && <p className="mt-4 text-sm"><b>Skin types:</b> {p.skinTypes.join(', ')}</p>}
        {p.concerns.length > 0 && <p className="mt-1 text-sm"><b>Helps with:</b> {p.concerns.join(', ')}</p>}
        {p.ingredients && <p className="mt-4 text-sm"><b>Ingredients:</b> {p.ingredients}</p>}
        {p.howToUse && <p className="mt-2 text-sm"><b>How to use:</b> {p.howToUse}</p>}
      </div>
    </div>
  )
}
