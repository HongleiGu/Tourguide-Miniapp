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

export interface GuideOrderView {
  id: number
  orderNo: string
  sessionTitle: string | null
  type: string
  date: string
  startTime: string | null
  endTime: string | null
  peopleCount: number
  status: string
  contactName: string | null
  contactPhone: string | null
}

export function getGuideOrders(status?: string): Promise<GuideOrderView[]> {
  return request<GuideOrderView[]>({ url: '/api/guide/orders', data: status ? { status } : {} })
}

export function getGuideOrder(id: number): Promise<GuideOrderView> {
  return request<GuideOrderView>({ url: `/api/guide/orders/${id}` })
}

export const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '待核销',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
}

export const EMPLOYMENT_LABELS: Record<string, string> = {
  SELF: '自营',
  OUTSOURCED: '外包',
}
