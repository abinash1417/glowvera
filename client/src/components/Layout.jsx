import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useCart } from '../cart.jsx'
import { useAuth } from '../auth.jsx'

export default function Layout() {
  const { count } = useCart()
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-gold/40 bg-blush">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-4 py-4">
          <Link to="/" className="font-display text-2xl font-semibold text-rose">Glowvera</Link>
          <nav className="flex flex-wrap items-center gap-5 text-sm">
            {user ? (
              <>
                <Link to="/track">Track order</Link>
                {user.role === 'ADMIN' && <Link to="/admin" className="font-medium text-rose">Admin</Link>}
                <Link to="/account">{user.name.split(' ')[0]}</Link>
                <button onClick={async () => { await logout(); navigate('/') }}>Log out</button>
              </>
            ) : (
              <>
                <Link to="/login">Log in</Link>
                <Link to="/register">Register</Link>
              </>
            )}
            <Link to="/cart">Cart ({count})</Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>
      <footer className="bg-blush text-sm text-muted">
        <div className="mx-auto grid max-w-6xl gap-8 px-4 py-10 sm:grid-cols-2 md:grid-cols-4">
          <div>
            <p className="font-display text-xl font-semibold text-rose">Glowvera</p>
            <p className="mt-2">Pure beauty, naturally yours. Skincare, hair and body care you can trust.</p>
          </div>
          <div>
            <p className="font-medium text-ink">Shop</p>
            <ul className="mt-2 space-y-1">
              <li><Link to="/" className="hover:text-rose">All products</Link></li>
              <li><Link to="/?category=skincare" className="hover:text-rose">Skincare</Link></li>
              <li><Link to="/cart" className="hover:text-rose">Your cart</Link></li>
            </ul>
          </div>
          <div>
            <p className="font-medium text-ink">Account</p>
            <ul className="mt-2 space-y-1">
              {user ? (
                <>
                  <li><Link to="/account" className="hover:text-rose">My account</Link></li>
                  <li><Link to="/track" className="hover:text-rose">Track order</Link></li>
                </>
              ) : (
                <>
                  <li><Link to="/login" className="hover:text-rose">Log in</Link></li>
                  <li><Link to="/register" className="hover:text-rose">Register</Link></li>
                </>
              )}
            </ul>
          </div>
          <div>
            <p className="font-medium text-ink">Payments</p>
            <ul className="mt-2 space-y-1">
              <li>Secure online payment (PayHere)</li>
              <li>Order via WhatsApp</li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gold/40 py-4 text-center text-xs">
          &copy; {new Date().getFullYear()} Glowvera. All rights reserved.
        </div>
      </footer>
    </div>
  )
}
