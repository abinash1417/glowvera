import { Link, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import RequireAuth from './components/RequireAuth.jsx'
import Shop from './pages/Shop.jsx'
import Product from './pages/Product.jsx'
import Cart from './pages/Cart.jsx'
import Checkout from './pages/Checkout.jsx'
import { Track, OrderPlaced, PaymentReturn } from './pages/OrderPages.jsx'
import { Login, Register, Account } from './pages/AuthPages.jsx'
import AdminLayout from './pages/admin/AdminLayout.jsx'
import Dashboard from './pages/admin/Dashboard.jsx'
import { OrderList, OrderDetail } from './pages/admin/AdminOrders.jsx'
import { ProductList, ProductEditor } from './pages/admin/AdminProducts.jsx'
import AdminInventory from './pages/admin/AdminInventory.jsx'
import Analytics from './pages/admin/Analytics.jsx'
import Coupons from './pages/admin/Coupons.jsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Shop />} />
        <Route path="/product/:slug" element={<Product />} />
        <Route path="/cart" element={<Cart />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/account" element={<Account />} />

        {/* Only logged-in customers can check out, pay and track orders */}
        <Route element={<RequireAuth />}>
          <Route path="/checkout" element={<Checkout />} />
          <Route path="/order-placed" element={<OrderPlaced />} />
          <Route path="/payment/return" element={<PaymentReturn />} />
          <Route path="/payment/cancelled" element={<PaymentReturn cancelled />} />
          <Route path="/track" element={<Track />} />
        </Route>
        <Route path="*" element={<p>Page not found. <Link className="text-rose" to="/">Back to the shop</Link></p>} />
      </Route>

      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<Dashboard />} />
        <Route path="orders" element={<OrderList />} />
        <Route path="orders/:id" element={<OrderDetail />} />
        <Route path="products" element={<ProductList />} />
        <Route path="products/:id" element={<ProductEditor />} />
        <Route path="inventory" element={<AdminInventory />} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="coupons" element={<Coupons />} />
      </Route>
    </Routes>
  )
}
