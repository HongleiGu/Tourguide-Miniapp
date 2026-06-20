/// <reference types="miniprogram-api-typings" />

interface IAppOption {
  globalData: {
    userInfo?: WechatMiniprogram.UserInfo,
    token?: string,
    roles?: string[],
  }
  userInfoReadyCallback?: WechatMiniprogram.GetUserInfoSuccessCallback,
}
