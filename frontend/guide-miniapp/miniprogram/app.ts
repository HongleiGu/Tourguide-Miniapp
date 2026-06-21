// app.ts — 讲解员 (guide) mini program
import { fetchMe, login } from './shared/auth'

App<IAppOption>({
  globalData: {
    token: '',
    roles: [],
  },
  onLaunch() {
    // Silent WeChat login: wx.login -> backend -> token + roles. Dev fallback issues a GUIDE token.
    login({ devLoginUrl: '/api/auth/dev-login-guide' })
      .then((token) => {
        this.globalData.token = token
        return fetchMe()
      })
      .then((me) => {
        this.globalData.roles = me.roles
      })
      .catch((err: Error) => {
        console.warn('wx login skipped/failed (expected without real WX credentials):', err.message)
      })
  },
})
