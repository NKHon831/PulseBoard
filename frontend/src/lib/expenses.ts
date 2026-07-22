import { useCallback, useEffect, useState } from 'react'
import { apiFetch, ApiError } from './api'

export type ExpenseDto = {
  id: string
  amount: number
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
  const [expenses, setExpenses] = useState<ExpenseDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const reload = useCallback(async () => {
    try {
      const data = await apiFetch<ExpenseDto[]>('/api/expenses')
      setExpenses(data)
      setError('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load expenses')
    } finally {
      setLoading(false)
    }
  }, [])

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
