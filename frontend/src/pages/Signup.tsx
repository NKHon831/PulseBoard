import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthNav } from '../components/NavBar'
import { useAuth } from '../lib/auth'
import { ApiError } from '../lib/api'
import './Auth.css'

type Errors = { name?: string; email?: string; password?: string; confirm?: string }

function Signup() {
  const navigate = useNavigate()
  const { signup } = useAuth()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function validate() {
    const next: Errors = {}
    if (!name.trim()) next.name = 'Name is required'
    if (!email.trim()) next.email = 'Email is required'
    else if (!/^\S+@\S+\.\S+$/.test(email)) next.email = 'Enter a valid email'
    if (!password) next.password = 'Password is required'
    else if (password.length < 8) next.password = 'Use at least 8 characters'
    if (confirm !== password) next.confirm = 'Passwords do not match'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError('')
    if (!validate()) return

    setSubmitting(true)
    try {
      await signup(name.trim(), email.trim(), password)
      navigate('/home')
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <AuthNav />

      <main className="auth-main">
        <div className="auth-card">
          <div className="auth-head">
            <h1>Create your account</h1>
            <p className="auth-sub">Start tracking what matters in under a minute.</p>
          </div>

          {formError && <div className="auth-error">{formError}</div>}

          <form className="auth-form" onSubmit={handleSubmit} noValidate>
            <div className={`field${errors.name ? ' field-error' : ''}`}>
              <label htmlFor="name">Name</label>
              <input
                id="name"
                type="text"
                autoComplete="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Ada Lovelace"
              />
              {errors.name && <span className="field-error-msg">{errors.name}</span>}
            </div>

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
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="At least 8 characters"
              />
              {errors.password && <span className="field-error-msg">{errors.password}</span>}
            </div>

            <div className={`field${errors.confirm ? ' field-error' : ''}`}>
              <label htmlFor="confirm">Confirm password</label>
              <input
                id="confirm"
                type="password"
                autoComplete="new-password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                placeholder="••••••••"
              />
              {errors.confirm && <span className="field-error-msg">{errors.confirm}</span>}
            </div>

            <button type="submit" className="btn btn-primary auth-submit" disabled={submitting}>
              {submitting ? 'Creating account…' : 'Sign up'}
            </button>
          </form>

          <p className="auth-foot">
            Already have an account?{' '}
            <Link to="/login" className="auth-link">
              Log in
            </Link>
          </p>
        </div>
      </main>
    </>
  )
}

export default Signup
