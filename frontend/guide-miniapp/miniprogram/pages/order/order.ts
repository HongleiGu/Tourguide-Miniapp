import { getGuideOrder, GuideOrderView, ORDER_STATUS_LABELS } from '../../api/guide'

Page({
  data: {
    id: 0,
    order: null as null | (GuideOrderView & { statusLabel: string; timeRange: string }),
    loading: true,
  },

  onLoad(query: Record<string, string | undefined>) {
    this.setData({ id: Number(query.id ?? 0) })
    this.load()
  },

  async load() {
    try {
      const o = await getGuideOrder(this.data.id)
      this.setData({
        order: {
          ...o,
          statusLabel: ORDER_STATUS_LABELS[o.status] ?? o.status,
          timeRange: o.startTime && o.endTime ? `${o.startTime}-${o.endTime}` : '',
        },
        loading: false,
      })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },
})
