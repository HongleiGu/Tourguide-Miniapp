import http from './http'
import type { ApiResponse } from './types'

export interface AdminOrder {
  id: number
  orderNo: string
  type: string
  status: string
  peopleCount: number
  amountFen: number
  visitDate: string | null
  guideId: number | null
  userId: number | null
  sessionTitle: string | null
  contactName: string | null
  contactPhone: string | null
}

export interface OrderFilters {
  status?: string
  type?: string
  guideId?: number
  from?: string
  to?: string
}

export async function listOrders(filters: OrderFilters): Promise<AdminOrder[]> {
  const res = await http.get<ApiResponse<AdminOrder[]>>('/admin/orders', { params: filters })
  return (res.data.data ?? []) as AdminOrder[]
}

export async function handleOrder(id: number, action: string, reason: string): Promise<AdminOrder> {
  const res = await http.post<ApiResponse<AdminOrder>>(`/admin/orders/${id}/handle`, { action, reason })
  return res.data.data as AdminOrder
}

export async function exportOrders(filters: OrderFilters): Promise<void> {
  const res = await http.get('/admin/orders/export', { params: filters, responseType: 'blob' })
  const url = URL.createObjectURL(res.data as Blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'orders.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}

export const ORDER_STATUS_OPTIONS = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'PAID', label: '待核销' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REFUNDED', label: '已退款' },
]

export const ORDER_STATUS_LABELS: Record<string, string> = Object.fromEntries(
  ORDER_STATUS_OPTIONS.map((o) => [o.value, o.label]),
)

export const ORDER_TYPE_OPTIONS = [
  { value: 'PRIVATE', label: '私讲' },
  { value: 'GROUP', label: '拼团' },
  { value: 'EXCLUSIVE', label: '专属时段' },
]

export const ORDER_TYPE_LABELS: Record<string, string> = Object.fromEntries(
  ORDER_TYPE_OPTIONS.map((o) => [o.value, o.label]),
)
