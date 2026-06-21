import http from './http'
import type { ApiResponse } from './types'

export interface Stats {
  from: string | null
  to: string | null
  totalOrders: number
  paidOrders: number
  completedOrders: number
  visitors: number
  revenueFen: number
  groupFormationRate: number
  verificationRate: number
}

export async function getStats(from?: string, to?: string): Promise<Stats> {
  const res = await http.get<ApiResponse<Stats>>('/admin/stats', { params: { from, to } })
  return res.data.data as Stats
}

export async function exportStats(from?: string, to?: string): Promise<void> {
  const res = await http.get('/admin/stats/export', { params: { from, to }, responseType: 'blob' })
  const url = URL.createObjectURL(res.data as Blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'stats.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}
