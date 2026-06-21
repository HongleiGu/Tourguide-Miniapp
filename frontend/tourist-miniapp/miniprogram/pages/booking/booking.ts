import { createOrder, TYPE_LABELS } from '../../api/tourist'

Page({
  data: {
    sessionId: 0,
    title: '',
    type: '',
    typeLabel: '',
    priceFen: 0,
    peopleCount: 1,
    contactName: '',
    contactPhone: '',
    totalYuan: '0',
    remaining: 0,
    isGroup: false,
    groupStatus: '',
    canBook: true,
    submitting: false,
  },

  onLoad(query: Record<string, string | undefined>) {
    const priceFen = Number(query.priceFen ?? 0)
    const type = query.type ?? ''
    const remaining = Number(query.remaining ?? 0)
    const groupStatus = query.groupStatus ?? ''
    this.setData({
      sessionId: Number(query.sessionId ?? 0),
      title: decodeURIComponent(query.title ?? ''),
      type,
      typeLabel: TYPE_LABELS[type] ?? type,
      priceFen,
      remaining,
      isGroup: type === 'GROUP',
      groupStatus,
      canBook: remaining > 0,
      totalYuan: (priceFen / 100).toFixed(0),
    })
  },

  onPeople(e: WechatMiniprogram.Input) {
    const peopleCount = Math.max(1, Number(e.detail.value) || 1)
    this.setData({ peopleCount, totalYuan: ((this.data.priceFen * peopleCount) / 100).toFixed(0) })
  },

  onName(e: WechatMiniprogram.Input) {
    this.setData({ contactName: e.detail.value })
  },

  onPhone(e: WechatMiniprogram.Input) {
    this.setData({ contactPhone: e.detail.value })
  },

  async submit() {
    if (this.data.submitting || !this.data.canBook) {
      return
    }
    this.setData({ submitting: true })
    try {
      const order = await createOrder({
        sessionId: this.data.sessionId,
        peopleCount: this.data.peopleCount,
        contactName: this.data.contactName,
        contactPhone: this.data.contactPhone,
      })
      wx.redirectTo({ url: `/pages/order/order?id=${order.id}` })
    } catch (e) {
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
