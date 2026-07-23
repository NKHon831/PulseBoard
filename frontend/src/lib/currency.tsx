import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import {
  BASE_CURRENCY,
  CURRENCIES,
  CURRENCY_CODES,
  fetchRates,
  formatAmount as formatIn,
  type CurrencyCode,
} from './currencies'

const CURRENCY_KEY = 'pulseboard.currency'
const RATES_KEY = 'pulseboard.rates'

type Rates = Record<CurrencyCode, number>

function staticRates(): Rates {
  return Object.fromEntries(CURRENCY_CODES.map((code) => [code, CURRENCIES[code].rate])) as Rates
}

function readStoredCurrency(): CurrencyCode {
  const saved = localStorage.getItem(CURRENCY_KEY)
  return saved === 'JPY' || saved === 'MYR' ? saved : 'MYR'
}

// Seed from the last rates we cached (instant + offline), falling back to the
// static approximations baked into CURRENCIES.
function readStoredRates(): Rates {
  const base = staticRates()
  try {
    const saved = localStorage.getItem(RATES_KEY)
    if (!saved) return base
    const parsed = JSON.parse(saved) as Partial<Record<CurrencyCode, number>>
    for (const code of CURRENCY_CODES) {
      const rate = parsed[code]
      if (typeof rate === 'number') base[code] = rate
    }
  } catch {
    // Ignore malformed cache and use the static fallbacks.
  }
  base[BASE_CURRENCY] = 1
  return base
}

type CurrencyContextValue = {
  currency: CurrencyCode
  setCurrency: (code: CurrencyCode) => void
  rates: Rates
}

const CurrencyContext = createContext<CurrencyContextValue | undefined>(undefined)

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const [currency, setCurrencyState] = useState<CurrencyCode>(readStoredCurrency)
  const [rates, setRates] = useState<Rates>(readStoredRates)

  const setCurrency = useCallback((code: CurrencyCode) => {
    setCurrencyState(code)
    localStorage.setItem(CURRENCY_KEY, code)
  }, [])

  // Refresh live exchange rates on mount; keep cached/fallback rates if it fails.
  useEffect(() => {
    let cancelled = false
    fetchRates(CURRENCY_CODES)
      .then((live) => {
        if (cancelled || Object.keys(live).length === 0) return
        setRates((prev) => {
          const next = { ...prev, ...live, [BASE_CURRENCY]: 1 }
          localStorage.setItem(RATES_KEY, JSON.stringify(next))
          return next
        })
      })
      .catch(() => {
        // Offline or API error — the cached/static rates remain in effect.
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <CurrencyContext.Provider value={{ currency, setCurrency, rates }}>
      {children}
    </CurrencyContext.Provider>
  )
}

export function useCurrency() {
  const ctx = useContext(CurrencyContext)
  if (!ctx) throw new Error('useCurrency must be used within a CurrencyProvider')
  return ctx
}

/** Returns a formatter bound to the currently selected currency and its live rate. */
export function useFormatAmount() {
  const { currency, rates } = useCurrency()
  return useCallback(
    (baseAmount: number) => formatIn(baseAmount, currency, rates[currency]),
    [currency, rates],
  )
}
