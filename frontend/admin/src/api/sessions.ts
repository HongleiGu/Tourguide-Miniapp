import http from './http'
import type { ApiResponse } from './types'

export interface AdminSession {
  id: number
  title: string
  type: string
  date: string
  startTime: string | null
  endTime: string | null
  capacity: number
  priceFen: number
  guideId: number | null
  status: string
  groupMin: number | null
  groupMax: number | null
  groupCurrent: number | null
}

export interface SessionInput {
  title: string
  type: string
  date: string
  startTime?: string
  endTime?: string
  capacity?: number | null
  priceFen: number
  guideId?: number | null
  groupMinSize?: number | null
  groupMaxSize?: number | null
}

export async function listSessions(date?: string): Promise<AdminSession[]> {
  const res = await http.get<ApiResponse<AdminSession[]>>('/admin/sessions', {
    params: date ? { date } : {},
  })
  return (res.data.data ?? []) as AdminSession[]
}

export async function createSession(input: SessionInput): Promise<AdminSession> {
  const res = await http.post<ApiResponse<AdminSession>>('/admin/sessions', input)
  return res.data.data as AdminSession
}

export async function updateSession(id: number, input: SessionInput): Promise<AdminSession> {
  const res = await http.put<ApiResponse<AdminSession>>(`/admin/sessions/${id}`, input)
  return res.data.data as AdminSession
}

export async function setSessionStatus(id: number, status: string): Promise<AdminSession> {
  const res = await http.post<ApiResponse<AdminSession>>(`/admin/sessions/${id}/status`, { status })
  return res.data.data as AdminSession
}

export const SESSION_TYPE_OPTIONS = [
  { value: 'PRIVATE', label: '私讲' },
  { value: 'GROUP', label: '拼团' },
  { value: 'EXCLUSIVE', label: '专属时段' },
]

export const SESSION_TYPE_LABELS: Record<string, string> = {
  PRIVATE: '私讲',
  GROUP: '拼团',
  EXCLUSIVE: '专属时段',
}

export const SESSION_STATUS_LABELS: Record<string, string> = {
  OPEN: '开放',
  LOCKED: '锁场',
  CLOSED: '停场',
  CANCELLED: '已取消',
}
