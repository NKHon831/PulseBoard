import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthNav } from '../components/NavBar'
import './Auth.css'

function Login() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({})
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function validate() {
    const next: typeof errors = {}
    if (!email.trim()) next.email = 'Email is required'
    else if (!/^\S+@\S+\.\S+$/.test(email)) next.email = 'Enter a valid email'
    if (!password) next.password = 'Password is required'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError('')
    if (!validate()) return

    setSubmitting(true)
    await new Promise((r) => setTimeout(r, 500))
    setSubmitting(false)
    navigate('/home')
  }

  return (
    <>
      <AuthNav />

      <main className="auth-main">
        <div className="auth-card">
          <div className="auth-head">
            <h1>Welcome back</h1>
            <p className="auth-sub">Log in to keep your streak going.</p>
          </div>

          {formError && <div className="auth-error">{formError}</div>}

          <form className="auth-form" onSubmit={handleSubmit} noValidate>
            <div className={`field${errors.email ? ' field-error' : ''}`}>
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
              />
              {errors.email && <span className="field-error-msg">{errors.email}</span>}
            </div>

            <div className={`field${errors.password ? ' field-error' : ''}`}>
              <div className="field-row">
                <label htmlFor="password">Password</label>
                <Link to="#" className="auth-link" tabIndex={-1}>
                  Forgot password?
                </Link>
              </div>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
              {errors.password && <span className="field-error-msg">{errors.password}</span>}
            </div>

            <button type="submit" className="btn btn-primary auth-submit" disabled={submitting}>
              {submitting ? 'Logging in…' : 'Log in'}
            </button>
          </form>

          <p className="auth-foot">
            Don't have an account?{' '}
            <Link to="/signup" className="auth-link">
              Sign up
            </Link>
          </p>
        </div>
      </main>
    </>
  )
}

export default Login
