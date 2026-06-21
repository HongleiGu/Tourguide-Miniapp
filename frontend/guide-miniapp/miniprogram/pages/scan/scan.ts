import { verifyCode, VerifyResult } from '../../api/guide'

Page({
  data: {
    code: '',
    busy: false,
    result: null as null | VerifyResult,
    error: '',
  },

  scan() {
    wx.scanCode({
      onlyFromCamera: false,
      success: (res) => {
        this.setData({ code: res.result })
        this.doVerify(res.result)
      },
      fail: () => {
        /* user cancelled */
      },
    })
  },

  onCode(e: WechatMiniprogram.Input) {
    this.setData({ code: e.detail.value })
  },

  submit() {
    const code = this.data.code.trim()
    if (code) {
      this.doVerify(code)
    }
  },

  async doVerify(code: string) {
    if (this.data.busy) {
      return
    }
    this.setData({ busy: true, error: '', result: null })
    try {
      const result = await verifyCode(code)
      this.setData({ result })
      wx.showToast({ title: '核销成功' })
    } catch (e) {
      this.setData({ error: (e as Error).message })
      wx.showToast({ title: (e as Error).message, icon: 'none' })
    } finally {
      this.setData({ busy: false })
    }
  },
})
