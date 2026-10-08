import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

// Pages inside this wrapper need a logged-in customer.
// After logging in, the customer is sent back to where they were going.
export default function RequireAuth() {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <p className="text-muted">Loading...</p>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  return <Outlet />
}
