import { request } from '../shared/request'

export interface GuideMe {
  guideId: number
  name: string | null
  employmentType: string
  acceptingOrders: boolean
  status: string
  rating: number
  starLevel: number
}

export function getGuideMe(): Promise<GuideMe> {
  return request<GuideMe>({ url: '/api/guide/me' })
}

export interface ScheduleSegment {
  id: number
  date: string
  type: string
  startTime: string | null
  endTime: string | null
}

export interface GuideWorkbench {
  date: string
  accepting: boolean
  onDutyToday: boolean
  pendingAcceptCount: number
  toVerifyCount: number
  completedCount: number
  remainingCapacity: number
  schedule: ScheduleSegment[]
}

export function getWorkbench(): Promise<GuideWorkbench> {
  return request<GuideWorkbench>({ url: '/api/guide/workbench' })
}

export const SCHEDULE_TYPE_LABELS: Record<string, string> = {
  WORK: '上班',
  REST: '休息',
}

export const EMPLOYMENT_LABELS: Record<string, string> = {
  SELF: '自营',
  OUTSOURCED: '外包',
}
