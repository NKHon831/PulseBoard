import { useCallback, useEffect, useRef, useState } from 'react'
import { apiFetch, ApiError } from './api'
import { useCurrency } from './currency'
import type { CurrencyCode } from './currencies'
import { todayStr } from './format'

// Keep in sync with CATEGORY_PATTERN in the backend's ExpenseRequest.
export const CATEGORIES = ['Breakfast', 'Lunch', 'Dinner', 'Others']

export type ExpenseDto = {
  id: string
  /** Already converted by the backend into `currency`. */
  amount: number
  currency: CurrencyCode
  category: string
  description: string | null
  expenseDate: string
  /** Written in by a fixed expense rather than typed by the user. */
  recurring: boolean
}

export type DayGroup = {
  date: string
  total: number
  items: ExpenseDto[]
}

export function useExpenses() {
  const { currency } = useCurrency()
  const [expenses, setExpenses] = useState<ExpenseDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  // Guards against a slow response for a previously selected currency landing
  // after a faster one and overwriting it.
  const requestId = useRef(0)

  const reload = useCallback(async () => {
    const id = ++requestId.current
    try {
      // The backend fills in any fixed expenses owed up to this date first. It
      // has to come from here: only the browser knows the user's local day.
      const data = await apiFetch<ExpenseDto[]>(
        `/api/expenses?currency=${currency}&today=${todayStr()}`,
      )
      if (id !== requestId.current) return
      setExpenses(data)
      setError('')
    } catch (err) {
      if (id !== requestId.current) return
      setError(err instanceof ApiError ? err.message : 'Failed to load expenses')
    } finally {
      if (id === requestId.current) setLoading(false)
    }
  }, [currency])

  useEffect(() => {
    reload()
  }, [reload])

  return { expenses, setExpenses, loading, error, setError, reload }
}

export function groupByDay(expenses: ExpenseDto[]): DayGroup[] {
  const order: string[] = []
  const byDate = new Map<string, ExpenseDto[]>()

  for (const expense of expenses) {
    if (!byDate.has(expense.expenseDate)) {
      order.push(expense.expenseDate)
      byDate.set(expense.expenseDate, [])
    }
    byDate.get(expense.expenseDate)!.push(expense)
  }

  return order.map((date) => {
    const items = byDate.get(date)!
    return { date, items, total: items.reduce((sum, e) => sum + e.amount, 0) }
  })
}
