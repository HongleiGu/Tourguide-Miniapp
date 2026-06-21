import { request } from '../shared/request'

export interface SessionView {
  id: number
  title: string
  type: string
  date: string
  startTime: string
  endTime: string
  capacity: number
  remaining: number
  priceFen: number
  joined: number | null
  groupStatus: string | null
}

export interface AnnouncementView {
  id: number
  title: string
  content: string
  type: string
}

export interface OrderView {
  id: number
  orderNo: string
  type: string
  peopleCount: number
  amountFen: number
  status: string
  verifyCode: string | null
  sessionId: number
  sessionTitle: string
  visitDate: string | null
}

export function getSessions(): Promise<SessionView[]> {
  return request<SessionView[]>({ url: '/api/tourist/sessions', auth: false })
}

export function getAnnouncements(): Promise<AnnouncementView[]> {
  return request<AnnouncementView[]>({ url: '/api/tourist/announcements', auth: false })
}

export function createOrder(data: {
  sessionId: number
  peopleCount: number
  contactName?: string
  contactPhone?: string
  visitDate?: string
}): Promise<OrderView> {
  return request<OrderView>({ url: '/api/tourist/orders', method: 'POST', data })
}

export function mockPay(id: number): Promise<OrderView> {
  return request<OrderView>({ url: `/api/tourist/orders/${id}/mock-pay`, method: 'POST' })
}

export function getOrder(id: number): Promise<OrderView> {
  return request<OrderView>({ url: `/api/tourist/orders/${id}` })
}

export function getMyOrders(): Promise<OrderView[]> {
  return request<OrderView[]>({ url: '/api/tourist/orders' })
}

export function cancelOrder(id: number): Promise<OrderView> {
  return request<OrderView>({ url: `/api/tourist/orders/${id}/cancel`, method: 'POST' })
}

export interface ReviewView {
  orderId: number
  rating: number
  content: string | null
  createdAt: string | null
}

export function submitReview(id: number, data: { rating: number; content?: string }): Promise<ReviewView> {
  return request<ReviewView>({ url: `/api/tourist/orders/${id}/review`, method: 'POST', data })
}

export function getReview(id: number): Promise<ReviewView | null> {
  return request<ReviewView | null>({ url: `/api/tourist/orders/${id}/review` })
}

export const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '待服务',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
}

export const TYPE_LABELS: Record<string, string> = {
  PRIVATE: '私人讲解',
  GROUP: '拼团讲解',
  EXCLUSIVE: '专属时段',
}

export const GROUP_STATUS_LABELS: Record<string, string> = {
  FORMING: '拼团中',
  LOCKED: '已满',
  CONFIRMED: '已成团',
  VOIDED: '已结束',
}
