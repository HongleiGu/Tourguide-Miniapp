Page({
  data: {
    roles: [] as string[],
  },
  onShow() {
    const app = getApp<IAppOption>()
    this.setData({ roles: app.globalData.roles ?? [] })
  },
})
