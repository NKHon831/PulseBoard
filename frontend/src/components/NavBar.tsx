import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/auth'
import './NavBar.css'

function Logo() {
  return (
    <Link to="/" className="logo">
      <svg viewBox="0 0 48 46" width="24" height="24" aria-hidden="true">
        <path
          fill="var(--accent)"
          d="M25.946 44.938c-.664.845-2.021.375-2.021-.698V33.937a2.26 2.26 0 0 0-2.262-2.262H10.287c-.92 0-1.456-1.04-.92-1.788l7.48-10.471c1.07-1.497 0-3.578-1.842-3.578H1.237c-.92 0-1.456-1.04-.92-1.788L10.013.474c.214-.297.556-.474.92-.474h28.894c.92 0 1.456 1.04.92 1.788l-7.48 10.471c-1.07 1.498 0 3.579 1.842 3.579h11.377c.943 0 1.473 1.088.89 1.83L25.947 44.94z"
        />
      </svg>
      <span>PulseBoard</span>
    </Link>
  )
}

export function LandingNav() {
  return (
    <header className="nav">
      <div className="container nav-inner">
        <Logo />
        <nav className="nav-links">
          <a href="#features">Features</a>
          <a href="#how-it-works">How it works</a>
        </nav>
        <div className="nav-auth">
          <Link to="/login" className="btn btn-ghost nav-cta">
            Log in
          </Link>
          <Link to="/signup" className="btn btn-primary nav-cta">
            Sign up
          </Link>
        </div>
      </div>
    </header>
  )
}

export function AppNav() {
  const location = useLocation()
  const navigate = useNavigate()
  const { logout } = useAuth()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="nav">
      <div className="container nav-inner">
        <Logo />
        <nav className="nav-links">
          <Link to="/home" className={location.pathname === '/home' ? 'nav-current' : undefined}>
            Overview
          </Link>
          <Link to="/expenses" className={location.pathname === '/expenses' ? 'nav-current' : undefined}>
            Expenses
          </Link>
        </nav>
        <button type="button" className="btn btn-ghost nav-cta" onClick={handleLogout}>
          Log out
        </button>
      </div>
    </header>
  )
}

export function AuthNav() {
  return (
    <header className="nav">
      <div className="container nav-inner">
        <Logo />
      </div>
    </header>
  )
}
