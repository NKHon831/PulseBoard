import { useCallback, useEffect, useRef, useState } from 'react'
import { apiFetch, ApiError } from './api'
import { useCurrency } from './currency'
import type { CurrencyCode } from './currencies'

// Keep in sync with RecurrenceFrequency in the backend.
export type Frequency = 'DAILY' | 'WEEKDAYS' | 'WEEKENDS'

export const FREQUENCIES: { value: Frequency; label: string; hint: string }[] = [
  { value: 'DAILY', label: 'Every day', hint: 'Monday to Sunday' },
  { value: 'WEEKDAYS', label: 'Weekdays only', hint: 'Monday to Friday' },
  { value: 'WEEKENDS', label: 'Weekends only', hint: 'Saturday and Sunday' },
]

export function frequencyLabel(frequency: Frequency) {
  return FREQUENCIES.find((f) => f.value === frequency)?.label ?? frequency
}

export type RecurringExpenseDto = {
  id: string
  /** Already converted by the backend into `currency`. */
  amount: number
  currency: CurrencyCode
  category: string
  description: string | null
  frequency: Frequency
  active: boolean
  startDate: string
  lastGeneratedDate: string | null
}

export function useRecurringExpenses() {
  const { currency } = useCurrency()
  const [recurring, setRecurring] = useState<RecurringExpenseDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  // Guards against a slow response for a previously selected currency landing
  // after a faster one and overwriting it.
  const requestId = useRef(0)

  const reload = useCallback(async () => {
    const id = ++requestId.current
    try {
      const data = await apiFetch<RecurringExpenseDto[]>(`/api/recurring-expenses?currency=${currency}`)
      if (id !== requestId.current) return
      setRecurring(data)
      setError('')
    } catch (err) {
      if (id !== requestId.current) return
      setError(err instanceof ApiError ? err.message : 'Failed to load fixed expenses')
    } finally {
      if (id === requestId.current) setLoading(false)
    }
  }, [currency])

  useEffect(() => {
    reload()
  }, [reload])

  return { recurring, setRecurring, loading, error, setError, reload }
}
