import { useEffect, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { api, postToPayHere } from '../api.js'
import { rs } from '../format.js'
import { useCart } from '../cart.jsx'

const lastOrder = () => {
  try {
    return JSON.parse(localStorage.getItem('lastOrder'))
  } catch {
    return null
  }
}
const trackUrl = (code) => `/orders/track?code=${encodeURIComponent(code)}`

function Summary({ o }) {
  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
      <p>Order <b>{o.orderCode}</b></p>
      <p className="text-sm">Status: <b>{o.status.replace(/_/g, ' ')}</b></p>
      <ul className="mt-3 text-sm">
        {o.items.map((i, n) => (
          <li key={n} className="flex justify-between"><span>{i.productName} ({i.variantName}) × {i.quantity}</span><span>{rs(i.lineTotalCents)}</span></li>
        ))}
      </ul>
      {o.discountCents > 0 && <p className="mt-3 flex justify-between text-sm text-emerald-700"><span>Discount ({o.couponCode})</span><span>-{rs(o.discountCents)}</span></p>}
      <p className="mt-3 flex justify-between border-t border-gold/30 pt-3 font-semibold"><span>Total</span><span>{rs(o.totalCents)}</span></p>
    </div>
  )
}

export function Track() {
  const saved = lastOrder()
  const [code, setCode] = useState(saved?.code ?? '')
  const [order, setOrder] = useState(null)
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setOrder(null)
    try {
      setOrder(await api(trackUrl(code)))
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="max-w-md space-y-3">
      <h1 className="font-display text-3xl font-semibold">Track your order</h1>
      <form onSubmit={submit} className="space-y-3">
        <input required value={code} onChange={(e) => setCode(e.target.value)} placeholder="Order code, e.g. GLW-2026-00012" className="w-full rounded-lg border border-gold/50 bg-white px-3 py-2" />
        <button className="rounded-full bg-rose px-6 py-3 text-white hover:bg-rose-dark">Find my order</button>
      </form>
      {error && <p className="text-rose">{error}</p>}
      {order && <Summary o={order} />}
    </div>
  )
}

export function OrderPlaced() {
  const { state } = useLocation()
  if (!state?.order) return <p>Looking for an order? <Link to="/track" className="text-rose underline">Track it here</Link>.</p>
  return (
    <div className="max-w-md space-y-4">
      <h1 className="font-display text-3xl font-semibold">Your order is saved</h1>
      <p>Send it on WhatsApp so we can confirm it. Keep your order code: <b>{state.order.orderCode}</b>.</p>
      <a href={state.url} target="_blank" rel="noopener noreferrer" className="inline-block rounded-full bg-rose px-6 py-3 text-white hover:bg-rose-dark">Open WhatsApp</a>
      <Summary o={state.order} />
    </div>
  )
}

export function PaymentReturn({ cancelled = false }) {
  const [params] = useSearchParams()
  const { clear } = useCart()
  const code = params.get('order')
  const [order, setOrder] = useState(null)
  const [error, setError] = useState('')

  // PayHere confirms to our server separately, so we check the order status for a short while
  useEffect(() => {
    if (cancelled || !code) return
    let tries = 0
    let timer
    const check = async () => {
      try {
        const o = await api(trackUrl(code))
        setOrder(o)
        if (o.status !== 'PENDING_PAYMENT') return clear()
      } catch (e) {
        setError(e.message)
      }
      if (++tries < 20) timer = setTimeout(check, 3000)
    }
    check()
    return () => clearTimeout(timer)
  }, [code])

  const retry = async () => {
    try {
      postToPayHere((await api('/payments/payhere/retry', { method: 'POST', body: { code } })).clientAction)
    } catch (e) {
      setError(e.message)
    }
  }

  const pending = order?.status === 'PENDING_PAYMENT'
  const paid = order && !pending
  return (
    <div className="max-w-md space-y-4">
      <h1 className="font-display text-3xl font-semibold">
        {cancelled ? 'Payment cancelled' : paid ? 'Thank you, payment received' : 'Confirming your payment'}
      </h1>
      {cancelled && <p>No money was taken. Your items are held for a short time.</p>}
      {!cancelled && pending && <p className="text-muted">Waiting for PayHere to confirm. This can take a minute.</p>}
      {error && <p className="text-rose">{error}</p>}
      {(cancelled || pending) && code && (
        <button onClick={retry} className="rounded-full bg-rose px-6 py-3 text-white hover:bg-rose-dark">Try paying again</button>
      )}
      {order && <Summary o={order} />}
    </div>
  )
}
