import type { components } from '@shared/api'

/** Generic backend envelope ({ code, message, data }). code 0 = success. */
export interface ApiResponse<T> {
  code: number
  message: string
  data?: T
}

// Re-export generated schema types for convenient use across the app.
export type PingResponse = components['schemas']['PingResponse']
export type AuthTokens = components['schemas']['AuthTokens']
export type MeResponse = components['schemas']['MeResponse']
