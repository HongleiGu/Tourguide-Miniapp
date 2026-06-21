import { cancelOrder, getOrder, mockPay } from '../../api/tourist'

const STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
}

Page({
  data: {
    id: 0,
    order: null as null | {
      orderNo: string
      sessionTitle: string
      peopleCount: number
      amountYuan: string
      status: string
      statusLabel: string
      verifyCode: string | null
      cancellable: boolean
    },
    paying: false,
    cancelling: false,
  },

  onLoad(query: Record<string, string | undefined>) {
    this.setData({ id: Number(query.id ?? 0) })
    this.load()
  },

  async load() {
    try {
      const o = await getOrder(this.data.id)
      this.setData({
        order: {
          orderNo: o.orderNo,
          sessionTitle: o.sessionTitle,
          peopleCount: o.peopleCount,
          amountYuan: (o.amountFen / 100).toFixed(0),
          status: o.status,
          statusLabel: STATUS_LABELS[o.status] ?? o.status,
          verifyCode: o.verifyCode,
          cancellable: o.status === 'PENDING_PAYMENT' || o.status === 'PAID',
        },
      })
    } catch (e) {
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },

  async pay() {
    if (this.data.paying) {
      return
    }
    this.setData({ paying: true })
    try {
      await mockPay(this.data.id)
      wx.showToast({ title: '支付成功' })
      await this.load()
    } catch (e) {
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    } finally {
      this.setData({ paying: false })
    }
  },

  async cancel() {
    if (this.data.cancelling) {
      return
    }
    try {
      await wx.showModal({ title: '取消订单', content: '确定取消该订单？' }).then((r) => {
        if (!r.confirm) {
          throw new Error('__abort__')
        }
      })
    } catch {
      return
    }
    this.setData({ cancelling: true })
    try {
      await cancelOrder(this.data.id)
      wx.showToast({ title: '已取消' })
      await this.load()
    } catch (e) {
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    } finally {
      this.setData({ cancelling: false })
    }
  },

  goHome() {
    wx.reLaunch({ url: '/pages/index/index' })
  },
})
