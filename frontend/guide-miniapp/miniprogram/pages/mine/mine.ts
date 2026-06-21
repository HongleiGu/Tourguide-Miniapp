import { EMPLOYMENT_LABELS, getGuideMe, GuideMe, setAccepting } from '../../api/guide'
import { logout } from '../../shared/auth'

Page({
  data: {
    me: null as null | (GuideMe & { employmentLabel: string }),
    loading: true,
    saving: false,
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

  async onToggleAccepting(e: WechatMiniprogram.SwitchChange) {
    if (this.data.saving || !this.data.me) {
      return
    }
    this.setData({ saving: true })
    try {
      const me = await setAccepting(e.detail.value)
      this.setData({
        me: { ...me, employmentLabel: EMPLOYMENT_LABELS[me.employmentType] ?? me.employmentType },
      })
      wx.showToast({ title: me.acceptingOrders ? '已开启接单' : '已关闭接单' })
    } catch (err) {
      wx.showToast({ title: (err as Error).message, icon: 'none' })
      this.load()
    } finally {
      this.setData({ saving: false })
    }
  },

  goSchedule() {
    wx.navigateTo({ url: '/pages/schedule/schedule' })
  },

  goIncome() {
    wx.navigateTo({ url: '/pages/income/income' })
  },

  doLogout() {
    logout()
    this.setData({ me: null })
    wx.showToast({ title: '已退出，请重启小程序重新登录' })
  },
})
