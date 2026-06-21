import http from './http'
import type { ApiResponse } from './types'

export interface PricingRule {
  id: number
  sessionType: string
  dayType: string
  priceFen: number
  groupMin: number | null
  groupMax: number | null
}

export interface PricingRuleInput {
  sessionType: string
  dayType: string
  priceFen: number
  groupMin?: number | null
  groupMax?: number | null
}

export async function listPricing(): Promise<PricingRule[]> {
  const res = await http.get<ApiResponse<PricingRule[]>>('/admin/pricing')
  return (res.data.data ?? []) as PricingRule[]
}

export async function upsertPricing(input: PricingRuleInput): Promise<PricingRule> {
  const res = await http.put<ApiResponse<PricingRule>>('/admin/pricing', input)
  return res.data.data as PricingRule
}

export const TYPE_LABELS: Record<string, string> = {
  PRIVATE: '私讲',
  GROUP: '拼团',
  EXCLUSIVE: '专属时段',
}

export const DAY_LABELS: Record<string, string> = {
  WORKDAY: '工作日',
  HOLIDAY: '节假日',
}
