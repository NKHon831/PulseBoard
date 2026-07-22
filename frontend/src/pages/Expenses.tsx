import { useEffect, useState, type FormEvent } from 'react'
import { AppNav } from '../components/NavBar'
import { apiFetch, ApiError } from '../lib/api'
import './Expenses.css'

type ExpenseDto = {
  id: string
  amount: number
  category: string
  description: string | null
  expenseDate: string
}

const CATEGORIES = ['Breakfast', 'Lunch', 'Dinner', 'Others']

function todayStr() {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

function formatAmount(n: number) {
  return `$${n.toFixed(2)}`
}

function formatDate(iso: string) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

function StatCard({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <div className="stat-card">
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
      <span className="stat-hint">{hint}</span>
    </div>
  )
}

function Expenses() {
  const today = todayStr()

  const [expenses, setExpenses] = useState<ExpenseDto[]>([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState('')

  const [amount, setAmount] = useState('')
  const [category, setCategory] = useState(CATEGORIES[0])
  const [description, setDescription] = useState('')
  const [date, setDate] = useState(today)
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function loadExpenses() {
    try {
      const data = await apiFetch<ExpenseDto[]>('/api/expenses')
      setExpenses(data)
      setListError('')
    } catch (err) {
      setListError(err instanceof ApiError ? err.message : 'Failed to load expenses')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadExpenses()
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError('')

    const amountNum = Number(amount)
    if (!amount || Number.isNaN(amountNum) || amountNum <= 0) {
      setFormError('Enter an amount greater than 0')
      return
    }
    if (!date) {
      setFormError('Pick a date')
      return
    }

    setSubmitting(true)
    try {
      await apiFetch<ExpenseDto>('/api/expenses', {
        method: 'POST',
        body: JSON.stringify({
          amount: amountNum,
          category,
          description: description.trim() || undefined,
          expenseDate: date,
        }),
      })
      setAmount('')
      setDescription('')
      setDate(today)
      await loadExpenses()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to add expense')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id: string) {
    try {
      await apiFetch<void>(`/api/expenses/${id}`, { method: 'DELETE' })
      setExpenses((prev) => prev.filter((expense) => expense.id !== id))
    } catch (err) {
      setListError(err instanceof ApiError ? err.message : 'Failed to delete expense')
    }
  }

  const todayTotal = expenses.filter((e) => e.expenseDate === today).reduce((sum, e) => sum + e.amount, 0)
  const monthPrefix = today.slice(0, 7)
  const monthExpenses = expenses.filter((e) => e.expenseDate.startsWith(monthPrefix))
  const monthTotal = monthExpenses.reduce((sum, e) => sum + e.amount, 0)

  return (
    <>
      <AppNav />

      <div className="container dashboard">
        <div className="dash-head">
          <div>
            <h1 className="dash-title">Expenses</h1>
            <p className="dash-date">Keep track of what you spend, day by day.</p>
          </div>
        </div>

        <div className="stat-grid expense-stat-grid">
          <StatCard label="Today" value={formatAmount(todayTotal)} hint="spent so far" />
          <StatCard label="This month" value={formatAmount(monthTotal)} hint={`${monthExpenses.length} entries`} />
          <StatCard label="Total logged" value={String(expenses.length)} hint="all-time entries" />
        </div>

        <div className="expense-layout">
          <section className="panel">
            <h2 className="panel-title">Add expense</h2>

            {formError && <div className="auth-error">{formError}</div>}

            <form className="expense-form" onSubmit={handleSubmit} noValidate>
              <div className="field">
                <label htmlFor="amount">Amount</label>
                <input
                  id="amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  inputMode="decimal"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="0.00"
                />
              </div>

              <div className="field">
                <label htmlFor="category">Category</label>
                <select id="category" value={category} onChange={(e) => setCategory(e.target.value)}>
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label htmlFor="date">Date</label>
                <input
                  id="date"
                  type="date"
                  max={today}
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                />
              </div>

              <div className="field">
                <label htmlFor="description">Description (optional)</label>
                <input
                  id="description"
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Lunch with the team"
                />
              </div>

              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Adding…' : 'Add expense'}
              </button>
            </form>
          </section>

          <section className="panel">
            <h2 className="panel-title">Recent expenses</h2>

            {listError && <div className="auth-error">{listError}</div>}

            {loading ? (
              <p className="expense-empty">Loading…</p>
            ) : expenses.length === 0 ? (
              <p className="expense-empty">No expenses logged yet. Add your first one.</p>
            ) : (
              <ul className="expense-list">
                {expenses.map((e) => (
                  <li className="expense-row" key={e.id}>
                    <div className="expense-info">
                      <span className="expense-category">{e.category}</span>
                      {e.description && <span className="expense-desc">{e.description}</span>}
                      <span className="expense-date">{formatDate(e.expenseDate)}</span>
                    </div>
                    <div className="expense-actions">
                      <span className="expense-amount">{formatAmount(e.amount)}</span>
                      <button
                        type="button"
                        className="expense-delete"
                        onClick={() => handleDelete(e.id)}
                        aria-label={`Delete ${e.category} expense of ${formatAmount(e.amount)}`}
                      >
                        ×
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      </div>
    </>
  )
}

export default Expenses
