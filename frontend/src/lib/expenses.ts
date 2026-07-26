import { useCallback, useEffect, useRef, useState } from 'react'
import { apiFetch, ApiError } from './api'
import { useCurrency } from './currency'
import type { CurrencyCode } from './currencies'

export type ExpenseDto = {
  id: string
  /** Already converted by the backend into `currency`. */
  amount: number
  currency: CurrencyCode
  category: string
  description: string | null
  expenseDate: string
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
      const data = await apiFetch<ExpenseDto[]>(`/api/expenses?currency=${currency}`)
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
