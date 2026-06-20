import { REFRESH_KEY, TOKEN_KEY } from './config'
import { request } from './request'

interface AuthTokens {
  accessToken: string
  refreshToken: string
}

interface MeResponse {
  userId: number
  type: string
  roles: string[]
}

/** Run wx.login and return the short-lived code. */
function wxLoginCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => (res.code ? resolve(res.code) : reject(new Error('wx.login 无 code'))),
      fail: (err) => reject(new Error(err.errMsg)),
    })
  })
}

/** Silent login: wx.login -> backend -> store tokens. Returns the access token. */
export async function login(): Promise<string> {
  const code = await wxLoginCode()
  const tokens = await request<AuthTokens>({
    url: '/api/auth/wx-login',
    method: 'POST',
    data: { code },
    auth: false,
  })
  wx.setStorageSync(TOKEN_KEY, tokens.accessToken)
  wx.setStorageSync(REFRESH_KEY, tokens.refreshToken)
  return tokens.accessToken
}

/** Bind the WeChat-verified phone using the code from the getPhoneNumber button. */
export function bindPhone(code: string): Promise<void> {
  return request<void>({ url: '/api/auth/wx-phone', method: 'POST', data: { code } })
}

export function fetchMe(): Promise<MeResponse> {
  return request<MeResponse>({ url: '/api/auth/me' })
}

export function isGuide(roles: string[]): boolean {
  return roles.includes('GUIDE')
}

export function isTourist(roles: string[]): boolean {
  return roles.includes('TOURIST')
}
