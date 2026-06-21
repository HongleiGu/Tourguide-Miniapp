import { getWorkbench, GuideWorkbench, SCHEDULE_TYPE_LABELS } from '../../api/guide'

interface ScheduleRow {
  id: number
  typeLabel: string
  range: string
}

Page({
  data: {
    board: null as null | GuideWorkbench,
    schedule: [] as ScheduleRow[],
    loading: true,
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const board = await getWorkbench()
      const schedule: ScheduleRow[] = board.schedule.map((s) => ({
        id: s.id,
        typeLabel: SCHEDULE_TYPE_LABELS[s.type] ?? s.type,
        range: s.startTime && s.endTime ? `${s.startTime}-${s.endTime}` : '全天',
      }))
      this.setData({ board, schedule, loading: false })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    }
  },
})
