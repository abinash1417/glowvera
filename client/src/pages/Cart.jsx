import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { rs } from '../format.js'
import { useCart } from '../cart.jsx'
import CouponBox from '../components/CouponBox.jsx'

export default function Cart() {
  const { items, setQty, clear, coupon } = useCart()
  const [quote, setQuote] = useState(null)
  const [error, setError] = useState('')
  const key = JSON.stringify(items.map((i) => [i.variantId, i.quantity]))

  useEffect(() => {
    if (!items.length) return
    setError('')
    api('/cart/quote', {
      method: 'POST',
      body: {
        items: items.map(({ variantId, quantity }) => ({ variantId, quantity })),
        couponCode: coupon || undefined,
      },
    })
      .then(setQuote)
      .catch((e) => setError(e.message))
  }, [key, coupon])

  if (!items.length) {
    return <p>Your cart is empty. <Link to="/" className="text-rose underline">Browse the shop</Link></p>
  }

  const lineFor = (id) => quote?.lines.find((l) => l.variantId === id)

  return (
    <div className="max-w-2xl">
      <div className="flex items-center justify-between gap-3">
        <h1 className="font-display text-3xl font-semibold">Your cart</h1>
        <button onClick={() => window.confirm('Remove everything from your cart?') && clear()} className="text-sm text-rose underline">Clear cart</button>
      </div>
      {error && <p className="mt-4 text-rose">{error}</p>}

      <ul className="mt-6 divide-y divide-gold/30">
        {items.map((i) => {
          const line = lineFor(i.variantId)
          return (
            <li key={i.variantId} className="flex flex-wrap items-center justify-between gap-4 py-4">
              <div>
                <p className="font-medium">{i.productName}</p>
                <p className="text-sm text-muted">{i.variantName}</p>
                {line?.status === 'INSUFFICIENT_STOCK' && <p className="text-sm text-rose">Only {line.available} available. Lower the quantity.</p>}
                {line?.status === 'UNAVAILABLE' && <p className="text-sm text-rose">No longer available. Remove it to continue.</p>}
              </div>
              <div className="flex items-center gap-3">
                <button onClick={() => setQty(i.variantId, i.quantity - 1)} aria-label="Decrease" className="h-8 w-8 rounded-full border border-gold/50">-</button>
                <span>{i.quantity}</span>
                <button onClick={() => setQty(i.variantId, i.quantity + 1)} aria-label="Increase" className="h-8 w-8 rounded-full border border-gold/50">+</button>
                <span className="w-28 text-right">{line?.lineTotalCents != null ? rs(line.lineTotalCents) : ''}</span>
                <button onClick={() => setQty(i.variantId, 0)} aria-label={`Remove ${i.productName}`} className="text-sm text-rose underline">Remove</button>
              </div>
            </li>
          )
        })}
      </ul>

      {quote && (
        <div className="mt-6 text-right">
          <CouponBox quote={quote} />
          <p>Subtotal: <b>{rs(quote.subtotalCents)}</b></p>
          {quote.discountCents > 0 && <p className="text-emerald-700">Discount ({quote.couponCode}): <b>-{rs(quote.discountCents)}</b></p>}
          <p className="text-sm text-muted">Delivery is added at checkout.</p>
          <div className="mt-4 flex flex-wrap justify-end gap-3">
            <Link to="/" className="rounded-full border border-rose px-6 py-3 text-rose hover:bg-blush">Back to browsing products</Link>
            <Link
              to="/checkout"
              className={`rounded-full bg-rose px-6 py-3 text-white ${quote.canCheckout ? '' : 'pointer-events-none opacity-40'}`}
            >
              Checkout
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
