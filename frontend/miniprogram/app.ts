// app.ts
import { fetchMe, login } from './utils/auth'

App<IAppOption>({
  globalData: {
    token: '',
    roles: [],
  },
  onLaunch() {
    // Silent WeChat login: wx.login -> backend -> token + roles.
    // The role-aware shell (游客 vs 讲解员 tab group) is built once those pages land
    // (MIN-3 / MIN-6); here we just establish identity and remember the roles.
    login()
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
