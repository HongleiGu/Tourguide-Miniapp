import http from './http'
import type { ApiResponse } from './types'

export interface AdminGuide {
  guideId: number
  name: string | null
  employmentType: string
  status: string
  acceptingOrders: boolean
  dispatchWeight: number
  rating: number
  starLevel: number
}

export async function listGuides(): Promise<AdminGuide[]> {
  const res = await http.get<ApiResponse<AdminGuide[]>>('/admin/guides')
  return (res.data.data ?? []) as AdminGuide[]
}

export async function createGuide(name: string, employmentType: string): Promise<AdminGuide> {
  const res = await http.post<ApiResponse<AdminGuide>>('/admin/guides', { name, employmentType })
  return res.data.data as AdminGuide
}

export async function setGuideEnabled(id: number, enabled: boolean): Promise<AdminGuide> {
  const res = await http.post<ApiResponse<AdminGuide>>(`/admin/guides/${id}/enabled`, { enabled })
  return res.data.data as AdminGuide
}

export async function setGuideEmployment(id: number, employmentType: string): Promise<AdminGuide> {
  const res = await http.post<ApiResponse<AdminGuide>>(`/admin/guides/${id}/employment`, { employmentType })
  return res.data.data as AdminGuide
}

export async function setGuideWeight(id: number, weight: number): Promise<AdminGuide> {
  const res = await http.post<ApiResponse<AdminGuide>>(`/admin/guides/${id}/dispatch-weight`, { weight })
  return res.data.data as AdminGuide
}

export async function setGuideSuspended(id: number, suspended: boolean): Promise<AdminGuide> {
  const res = await http.post<ApiResponse<AdminGuide>>(`/admin/guides/${id}/suspend`, { suspended })
  return res.data.data as AdminGuide
}

export const EMPLOYMENT_OPTIONS = [
  { value: 'SELF', label: '自营' },
  { value: 'OUTSOURCED', label: '外包' },
]

export const GUIDE_STATUS_LABELS: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '禁用',
  SUSPENDED: '暂停',
}
