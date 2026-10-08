import { createContext, useContext, useEffect, useState } from 'react'

const CartContext = createContext(null)
export const useCart = () => useContext(CartContext)

// The cart stores only ids, names and quantities. Prices always come from the server.
const load = () => {
  try {
    return JSON.parse(localStorage.getItem('cart')) ?? []
  } catch {
    return []
  }
}

export function CartProvider({ children }) {
  const [items, setItems] = useState(load)
  const [coupon, setCouponState] = useState(() => localStorage.getItem('coupon') ?? '')

  useEffect(() => {
    try {
      localStorage.setItem('cart', JSON.stringify(items))
    } catch {
      /* storage can be unavailable; the cart still works for this visit */
    }
  }, [items])

  const add = (item, quantity = 1) =>
    setItems((cur) => {
      const found = cur.find((i) => i.variantId === item.variantId)
      return found
        ? cur.map((i) => (i === found ? { ...i, quantity: Math.min(i.quantity + quantity, 50) } : i))
        : [...cur, { ...item, quantity }]
    })

  const setQty = (variantId, quantity) =>
    setItems((cur) =>
      quantity <= 0
        ? cur.filter((i) => i.variantId !== variantId)
        : cur.map((i) => (i.variantId === variantId ? { ...i, quantity: Math.min(quantity, 50) } : i)),
    )

  const setCoupon = (code) => {
    setCouponState(code)
    try {
      code ? localStorage.setItem('coupon', code) : localStorage.removeItem('coupon')
    } catch {
      /* ignore */
    }
  }

  const clear = () => {
    setItems([])
    setCoupon('')
  }
  const count = items.reduce((sum, i) => sum + i.quantity, 0)

  return <CartContext.Provider value={{ items, add, setQty, clear, count, coupon, setCoupon }}>{children}</CartContext.Provider>
}
