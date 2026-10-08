import { useState } from 'react'
import { useCart } from '../cart.jsx'

export default function CouponBox({ quote, align = 'right' }) {
  const { coupon, setCoupon } = useCart()
  const [text, setText] = useState('')
  const side = align === 'left' ? '' : 'justify-end'

  if (coupon) {
    return (
      <div className="mb-3 text-sm">
        <div className={`flex flex-wrap items-center gap-2 ${side}`}>
          <span className="rounded-full bg-blush px-3 py-1 font-medium">{coupon}</span>
          <button type="button" onClick={() => setCoupon('')} className="text-rose underline">Remove</button>
        </div>
        {quote?.couponError && <p className="mt-1 text-rose">{quote.couponError}</p>}
        {quote?.couponCode && <p className="mt-1 text-emerald-700">Coupon applied</p>}
      </div>
    )
  }

  const apply = () => {
    const code = text.trim().toUpperCase()
    if (code) {
      setCoupon(code)
      setText('')
    }
  }

  return (
    <div className={`mb-3 flex gap-2 ${side}`}>
      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); apply() } }}
        placeholder="Coupon code"
        maxLength={40}
        className="w-40 rounded-lg border border-gold/50 bg-white px-3 py-2 text-sm uppercase"
      />
      <button type="button" onClick={apply} className="rounded-full border border-rose px-4 py-2 text-sm text-rose hover:bg-blush">Apply</button>
    </div>
  )
}
