import { getAnnouncements, getSessions, GROUP_STATUS_LABELS, TYPE_LABELS } from '../../api/tourist'

Page({
  data: {
    sessions: [] as Array<{
      id: number
      title: string
      typeLabel: string
      date: string
      startTime: string
      endTime: string
      remaining: number
      capacity: number
      priceYuan: string
      priceFen: number
      type: string
      isGroup: boolean
      joined: number
      groupLabel: string
    }>,
    announcements: [] as Array<{ id: number; title: string; content: string }>,
    loading: true,
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const [sessions, announcements] = await Promise.all([getSessions(), getAnnouncements()])
      this.setData({
        sessions: sessions.map((s) => ({
          id: s.id,
          title: s.title,
          type: s.type,
          typeLabel: TYPE_LABELS[s.type] ?? s.type,
          date: s.date,
          startTime: s.startTime,
          endTime: s.endTime,
          remaining: s.remaining,
          capacity: s.capacity,
          priceFen: s.priceFen,
          priceYuan: (s.priceFen / 100).toFixed(0),
          isGroup: s.type === 'GROUP',
          joined: s.joined ?? 0,
          groupLabel: s.groupStatus ? GROUP_STATUS_LABELS[s.groupStatus] ?? s.groupStatus : '',
        })),
        announcements: announcements.map((a) => ({ id: a.id, title: a.title, content: a.content })),
        loading: false,
      })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },

  goMine() {
    wx.navigateTo({ url: '/pages/mine/mine' })
  },

  goBooking(e: WechatMiniprogram.TouchEvent) {
    const { id, title, price, type, remaining, status } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/booking/booking?sessionId=${id}&title=${encodeURIComponent(title)}&priceFen=${price}&type=${type}&remaining=${remaining}&groupStatus=${status ?? ''}`,
    })
  },
})
