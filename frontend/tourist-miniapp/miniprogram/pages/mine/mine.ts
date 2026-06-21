import { bindPhone, fetchMe, login, logout } from '../../shared/auth'
import { TOKEN_KEY } from '../../shared/config'

Page({
  data: {
    loggedIn: false,
    userId: 0,
    type: '',
    roles: [] as string[],
    tokenPreview: '',
    phoneBound: false,
    busy: false,
  },

  onShow() {
    this.refresh()
  },

  refresh() {
    const token = wx.getStorageSync(TOKEN_KEY)
    if (!token) {
      this.setData({ loggedIn: false, userId: 0, type: '', roles: [], tokenPreview: '' })
      return
    }
    this.setData({ tokenPreview: String(token).slice(0, 28) + '…' })
    fetchMe()
      .then((me) => this.setData({ loggedIn: true, userId: me.userId, type: me.type, roles: me.roles }))
      .catch((e: Error) => {
        this.setData({ loggedIn: false })
        wx.showToast({ title: e.message, icon: 'none' })
      })
  },

  async doLogin() {
    if (this.data.busy) {
      return
    }
    this.setData({ busy: true })
    try {
      await login()
      wx.showToast({ title: '登录成功' })
      this.refresh()
    } catch (e) {
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    } finally {
      this.setData({ busy: false })
    }
  },

  goOrders() {
    wx.navigateTo({ url: '/pages/orders/orders' })
  },

  doLogout() {
    logout()
    this.setData({ loggedIn: false, userId: 0, type: '', roles: [], tokenPreview: '', phoneBound: false })
    wx.showToast({ title: '已退出' })
  },

  async onGetPhone(e: { detail: { code?: string; errMsg?: string } }) {
    const code = e.detail.code
    if (!code) {
      wx.showToast({ title: '未获取到手机号（需企业小程序）', icon: 'none' })
      return
    }
    try {
      await bindPhone(code)
      this.setData({ phoneBound: true })
      wx.showToast({ title: '手机号已绑定' })
    } catch (err) {
      wx.showToast({ title: (err as Error).message, icon: 'none' })
    }
  },
})
