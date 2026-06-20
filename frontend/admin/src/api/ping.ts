import http from './http'
import type { ApiResponse, PingResponse } from './types'

/** Calls GET /api/ping and returns the typed payload (demonstrates the shared API types). */
export async function getPing(): Promise<PingResponse> {
  const res = await http.get<ApiResponse<PingResponse>>('/ping')
  return res.data.data as PingResponse
}
