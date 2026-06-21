import { cancelOrder, getOrder, getReview, getVerifyQr, mockPay, submitReview } from '../../api/tourist'

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
    qrDataUrl: '',
    review: null as null | { rating: number; content: string | null; createdAt: string | null },
    ratingInput: 5,
    contentInput: '',
    stars: [1, 2, 3, 4, 5],
    submittingReview: false,
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
      if (o.status === 'PAID' && o.verifyCode) {
        const qr = await getVerifyQr(this.data.id)
        this.setData({ qrDataUrl: qr.dataUrl })
      }
      if (o.status === 'COMPLETED') {
        const review = await getReview(this.data.id)
        this.setData({ review })
      }
    } catch (e) {
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },

  setRating(e: WechatMiniprogram.TouchEvent) {
    this.setData({ ratingInput: Number(e.currentTarget.dataset.n) })
  },

  onReviewContent(e: WechatMiniprogram.Input) {
    this.setData({ contentInput: e.detail.value })
  },

  async submitReviewTap() {
    if (this.data.submittingReview) {
      return
    }
    this.setData({ submittingReview: true })
    try {
      const review = await submitReview(this.data.id, {
        rating: this.data.ratingInput,
        content: this.data.contentInput,
      })
      this.setData({ review })
      wx.showToast({ title: '评价成功' })
    } catch (e) {
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    } finally {
      this.setData({ submittingReview: false })
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
