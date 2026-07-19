import { Link } from 'react-router-dom'
import { LandingNav } from '../components/NavBar'
import './Landing.css'

const FEATURES = [
  {
    title: 'Track habits',
    desc: 'Log daily habits in seconds and watch your streaks build automatically.',
    icon: (
      <path d="M8 21l6 6 10-12" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    ),
  },
  {
    title: 'Visualize trends',
    desc: 'See weekly and monthly patterns at a glance — no spreadsheets required.',
    icon: (
      <>
        <path d="M6 26V16" strokeWidth="2.5" strokeLinecap="round" />
        <path d="M16 26V6" strokeWidth="2.5" strokeLinecap="round" />
        <path d="M26 26V12" strokeWidth="2.5" strokeLinecap="round" />
      </>
    ),
  },
  {
    title: 'Set goals',
    desc: 'Break big goals into milestones you can actually hit, one check-in at a time.',
    icon: (
      <>
        <circle cx="16" cy="16" r="10" strokeWidth="2.5" />
        <circle cx="16" cy="16" r="5" strokeWidth="2.5" />
        <circle cx="16" cy="16" r="1" strokeWidth="4" strokeLinecap="round" />
      </>
    ),
  },
  {
    title: 'Stay consistent',
    desc: 'Gentle streaks and reminders keep you showing up, without the guilt trips.',
    icon: (
      <path
        d="M16 4c1 5-4 6-4 11a4 4 0 0 0 8 0c0-2-1-3-1-3 2 1 3 4 3 6a6 6 0 0 1-12 0c0-7 6-8 6-14z"
        strokeWidth="2.5"
        strokeLinejoin="round"
      />
    ),
  },
]

const STEPS = [
  {
    n: '01',
    title: 'Add what matters',
    desc: 'Set up the habits, goals, and metrics you actually want to keep an eye on.',
  },
  {
    n: '02',
    title: 'Check in daily',
    desc: 'Quick taps, not long forms. Logging progress takes seconds, not minutes.',
  },
  {
    n: '03',
    title: 'Watch your pulse',
    desc: 'Streaks, trends, and progress update automatically as you go.',
  },
]

function DashboardPreview() {
  return (
    <div className="preview" aria-hidden="true">
      <div className="preview-row">
        <div className="preview-card">
          <span className="preview-label">Streak</span>
          <span className="preview-value">12 days</span>
        </div>
        <div className="preview-card">
          <span className="preview-label">Today</span>
          <span className="preview-value">3/5</span>
        </div>
      </div>
      <div className="preview-chart">
        {[38, 62, 48, 80, 55, 90, 70].map((h, i) => (
          <span key={i} className="preview-bar" style={{ height: `${h}%` }} />
        ))}
      </div>
      <div className="preview-list">
        <div className="preview-item done">
          <span className="preview-check" />
          Morning run
        </div>
        <div className="preview-item done">
          <span className="preview-check" />
          Read 20 pages
        </div>
        <div className="preview-item">
          <span className="preview-check" />
          Meditate
        </div>
      </div>
    </div>
  )
}

function Landing() {
  return (
    <>
      <LandingNav />

      <section className="hero container">
        <div className="hero-copy">
          <span className="eyebrow">Personal dashboard</span>
          <h1>Every habit, goal, and metric — on one pulse.</h1>
          <p className="hero-sub">
            PulseBoard brings your daily habits, personal goals, and progress into a single,
            calm dashboard — so you always know where you stand.
          </p>
          <div className="hero-actions">
            <Link to="/home" className="btn btn-primary">
              Get started
            </Link>
            <a href="#how-it-works" className="btn btn-ghost">
              See how it works
            </a>
          </div>
        </div>
        <DashboardPreview />
      </section>

      <section id="features" className="features container">
        <div className="section-head">
          <span className="eyebrow">Features</span>
          <h2>Everything your routine needs, nothing it doesn't.</h2>
        </div>
        <div className="feature-grid">
          {FEATURES.map((f) => (
            <div className="feature-card" key={f.title}>
              <svg
                className="feature-icon"
                viewBox="0 0 32 32"
                fill="none"
                stroke="var(--accent)"
              >
                {f.icon}
              </svg>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="how-it-works" className="how container">
        <div className="section-head">
          <span className="eyebrow">How it works</span>
          <h2>Three steps to your daily rhythm.</h2>
        </div>
        <div className="steps">
          {STEPS.map((s) => (
            <div className="step" key={s.n}>
              <span className="step-n">{s.n}</span>
              <h3>{s.title}</h3>
              <p>{s.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="cta-banner">
        <div className="container cta-inner">
          <h2>Ready to find your rhythm?</h2>
          <p>Start tracking what matters in under a minute.</p>
          <Link to="/home" className="btn btn-primary">
            Get started — it's free
          </Link>
        </div>
      </section>

      <footer className="footer">
        <div className="container footer-inner">
          <span>© {new Date().getFullYear()} PulseBoard</span>
          <span className="footer-tag">Built for people who like knowing where they stand.</span>
        </div>
      </footer>
    </>
  )
}

export default Landing
