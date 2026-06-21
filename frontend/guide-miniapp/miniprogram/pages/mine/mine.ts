import { EMPLOYMENT_LABELS, getGuideMe, GuideMe } from '../../api/guide'
import { logout } from '../../shared/auth'

Page({
  data: {
    me: null as null | (GuideMe & { employmentLabel: string }),
    loading: true,
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const me = await getGuideMe()
      this.setData({
        me: { ...me, employmentLabel: EMPLOYMENT_LABELS[me.employmentType] ?? me.employmentType },
        loading: false,
      })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },

  doLogout() {
    logout()
    this.setData({ me: null })
    wx.showToast({ title: '已退出，请重启小程序重新登录' })
  },
})
