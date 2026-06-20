import { API_BASE_URL, TOKEN_KEY } from '../config'

/** Standard backend envelope. */
interface ApiResponse<T> {
  code: number
  message: string
  data?: T
}

/**
 * Promise wrapper over wx.request: prefixes the API base URL, attaches the bearer token,
 * and unwraps the { code, message, data } envelope (rejecting on non-zero code).
 */
export function request<T>(options: {
  url: string
  method?: WechatMiniprogram.RequestOption['method']
  data?: Record<string, unknown>
  auth?: boolean
}): Promise<T> {
  const token = options.auth === false ? '' : wx.getStorageSync(TOKEN_KEY)
  const header: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) {
    header.Authorization = `Bearer ${token}`
  }
  return new Promise<T>((resolve, reject) => {
    wx.request({
      url: API_BASE_URL + options.url,
      method: options.method ?? 'GET',
      data: options.data,
      header,
      success: (res) => {
        const body = res.data as ApiResponse<T>
        if (res.statusCode >= 200 && res.statusCode < 300 && body && body.code === 0) {
          resolve(body.data as T)
        } else {
          reject(new Error(body?.message || `请求失败 (${res.statusCode})`))
        }
      },
      fail: (err) => reject(new Error(err.errMsg || '网络错误')),
    })
  })
}
