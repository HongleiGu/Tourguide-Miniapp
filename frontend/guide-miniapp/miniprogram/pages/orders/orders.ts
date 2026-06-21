import { getGuideOrders, ORDER_STATUS_LABELS } from '../../api/guide'

interface OrderRow {
  id: number
  orderNo: string
  sessionTitle: string
  date: string
  timeRange: string
  peopleCount: number
  status: string
  statusLabel: string
}

const TABS = [
  { key: 'ALL', label: '全部' },
  { key: 'PAID', label: '待核销' },
  { key: 'COMPLETED', label: '已完成' },
]

Page({
  data: {
    tabs: TABS,
    activeTab: 'ALL',
    list: [] as OrderRow[],
    loading: true,
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const status = this.data.activeTab === 'ALL' ? undefined : this.data.activeTab
      const orders = await getGuideOrders(status)
      const list: OrderRow[] = orders.map((o) => ({
        id: o.id,
        orderNo: o.orderNo,
        sessionTitle: o.sessionTitle ?? '场次',
        date: o.date,
        timeRange: o.startTime && o.endTime ? `${o.startTime}-${o.endTime}` : '',
        peopleCount: o.peopleCount,
        status: o.status,
        statusLabel: ORDER_STATUS_LABELS[o.status] ?? o.status,
      }))
      this.setData({ list, loading: false })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    this.setData({ activeTab: e.currentTarget.dataset.key, loading: true })
    this.load()
  },

  goDetail(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/order/order?id=${e.currentTarget.dataset.id}` })
  },
})
