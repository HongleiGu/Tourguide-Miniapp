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

function storeTokens(tokens: AuthTokens): string {
  wx.setStorageSync(TOKEN_KEY, tokens.accessToken)
  wx.setStorageSync(REFRESH_KEY, tokens.refreshToken)
  return tokens.accessToken
}

/**
 * Silent login: wx.login -> backend -> store tokens. Returns the access token.
 * Dev fallback: if wx-login fails (e.g. no real WX credentials), tries the backend's
 * dev-login (only available under the dev profile) so the app stays testable in DevTools.
 */
export async function login(opts?: { devLoginUrl?: string }): Promise<string> {
  try {
    const code = await wxLoginCode()
    const tokens = await request<AuthTokens>({
      url: '/api/auth/wx-login',
      method: 'POST',
      data: { code },
      auth: false,
    })
    return storeTokens(tokens)
  } catch {
    // Dev fallback (no real WX credentials). The guide app passes /api/auth/dev-login-guide.
    const tokens = await request<AuthTokens>({
      url: opts?.devLoginUrl ?? '/api/auth/dev-login',
      method: 'POST',
      auth: false,
    })
    return storeTokens(tokens)
  }
}

/** Clear stored tokens (sign out). */
export function logout(): void {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(REFRESH_KEY)
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
