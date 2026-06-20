// app.ts — 讲解员 (guide) mini program
import { fetchMe, login } from './shared/auth'

App<IAppOption>({
  globalData: {
    token: '',
    roles: [],
  },
  onLaunch() {
    // Silent WeChat login: wx.login -> backend -> token + roles. Guide features land in MIN-6.
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
