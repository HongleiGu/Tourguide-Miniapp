import { getIncome, ORDER_STATUS_LABELS } from '../../api/guide'

interface IncomeRow {
  orderId: number
  orderNo: string
  sessionTitle: string
  date: string
  amountYuan: string
  statusLabel: string
}

Page({
  data: {
    orderCount: 0,
    totalYuan: '0',
    items: [] as IncomeRow[],
    loading: true,
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const income = await getIncome()
      this.setData({
        orderCount: income.orderCount,
        totalYuan: (income.totalFen / 100).toFixed(2),
        items: income.items.map((i) => ({
          orderId: i.orderId,
          orderNo: i.orderNo,
          sessionTitle: i.sessionTitle ?? '场次',
          date: i.date,
          amountYuan: (i.amountFen / 100).toFixed(2),
          statusLabel: ORDER_STATUS_LABELS[i.status] ?? i.status,
        })),
        loading: false,
      })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },
})
