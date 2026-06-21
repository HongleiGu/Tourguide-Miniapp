import { getSchedule, SCHEDULE_TYPE_LABELS } from '../../api/guide'

interface ScheduleRow {
  id: number
  date: string
  typeLabel: string
  type: string
  range: string
}

Page({
  data: {
    list: [] as ScheduleRow[],
    loading: true,
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const segments = await getSchedule()
      const list: ScheduleRow[] = segments.map((s) => ({
        id: s.id,
        date: s.date,
        type: s.type,
        typeLabel: SCHEDULE_TYPE_LABELS[s.type] ?? s.type,
        range: s.startTime && s.endTime ? `${s.startTime}-${s.endTime}` : '全天',
      }))
      this.setData({ list, loading: false })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },
})
