import { Link } from 'react-router-dom'
import { AppNav } from '../components/NavBar'
import { useAuth } from '../lib/auth'
import { useFormatAmount } from '../lib/currency'
import { useExpenses, groupByDay } from '../lib/expenses'
import { formatDate, todayStr } from '../lib/format'
import './Home.css'

function StatCard({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <div className="stat-card">
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
      <span className="stat-hint">{hint}</span>
    </div>
  )
}

function Home() {
  const { user } = useAuth()
  const formatAmount = useFormatAmount()
  const { expenses, loading } = useExpenses()

  const today = todayStr()
  const monthPrefix = today.slice(0, 7)

  const todayTotal = expenses.filter((e) => e.expenseDate === today).reduce((sum, e) => sum + e.amount, 0)
  const monthExpenses = expenses.filter((e) => e.expenseDate.startsWith(monthPrefix))
  const monthTotal = monthExpenses.reduce((sum, e) => sum + e.amount, 0)
  const recentDays = groupByDay(expenses).slice(0, 4)

  const firstName = user?.name.split(' ')[0] ?? ''
  const dateLabel = new Date().toLocaleDateString(undefined, {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  })

  return (
    <>
      <AppNav />

      <div className="container dashboard">
        <div className="dash-head">
          <div>
            <h1 className="dash-title">Welcome back{firstName && `, ${firstName}`}</h1>
            <p className="dash-date">{dateLabel}</p>
          </div>
        </div>

        <div className="stat-grid">
          <StatCard label="Today" value={formatAmount(todayTotal)} hint="spent so far" />
          <StatCard label="This month" value={formatAmount(monthTotal)} hint={`${monthExpenses.length} entries`} />
          <StatCard label="Total logged" value={String(expenses.length)} hint="all-time entries" />
        </div>

        <div className="dash-grid">
          <section className="panel panel-wide">
            <div className="panel-head">
              <h2 className="panel-title">Expenses summary</h2>
              <Link to="/expenses" className="panel-link">
                View all
              </Link>
            </div>

            {loading ? (
              <p className="summary-empty">Loading…</p>
            ) : recentDays.length === 0 ? (
              <p className="summary-empty">No expenses logged yet.</p>
            ) : (
              <ul className="day-summary-list">
                {recentDays.map((group) => (
                  <li className="day-summary-row" key={group.date}>
                    <span className="day-summary-date">{formatDate(group.date)}</span>
                    <span className="day-summary-total">{formatAmount(group.total)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="panel module-placeholder">
            <h2 className="panel-title">Water intake</h2>
            <div className="placeholder-body">
              <span className="placeholder-icon" aria-hidden="true">
                💧
              </span>
              <p>Track your daily water intake here.</p>
              <span className="coming-soon-badge">Coming soon</span>
            </div>
          </section>

          <section className="panel module-placeholder">
            <h2 className="panel-title">Sleep schedule</h2>
            <div className="placeholder-body">
              <span className="placeholder-icon" aria-hidden="true">
                🌙
              </span>
              <p>Track your sleep hours and schedule here.</p>
              <span className="coming-soon-badge">Coming soon</span>
            </div>
          </section>
        </div>
      </div>
    </>
  )
}

export default Home
