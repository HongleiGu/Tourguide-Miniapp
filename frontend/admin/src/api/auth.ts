import http from './http'
import type { ApiResponse, AuthTokens, MeResponse } from './types'

export async function adminLogin(username: string, password: string): Promise<AuthTokens> {
  const res = await http.post<ApiResponse<AuthTokens>>('/auth/admin/login', { username, password })
  return res.data.data as AuthTokens
}

export async function fetchMe(): Promise<MeResponse> {
  const res = await http.get<ApiResponse<MeResponse>>('/auth/me')
  return res.data.data as MeResponse
}
