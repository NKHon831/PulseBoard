import { useState } from 'react'
import { AppNav } from '../components/NavBar'
import './Home.css'

type Habit = { id: string; label: string; done: boolean }

const INITIAL_HABITS: Habit[] = [
  { id: 'run', label: 'Morning run', done: true },
  { id: 'read', label: 'Read 20 pages', done: true },
  { id: 'water', label: 'Drink 2L of water', done: true },
  { id: 'meditate', label: 'Meditate 10 min', done: false },
  { id: 'journal', label: 'Journal', done: false },
]

const WEEK = [
  { day: 'Mon', value: 60 },
  { day: 'Tue', value: 80 },
  { day: 'Wed', value: 45 },
  { day: 'Thu', value: 90 },
  { day: 'Fri', value: 70 },
  { day: 'Sat', value: 55 },
  { day: 'Sun', value: 40 },
]

const GOALS = [
  { label: 'Run 100km this month', progress: 62 },
  { label: 'Read 6 books this quarter', progress: 33 },
  { label: '30-day meditation streak', progress: 80 },
]

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
  const [habits, setHabits] = useState(INITIAL_HABITS)
  const done = habits.filter((h) => h.done).length

  function toggle(id: string) {
    setHabits((hs) => hs.map((h) => (h.id === id ? { ...h, done: !h.done } : h)))
  }

  const today = new Date().toLocaleDateString(undefined, {
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
            <h1 className="dash-title">Welcome back</h1>
            <p className="dash-date">{today}</p>
          </div>
          <span className="streak-badge">🔥 12 day streak</span>
        </div>

        <div className="stat-grid">
          <StatCard label="Today's progress" value={`${done}/${habits.length}`} hint="habits completed" />
          <StatCard label="Current streak" value="12 days" hint="personal best: 21" />
          <StatCard label="Weekly goal" value="68%" hint="on track" />
          <StatCard label="Avg. mood" value="4.2/5" hint="last 7 days" />
        </div>

        <div className="dash-grid">
          <section className="panel">
            <h2 className="panel-title">Today's habits</h2>
            <ul className="habit-list">
              {habits.map((h) => (
                <li key={h.id}>
                  <button
                    type="button"
                    className={`habit-item${h.done ? ' done' : ''}`}
                    onClick={() => toggle(h.id)}
                    aria-pressed={h.done}
                  >
                    <span className="habit-check" aria-hidden="true">
                      {h.done && (
                        <svg viewBox="0 0 16 16" width="11" height="11">
                          <path
                            d="M3 8.5l3 3 7-7.5"
                            fill="none"
                            stroke="#fff"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        </svg>
                      )}
                    </span>
                    {h.label}
                  </button>
                </li>
              ))}
            </ul>
          </section>

          <section className="panel">
            <h2 className="panel-title">This week</h2>
            <div className="week-chart">
              {WEEK.map((d) => (
                <div className="week-col" key={d.day}>
                  <span className="week-bar" style={{ height: `${d.value}%` }} />
                  <span className="week-label">{d.day}</span>
                </div>
              ))}
            </div>
          </section>

          <section className="panel panel-wide">
            <h2 className="panel-title">Goals in progress</h2>
            <div className="goal-list">
              {GOALS.map((g) => (
                <div className="goal-row" key={g.label}>
                  <div className="goal-info">
                    <span>{g.label}</span>
                    <span className="goal-pct">{g.progress}%</span>
                  </div>
                  <div className="goal-track">
                    <span className="goal-fill" style={{ width: `${g.progress}%` }} />
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    </>
  )
}

export default Home
