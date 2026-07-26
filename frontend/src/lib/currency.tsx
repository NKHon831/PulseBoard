import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { apiFetch } from './api'
import {
  BASE_CURRENCY,
  formatAmount as formatIn,
  isCurrencyCode,
  type CurrencyCode,
} from './currencies'

const CURRENCY_KEY = 'pulseboard.currency'

/** Rates are display-only: the backend converts every amount it returns. */
type Rates = Partial<Record<CurrencyCode, number>>

type RatesResponse = { base: string; rates: Record<string, number> }

function readStoredCurrency(): CurrencyCode {
  const saved = localStorage.getItem(CURRENCY_KEY)
  return isCurrencyCode(saved) ? saved : BASE_CURRENCY
}

type CurrencyContextValue = {
  currency: CurrencyCode
  setCurrency: (code: CurrencyCode) => void
  rates: Rates
}

const CurrencyContext = createContext<CurrencyContextValue | undefined>(undefined)

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const [currency, setCurrencyState] = useState<CurrencyCode>(readStoredCurrency)
  const [rates, setRates] = useState<Rates>({ [BASE_CURRENCY]: 1 })

  const setCurrency = useCallback((code: CurrencyCode) => {
    setCurrencyState(code)
    localStorage.setItem(CURRENCY_KEY, code)
  }, [])

  // Fetch the rates the backend is converting with, purely so the nav can show
  // them. If this fails the app still works — amounts arrive pre-converted.
  useEffect(() => {
    let cancelled = false
    apiFetch<RatesResponse>('/api/currency/rates')
      .then((data) => {
        if (cancelled) return
        const next: Rates = {}
        for (const [code, rate] of Object.entries(data.rates)) {
          if (isCurrencyCode(code) && typeof rate === 'number') next[code] = rate
        }
        setRates(next)
      })
      .catch(() => {
        // Rate display is optional; leave it hidden.
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

/**
 * Returns a formatter for amounts the API already converted into the selected
 * currency.
 */
export function useFormatAmount() {
  const { currency } = useCurrency()
  return useCallback((amount: number) => formatIn(amount, currency), [currency])
}
