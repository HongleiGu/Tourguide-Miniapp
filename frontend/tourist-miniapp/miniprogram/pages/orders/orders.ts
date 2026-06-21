import { getMyOrders, ORDER_STATUS_LABELS } from '../../api/tourist'

interface OrderRow {
  id: number
  orderNo: string
  sessionTitle: string
  status: string
  statusLabel: string
  amountYuan: string
  peopleCount: number
  visitDate: string
}

const TABS = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING_PAYMENT', label: '待支付' },
  { key: 'PAID', label: '待服务' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
]

Page({
  data: {
    tabs: TABS,
    activeTab: 'ALL',
    all: [] as OrderRow[],
    list: [] as OrderRow[],
    loading: true,
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const orders = await getMyOrders()
      const all: OrderRow[] = orders.map((o) => ({
        id: o.id,
        orderNo: o.orderNo,
        sessionTitle: o.sessionTitle,
        status: o.status,
        statusLabel: ORDER_STATUS_LABELS[o.status] ?? o.status,
        amountYuan: (o.amountFen / 100).toFixed(0),
        peopleCount: o.peopleCount,
        visitDate: o.visitDate ?? '',
      }))
      this.setData({ all, loading: false })
      this.applyFilter()
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    this.setData({ activeTab: e.currentTarget.dataset.key })
    this.applyFilter()
  },

  applyFilter() {
    const t = this.data.activeTab
    this.setData({ list: t === 'ALL' ? this.data.all : this.data.all.filter((o) => o.status === t) })
  },

  goOrder(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/order/order?id=${e.currentTarget.dataset.id}` })
  },
})
