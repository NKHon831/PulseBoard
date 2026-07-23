export type CurrencyCode = 'MYR' | 'JPY'

export type CurrencyDef = {
  code: CurrencyCode
  label: string
  locale: string
  // Conversion rate FROM the base currency (MYR) TO this currency. MYR is the
  // base, so its rate is 1. Non-base rates here are approximate offline fallbacks
  // used only until the live rate loads from the exchange-rate API (see fetchRates).
  rate: number
}

// The currency all stored amounts are denominated in.
export const BASE_CURRENCY: CurrencyCode = 'MYR'

export const CURRENCIES: Record<CurrencyCode, CurrencyDef> = {
  MYR: { code: 'MYR', label: 'MYR (RM)', locale: 'en-MY', rate: 1 },
  JPY: { code: 'JPY', label: 'JPY (¥)', locale: 'ja-JP', rate: 34 },
}

export const CURRENCY_CODES = Object.keys(CURRENCIES) as CurrencyCode[]

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

/** Convert an amount from the base currency into `code` at the given rate. */
export function convertAmount(baseAmount: number, rate: number) {
  return baseAmount * rate
}

/**
 * Format a base-currency amount for display in `code`. Pass a live `rate` (from
 * the exchange-rate API); falls back to the static rate when one isn't supplied.
 */
export function formatAmount(baseAmount: number, code: CurrencyCode, rate = CURRENCIES[code].rate) {
  return getFormatter(code).format(convertAmount(baseAmount, rate))
}

/** Format a conversion rate for display, e.g. 39.979 -> "39.98". */
export function formatRate(rate: number) {
  return rate.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const RATES_API_URL = import.meta.env.VITE_EXCHANGE_RATE_API_URL

/**
 * Fetch live conversion rates from the base currency to each of `codes`
 * (base-currency entries resolve to 1 without a request). Returns only the
 * rates the API actually provided.
 */
export async function fetchRates(
  codes: CurrencyCode[],
): Promise<Partial<Record<CurrencyCode, number>>> {
  const symbols = codes.filter((code) => code !== BASE_CURRENCY)
  if (symbols.length === 0) return {}

  const url = `${RATES_API_URL}?base=${BASE_CURRENCY}&symbols=${symbols.join(',')}`
  const res = await fetch(url)
  if (!res.ok) throw new Error(`Exchange rate request failed: ${res.status}`)

  const data = (await res.json()) as { rates?: Record<string, number> }
  const result: Partial<Record<CurrencyCode, number>> = {}
  for (const code of symbols) {
    const rate = data.rates?.[code]
    if (typeof rate === 'number') result[code] = rate
  }
  return result
}
