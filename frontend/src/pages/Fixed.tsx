import { useState, type FormEvent } from 'react'
import { AppNav } from '../components/NavBar'
import { ConfirmModal } from '../components/ConfirmModal'
import { apiFetch, ApiError } from '../lib/api'
import { useCurrency, useFormatAmount } from '../lib/currency'
import { CATEGORIES } from '../lib/expenses'
import { formatDate, todayStr } from '../lib/format'
import {
  FREQUENCIES,
  frequencyLabel,
  useRecurringExpenses,
  type Frequency,
  type RecurringExpenseDto,
} from '../lib/recurring'
import './Expenses.css'

function Fixed() {
  const today = todayStr()
  const formatAmount = useFormatAmount()
  const { currency } = useCurrency()

  const { recurring, loading, error: listError, setError: setListError, reload } = useRecurringExpenses()

  const [editing, setEditing] = useState<RecurringExpenseDto | null>(null)
  const [amount, setAmount] = useState('')
  const [category, setCategory] = useState(CATEGORIES[0])
  const [frequency, setFrequency] = useState<Frequency>('DAILY')
  const [description, setDescription] = useState('')
  const [startDate, setStartDate] = useState(today)
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [pendingDelete, setPendingDelete] = useState<RecurringExpenseDto | null>(null)

  function resetForm() {
    setEditing(null)
    setAmount('')
    setCategory(CATEGORIES[0])
    setFrequency('DAILY')
    setDescription('')
    setStartDate(today)
    setFormError('')
  }

  function startEditing(item: RecurringExpenseDto) {
    setEditing(item)
    // The amount is shown in the currently selected currency, so that is what
    // gets edited and what will be saved.
    setAmount(String(item.amount))
    setCategory(item.category)
    setFrequency(item.frequency)
    setDescription(item.description ?? '')
    setStartDate(item.startDate)
    setFormError('')
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError('')

    const amountNum = Number(amount)
    if (!amount || Number.isNaN(amountNum) || amountNum <= 0) {
      setFormError('Enter an amount greater than 0')
      return
    }
    if (!startDate) {
      setFormError('Pick a start date')
      return
    }

    setSubmitting(true)
    try {
      const body = JSON.stringify({
        amount: amountNum,
        currency,
        category,
        description: description.trim() || undefined,
        frequency,
        startDate,
        active: editing ? editing.active : true,
      })

      // `today` is the client's local date: the backend generates any days this
      // fixed expense already owes, up to that date.
      await apiFetch<RecurringExpenseDto>(
        editing ? `/api/recurring-expenses/${editing.id}?today=${today}` : `/api/recurring-expenses?today=${today}`,
        { method: editing ? 'PUT' : 'POST', body },
      )
      resetForm()
      await reload()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to save fixed expense')
    } finally {
      setSubmitting(false)
    }
  }

  async function toggleActive(item: RecurringExpenseDto) {
    try {
      await apiFetch<RecurringExpenseDto>(
        `/api/recurring-expenses/${item.id}/active?currency=${currency}&today=${today}`,
        { method: 'PATCH', body: JSON.stringify({ active: !item.active }) },
      )
      await reload()
    } catch (err) {
      setListError(err instanceof ApiError ? err.message : 'Failed to update fixed expense')
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) return
    const item = pendingDelete
    setPendingDelete(null)

    try {
      await apiFetch<void>(`/api/recurring-expenses/${item.id}`, { method: 'DELETE' })
      if (editing?.id === item.id) resetForm()
      await reload()
    } catch (err) {
      setListError(err instanceof ApiError ? err.message : 'Failed to delete fixed expense')
    }
  }

  const activeCount = recurring.filter((item) => item.active).length

  return (
    <>
      <AppNav />

      <div className="container dashboard">
        <div className="dash-head">
          <div>
            <h1 className="dash-title">Fixed expenses</h1>
            <p className="dash-date">
              Set something once and it is added to your expenses on every matching day.
            </p>
          </div>
        </div>

        <div className="expense-layout">
          <section className="panel">
            <h2 className="panel-title">{editing ? 'Edit fixed expense' : 'Add fixed expense'}</h2>

            {formError && <div className="auth-error">{formError}</div>}

            <form className="expense-form" onSubmit={handleSubmit} noValidate>
              <div className="field">
                <label htmlFor="fixed-amount">Amount ({currency})</label>
                <input
                  id="fixed-amount"
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
                <label htmlFor="fixed-category">Category</label>
                <select id="fixed-category" value={category} onChange={(e) => setCategory(e.target.value)}>
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label htmlFor="fixed-frequency">Repeats</label>
                <select
                  id="fixed-frequency"
                  value={frequency}
                  onChange={(e) => setFrequency(e.target.value as Frequency)}
                >
                  {FREQUENCIES.map((f) => (
                    <option key={f.value} value={f.value}>
                      {f.label} — {f.hint}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label htmlFor="fixed-start">Starts on</label>
                <input
                  id="fixed-start"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>

              <div className="field">
                <label htmlFor="fixed-description">Description (optional)</label>
                <input
                  id="fixed-description"
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Office parking"
                />
              </div>

              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Saving…' : editing ? 'Save changes' : 'Add fixed expense'}
              </button>

              {editing && (
                <button type="button" className="btn btn-ghost" onClick={resetForm}>
                  Cancel
                </button>
              )}
            </form>
          </section>

          <section className="panel">
            <h2 className="panel-title">
              Your fixed expenses{recurring.length > 0 && ` (${activeCount} active)`}
            </h2>

            {listError && <div className="auth-error">{listError}</div>}

            {loading ? (
              <p className="expense-empty">Loading…</p>
            ) : recurring.length === 0 ? (
              <p className="expense-empty">
                No fixed expenses yet. Add one and it will be logged for you each day.
              </p>
            ) : (
              <ul className="expense-list">
                {recurring.map((item) => (
                  <li className={`expense-row fixed-row${item.active ? '' : ' is-paused'}`} key={item.id}>
                    <div className="expense-info">
                      <span className="expense-category">{item.category}</span>
                      <span className="fixed-frequency">{frequencyLabel(item.frequency)}</span>
                      {item.description && <span className="expense-desc">{item.description}</span>}
                      {!item.active && <span className="fixed-paused">Paused</span>}
                      <span className="fixed-since">since {formatDate(item.startDate)}</span>
                    </div>

                    <div className="expense-actions">
                      <span className="expense-amount">{formatAmount(item.amount)}</span>
                      <button type="button" className="btn btn-ghost fixed-action" onClick={() => startEditing(item)}>
                        Edit
                      </button>
                      <button type="button" className="btn btn-ghost fixed-action" onClick={() => toggleActive(item)}>
                        {item.active ? 'Pause' : 'Resume'}
                      </button>
                      <button
                        type="button"
                        className="expense-delete"
                        onClick={() => setPendingDelete(item)}
                        aria-label={`Delete fixed ${item.category} expense of ${formatAmount(item.amount)}`}
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

      <ConfirmModal
        open={pendingDelete !== null}
        title="Delete fixed expense?"
        message={
          pendingDelete
            ? `Stop logging this ${pendingDelete.category} expense of ${formatAmount(pendingDelete.amount)}? Entries it already added are kept.`
            : ''
        }
        confirmLabel="Delete"
        danger
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </>
  )
}

export default Fixed
