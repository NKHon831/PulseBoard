import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/auth'
import { useCurrency } from '../lib/currency'
import { BASE_CURRENCY, CURRENCIES, CURRENCY_CODES, formatRate, type CurrencyCode } from '../lib/currencies'
import './NavBar.css'

function Logo() {
  const { user } = useAuth()
  // Once logged in, the logo should return to the dashboard, not the landing page.
  const to = user ? '/home' : '/'

  return (
    <Link to={to} className="logo">
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

function CurrencySelect({ id }: { id: string }) {
  const { currency, setCurrency } = useCurrency()

  return (
    <label className="currency-select" htmlFor={id}>
      <span className="sr-only">Display currency</span>
      <select
        id={id}
        value={currency}
        onChange={(e) => setCurrency(e.target.value as CurrencyCode)}
      >
        {CURRENCY_CODES.map((code) => (
          <option key={code} value={code}>
            {CURRENCIES[code].label}
          </option>
        ))}
      </select>
    </label>
  )
}

function CurrencyRate() {
  const { currency, rates } = useCurrency()
  const rate = rates[currency]
  // Only meaningful for a non-base currency, and only once the rate has loaded.
  if (currency === BASE_CURRENCY || rate === undefined) return null

  return (
    <span className="currency-rate">
      1 {BASE_CURRENCY} = {formatRate(rate)} {CURRENCIES[currency].code}
    </span>
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
  const [menuOpen, setMenuOpen] = useState(false)

  // Close the mobile menu whenever the route changes.
  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  function handleLogout() {
    setMenuOpen(false)
    logout()
    navigate('/login')
  }

  return (
    <header className="nav">
      <div className="container nav-inner">
        <Logo />

        <nav className="nav-links nav-links-desktop">
          <Link to="/home" className={location.pathname === '/home' ? 'nav-current' : undefined}>
            Overview
          </Link>
          <Link to="/expenses" className={location.pathname === '/expenses' ? 'nav-current' : undefined}>
            Expenses
          </Link>
          <Link to="/fixed" className={location.pathname === '/fixed' ? 'nav-current' : undefined}>
            Fixed
          </Link>
        </nav>

        <div className="nav-auth nav-auth-desktop">
          <CurrencyRate />
          <CurrencySelect id="currency-desktop" />
          <button type="button" className="btn btn-ghost nav-cta" onClick={handleLogout}>
            Log out
          </button>
        </div>

        <button
          type="button"
          className="nav-toggle"
          aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={menuOpen}
          aria-controls="mobile-menu"
          onClick={() => setMenuOpen((open) => !open)}
        >
          <span className={`nav-toggle-bars${menuOpen ? ' is-open' : ''}`} aria-hidden="true">
            <span />
            <span />
            <span />
          </span>
        </button>
      </div>

      {menuOpen && <div className="nav-backdrop" onClick={() => setMenuOpen(false)} aria-hidden="true" />}

      <aside id="mobile-menu" className={`mobile-menu${menuOpen ? ' is-open' : ''}`}>
        <nav className="mobile-menu-links">
          <Link
            to="/home"
            className={location.pathname === '/home' ? 'mobile-link nav-current' : 'mobile-link'}
          >
            Overview
          </Link>
          <Link
            to="/expenses"
            className={location.pathname === '/expenses' ? 'mobile-link nav-current' : 'mobile-link'}
          >
            Expenses
          </Link>
          <Link
            to="/fixed"
            className={location.pathname === '/fixed' ? 'mobile-link nav-current' : 'mobile-link'}
          >
            Fixed
          </Link>
        </nav>

        <div className="mobile-menu-footer">
          <CurrencySelect id="currency-mobile" />
          <CurrencyRate />
          <button type="button" className="btn btn-ghost" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </aside>
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
