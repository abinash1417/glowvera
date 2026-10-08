import { Link, NavLink, Navigate, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth.jsx'

const tab = ({ isActive }) =>
  `whitespace-nowrap rounded-full px-4 py-1.5 text-sm ${isActive ? 'bg-rose text-white' : 'hover:bg-blush'}`

export default function AdminLayout() {
  const { user, loading, logout } = useAuth()
  const navigate = useNavigate()

  if (loading) return <p className="p-6 text-muted">Loading...</p>
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />

  return (
    <div className="min-h-screen">
      <header className="border-b border-gold/40 bg-blush">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-4 py-3">
          <Link to="/admin" className="font-display text-xl font-semibold text-rose">
            Glowvera <span className="text-sm font-normal text-muted">Admin</span>
          </Link>
          <div className="flex items-center gap-4 text-sm">
            <Link to="/" className="hover:underline">View store</Link>
            <button onClick={async () => { await logout(); navigate('/login') }}>Log out</button>
          </div>
        </div>
        <nav className="mx-auto flex max-w-6xl gap-2 overflow-x-auto px-4 pb-3">
          <NavLink to="/admin" end className={tab}>Dashboard</NavLink>
          <NavLink to="/admin/orders" className={tab}>Orders</NavLink>
          <NavLink to="/admin/products" className={tab}>Products</NavLink>
          <NavLink to="/admin/inventory" className={tab}>Inventory</NavLink>
          <NavLink to="/admin/analytics" className={tab}>Analytics</NavLink>
          <NavLink to="/admin/coupons" className={tab}>Coupons</NavLink>
        </nav>
      </header>
      <main className="mx-auto w-full max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
