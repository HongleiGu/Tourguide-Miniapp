package com.tourguide.backend.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** WeChat Mini Program API client (WxJava), configured from app.wechat.miniapp.* (env). */
@Configuration
public class WxMaConfig {

    @Bean
    public WxMaService wxMaService(AppProperties props) {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(props.wechat().miniapp().appId());
        config.setSecret(props.wechat().miniapp().appSecret());
        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
