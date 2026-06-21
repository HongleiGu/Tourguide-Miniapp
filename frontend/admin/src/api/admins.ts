import http from './http'
import type { ApiResponse } from './types'

export interface AdminUser {
  id: number
  username: string | null
  nickname: string | null
  roles: string[]
}

export async function listAdmins(): Promise<AdminUser[]> {
  const res = await http.get<ApiResponse<AdminUser[]>>('/admin/admins')
  return (res.data.data ?? []) as AdminUser[]
}

export async function createAdmin(username: string, password: string, roles: string[]): Promise<AdminUser> {
  const res = await http.post<ApiResponse<AdminUser>>('/admin/admins', { username, password, roles })
  return res.data.data as AdminUser
}

export async function setAdminRoles(id: number, roles: string[]): Promise<AdminUser> {
  const res = await http.post<ApiResponse<AdminUser>>(`/admin/admins/${id}/roles`, { roles })
  return res.data.data as AdminUser
}

export const ROLE_OPTIONS = [
  { value: 'ADMIN_SUPER', label: '超级管理员' },
  { value: 'ADMIN_OPS', label: '运营管理员' },
  { value: 'ADMIN_FINANCE', label: '财务管理员' },
]

export const ROLE_LABELS: Record<string, string> = Object.fromEntries(
  ROLE_OPTIONS.map((o) => [o.value, o.label]),
)
