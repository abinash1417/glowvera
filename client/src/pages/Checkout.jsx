import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, postToPayHere } from '../api.js'
import { rs } from '../format.js'
import { useCart } from '../cart.jsx'
import { useAuth } from '../auth.jsx'
import CouponBox from '../components/CouponBox.jsx'

const input = 'w-full rounded-lg border border-gold/50 bg-white px-3 py-2'

export default function Checkout() {
  const { items, setQty, clear, coupon } = useCart()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: user?.name ?? '', email: user?.email ?? '', phone: user?.phone ?? '', addressLine: '', city: '', district: '', notes: '' })
  const [method, setMethod] = useState('PAYHERE')
  const [districts, setDistricts] = useState([])
  const [quote, setQuote] = useState(null)
  const [error, setError] = useState('')
  const [problems, setProblems] = useState([])
  const [busy, setBusy] = useState(false)

  const cartItems = items.map(({ variantId, quantity }) => ({ variantId, quantity }))
  const field = (name) => ({ value: form[name], onChange: (e) => setForm({ ...form, [name]: e.target.value }), className: input })

  useEffect(() => {
    api('/cart/districts').then(setDistricts).catch(() => {})
  }, [])

  useEffect(() => {
    if (!cartItems.length) return
    api('/cart/quote', { method: 'POST', body: { items: cartItems, district: form.district || undefined, couponCode: coupon || undefined } })
      .then(setQuote)
      .catch((e) => setError(e.message))
  }, [form.district, items, coupon])

  if (!items.length) return <p>Your cart is empty. <Link to="/" className="text-rose underline">Browse the shop</Link></p>

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    setProblems([])
    try {
      const { notes, ...customer } = form
      const data = await api('/orders/checkout', {
        method: 'POST',
        body: { method, items: cartItems, customer, notes: notes || undefined, couponCode: coupon || undefined },
      })
      localStorage.setItem('lastOrder', JSON.stringify({ code: data.order.orderCode, phone: form.phone }))
      const action = data.clientAction
      if (action.type === 'PAYHERE_FORM') return postToPayHere(action)
      clear()
      navigate('/order-placed', { state: { order: data.order, url: action.url } })
    } catch (err) {
      setError(err.message)
      setProblems(Array.isArray(err.details) ? err.details : [])
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="grid max-w-4xl gap-8 md:grid-cols-2">
      <div className="space-y-3">
        <h1 className="font-display text-3xl font-semibold">Checkout</h1>
        <input required placeholder="Full name" autoComplete="name" {...field('name')} />
        <input required type="email" placeholder="Email" autoComplete="email" {...field('email')} />
        <input required placeholder="Mobile, e.g. 0771234567" autoComplete="tel" {...field('phone')} />
        <input required placeholder="Address" autoComplete="street-address" {...field('addressLine')} />
        <input required placeholder="City" {...field('city')} />
        <select required {...field('district')}>
          <option value="">Choose district</option>
          {districts.map((d) => <option key={d.district} value={d.district}>{d.district} ({rs(d.feeCents)})</option>)}
        </select>
        <textarea placeholder="Notes for delivery (optional)" maxLength={500} {...field('notes')} />
      </div>

      <div className="space-y-4">
        <div className="rounded-2xl bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <h2 className="font-medium">Order summary</h2>
            <button type="button" onClick={() => window.confirm('Remove everything from your cart?') && clear()} className="text-sm text-rose underline">Clear cart</button>
          </div>
          {quote?.lines.map((l) => (
            <div key={l.variantId} className="mt-2 flex items-start justify-between gap-2 text-sm">
              <span>
                {l.productName} ({l.variantName}) × {l.quantity}
                <button type="button" onClick={() => setQty(l.variantId, 0)} className="ml-2 text-rose underline">Remove</button>
              </span>
              <span>{l.lineTotalCents != null ? rs(l.lineTotalCents) : ''}</span>
            </div>
          ))}
          <Link to="/" className="mt-3 inline-block text-sm text-rose underline">Back to browsing products</Link>
          <div className="mt-3"><CouponBox quote={quote} align="left" /></div>
          {quote && (
            <div className="mt-3 border-t border-gold/30 pt-3 text-sm">
              <p className="flex justify-between"><span>Subtotal</span><span>{rs(quote.subtotalCents)}</span></p>
              {quote.discountCents > 0 && <p className="flex justify-between text-emerald-700"><span>Discount ({quote.couponCode})</span><span>-{rs(quote.discountCents)}</span></p>}
              <p className="flex justify-between"><span>Delivery</span><span>{quote.shippingCents != null ? rs(quote.shippingCents) : 'Choose a district'}</span></p>
              <p className="mt-1 flex justify-between font-semibold"><span>Total</span><span>{rs(quote.totalCents)}</span></p>
            </div>
          )}
        </div>

        <fieldset className="space-y-2">
          <legend className="font-medium">How would you like to order?</legend>
          <label className="flex items-center gap-2"><input type="radio" checked={method === 'PAYHERE'} onChange={() => setMethod('PAYHERE')} /> Pay online with PayHere</label>
          <label className="flex items-center gap-2"><input type="radio" checked={method === 'WHATSAPP'} onChange={() => setMethod('WHATSAPP')} /> Send my order on WhatsApp</label>
        </fieldset>

        {error && <p className="text-rose">{error}</p>}
        {problems.map((p, i) => (
          <p key={i} className="text-sm text-rose">{p.name ?? 'An item'}: {p.available > 0 ? `only ${p.available} available` : 'unavailable'}. Update your cart.</p>
        ))}

        <button disabled={busy || !quote?.canCheckout} className="w-full rounded-full bg-rose px-6 py-3 text-white hover:bg-rose-dark disabled:opacity-40">
          {busy ? 'Placing order...' : method === 'PAYHERE' ? 'Pay with PayHere' : 'Place order and open WhatsApp'}
        </button>
      </div>
    </form>
  )
}
