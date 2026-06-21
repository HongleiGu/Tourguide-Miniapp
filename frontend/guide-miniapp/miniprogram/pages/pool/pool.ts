import { getPool, grabOrder } from '../../api/guide'

interface PoolRow {
  id: number
  sessionTitle: string
  date: string
  timeRange: string
  peopleCount: number
}

Page({
  data: {
    list: [] as PoolRow[],
    loading: true,
    grabbing: 0,
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const orders = await getPool()
      this.setData({
        list: orders.map((o) => ({
          id: o.id,
          sessionTitle: o.sessionTitle ?? '场次',
          date: o.date,
          timeRange: o.startTime && o.endTime ? `${o.startTime}-${o.endTime}` : '',
          peopleCount: o.peopleCount,
        })),
        loading: false,
      })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },

  async grab(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id)
    if (this.data.grabbing) {
      return
    }
    this.setData({ grabbing: id })
    try {
      await grabOrder(id)
      wx.showToast({ title: '抢单成功' })
    } catch (err) {
      wx.showToast({ title: (err as Error).message, icon: 'none' })
    } finally {
      this.setData({ grabbing: 0 })
      this.load()
    }
  },
})
