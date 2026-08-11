import { useState, type FormEvent } from 'react'
import { AppNav } from '../components/NavBar'
import { ConfirmModal } from '../components/ConfirmModal'
import { apiFetch, ApiError } from '../lib/api'
import { useCurrency, useFormatAmount } from '../lib/currency'
import { CATEGORIES, useExpenses, groupByDay, type ExpenseDto } from '../lib/expenses'
import { formatDate, todayStr } from '../lib/format'
import './Expenses.css'

/** One line of the add form. Several can be filled in and submitted together. */
type DraftEntry = {
  key: number
  amount: string
  category: string
  description: string
}

let nextDraftKey = 0

function blankEntry(): DraftEntry {
  return { key: nextDraftKey++, amount: '', category: CATEGORIES[0], description: '' }
}

/** Only the first row of the form shows its labels; see .field-label-repeat. */
function labelClass(index: number) {
  return index === 0 ? undefined : 'field-label-repeat'
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
  const formatAmount = useFormatAmount()
  const { currency } = useCurrency()

  const { expenses, setExpenses, loading, error: listError, setError: setListError, reload } = useExpenses()

  const [entries, setEntries] = useState<DraftEntry[]>(() => [blankEntry()])
  const [date, setDate] = useState(today)
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [pendingDelete, setPendingDelete] = useState<ExpenseDto | null>(null)

  function updateEntry(key: number, patch: Partial<DraftEntry>) {
    setEntries((prev) => prev.map((entry) => (entry.key === key ? { ...entry, ...patch } : entry)))
  }

  function removeEntry(key: number) {
    setEntries((prev) => (prev.length === 1 ? prev : prev.filter((entry) => entry.key !== key)))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError('')

    if (!date) {
      setFormError('Pick a date')
      return
    }
    // The form is noValidate, so the input's `max` is not enforced on submit.
    // Both are YYYY-MM-DD, so a string compare is a date compare.
    if (date > today) {
      setFormError('Date cannot be in the future')
      return
    }

    // An untouched line is just an empty slot, not a mistake — drop it. A line
    // with a description but no amount is half-filled, so say so rather than
    // quietly discarding what was typed.
    const filled = []
    for (const [index, entry] of entries.entries()) {
      const amountNum = Number(entry.amount)
      if (!entry.amount.trim()) {
        if (entry.description.trim()) {
          setFormError(`Entry ${index + 1}: enter an amount greater than 0`)
          return
        }
        continue
      }
      if (Number.isNaN(amountNum) || amountNum <= 0) {
        setFormError(`Entry ${index + 1}: enter an amount greater than 0`)
        return
      }
      filled.push({
        amount: amountNum,
        currency,
        category: entry.category,
        description: entry.description.trim() || undefined,
        expenseDate: date,
      })
    }

    if (filled.length === 0) {
      setFormError('Enter an amount greater than 0')
      return
    }

    setSubmitting(true)
    try {
      // Amounts go up exactly as typed plus the currency they were typed in; the
      // backend converts them at the rate current right now. The whole batch is
      // saved together, so a rejected line leaves none of them stored.
      await apiFetch<ExpenseDto[]>('/api/expenses/batch', {
        method: 'POST',
        body: JSON.stringify({ expenses: filled }),
      })
      setEntries([blankEntry()])
      setDate(today)
      await reload()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to add expenses')
    } finally {
      setSubmitting(false)
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) return
    const expense = pendingDelete
    setPendingDelete(null)

    try {
      await apiFetch<void>(`/api/expenses/${expense.id}`, { method: 'DELETE' })
      setExpenses((prev) => prev.filter((e) => e.id !== expense.id))
    } catch (err) {
      setListError(err instanceof ApiError ? err.message : 'Failed to delete expense')
    }
  }

  const todayTotal = expenses.filter((e) => e.expenseDate === today).reduce((sum, e) => sum + e.amount, 0)
  const monthPrefix = today.slice(0, 7)
  const monthExpenses = expenses.filter((e) => e.expenseDate.startsWith(monthPrefix))
  const monthTotal = monthExpenses.reduce((sum, e) => sum + e.amount, 0)
  const dayGroups = groupByDay(expenses)

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

        <div className="expense-layout expense-layout-stacked">
          <section className="panel">
            <h2 className="panel-title">Add expense</h2>

            {formError && <div className="auth-error">{formError}</div>}

            <form className="expense-form" onSubmit={handleSubmit} noValidate>
              <div className="field entry-date">
                <label htmlFor="date">Date</label>
                <input
                  id="date"
                  type="date"
                  max={today}
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                />
              </div>

              <div className="entry-rows">
                {entries.map((entry, index) => (
                  <fieldset className="entry-row" key={entry.key}>
                    <legend className="sr-only">Entry {index + 1}</legend>

                    <div className="field">
                      {/* Labelling every row would repeat the same three words down
                          the form, so later rows keep their labels for screen
                          readers only — until the fields stack and need them back. */}
                      <label htmlFor={`amount-${entry.key}`} className={labelClass(index)}>
                        Amount ({currency})
                      </label>
                      <input
                        id={`amount-${entry.key}`}
                        type="number"
                        min="0.01"
                        step="0.01"
                        inputMode="decimal"
                        value={entry.amount}
                        onChange={(e) => updateEntry(entry.key, { amount: e.target.value })}
                        placeholder="0.00"
                      />
                    </div>

                    <div className="field">
                      <label htmlFor={`category-${entry.key}`} className={labelClass(index)}>
                        Category
                      </label>
                      <select
                        id={`category-${entry.key}`}
                        value={entry.category}
                        onChange={(e) => updateEntry(entry.key, { category: e.target.value })}
                      >
                        {CATEGORIES.map((c) => (
                          <option key={c} value={c}>
                            {c}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="field">
                      <label htmlFor={`description-${entry.key}`} className={labelClass(index)}>
                        Description (optional)
                      </label>
                      <input
                        id={`description-${entry.key}`}
                        type="text"
                        value={entry.description}
                        onChange={(e) => updateEntry(entry.key, { description: e.target.value })}
                        placeholder="Lunch with the team"
                      />
                    </div>

                    {/* Held open even with nothing in it, so the fields above stay
                        in line whether or not a row can be removed. */}
                    <div className="entry-row-action">
                      {entries.length > 1 && (
                        <button
                          type="button"
                          className="expense-delete"
                          onClick={() => removeEntry(entry.key)}
                          aria-label={`Remove entry ${index + 1}`}
                        >
                          ×
                        </button>
                      )}
                    </div>
                  </fieldset>
                ))}
              </div>

              <div className="entry-actions">
                <button
                  type="button"
                  className="btn btn-ghost entry-add"
                  onClick={() => setEntries((prev) => [...prev, blankEntry()])}
                >
                  + Add another entry
                </button>

                <button type="submit" className="btn btn-primary entry-submit" disabled={submitting}>
                  {submitting ? 'Adding…' : entries.length > 1 ? `Add ${entries.length} expenses` : 'Add expense'}
                </button>
              </div>
            </form>
          </section>

          <section className="panel">
            <h2 className="panel-title">Recent expenses</h2>

            {listError && <div className="auth-error">{listError}</div>}

            {loading ? (
              <p className="expense-empty">Loading…</p>
            ) : dayGroups.length === 0 ? (
              <p className="expense-empty">No expenses logged yet. Add your first one.</p>
            ) : (
              <div className="expense-groups">
                {dayGroups.map((group) => (
                  <div className="expense-day-group" key={group.date}>
                    <div className="expense-day-head">
                      <span className="expense-day-date">{formatDate(group.date)}</span>
                      <span className="expense-day-total">{formatAmount(group.total)}</span>
                    </div>
                    <ul className="expense-list">
                      {group.items.map((e) => (
                        <li className="expense-row" key={e.id}>
                          <div className="expense-info">
                            <span className="expense-category">{e.category}</span>
                            {e.recurring && (
                              <span className="expense-fixed" title="Added automatically by a fixed expense">
                                Fixed
                              </span>
                            )}
                            {e.description && <span className="expense-desc">{e.description}</span>}
                          </div>
                          <div className="expense-actions">
                            <span className="expense-amount">{formatAmount(e.amount)}</span>
                            <button
                              type="button"
                              className="expense-delete"
                              onClick={() => setPendingDelete(e)}
                              aria-label={`Delete ${e.category} expense of ${formatAmount(e.amount)}`}
                            >
                              ×
                            </button>
                          </div>
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      </div>

      <ConfirmModal
        open={pendingDelete !== null}
        title="Delete expense?"
        message={
          pendingDelete
            ? `Delete this ${pendingDelete.category} expense of ${formatAmount(pendingDelete.amount)} from ${formatDate(pendingDelete.expenseDate)}? This cannot be undone.`
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

export default Expenses
