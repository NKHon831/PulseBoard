export type CurrencyCode = 'MYR' | 'JPY'

export type CurrencyDef = {
  code: CurrencyCode
  label: string
  locale: string
}

// The currency the backend stores every amount in. Kept here only so the UI can
// tell when a displayed rate is meaningful — the frontend never converts.
export const BASE_CURRENCY: CurrencyCode = 'MYR'

// Keep in sync with CurrencyCode in the backend.
export const CURRENCIES: Record<CurrencyCode, CurrencyDef> = {
  MYR: { code: 'MYR', label: 'MYR (RM)', locale: 'en-MY' },
  JPY: { code: 'JPY', label: 'JPY (¥)', locale: 'ja-JP' },
}

export const CURRENCY_CODES = Object.keys(CURRENCIES) as CurrencyCode[]

export function isCurrencyCode(value: unknown): value is CurrencyCode {
  return typeof value === 'string' && value in CURRENCIES
}

const formatters = new Map<CurrencyCode, Intl.NumberFormat>()

function getFormatter(code: CurrencyCode) {
  let formatter = formatters.get(code)
  if (!formatter) {
    const def = CURRENCIES[code]
    formatter = new Intl.NumberFormat(def.locale, { style: 'currency', currency: def.code })
    formatters.set(code, formatter)
  }
  return formatter
}

/**
 * Format an amount that is ALREADY denominated in `code`. The API converts
 * amounts server-side, so this is purely presentational — no rate math here.
 */
export function formatAmount(amount: number, code: CurrencyCode) {
  return getFormatter(code).format(amount)
}

/** Format a conversion rate for display, e.g. 39.979 -> "39.98". */
export function formatRate(rate: number) {
  return rate.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
