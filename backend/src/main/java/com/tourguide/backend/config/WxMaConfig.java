package com.tourguide.backend.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** WeChat Mini Program API client (WxJava), configured from app.wechat.miniapp.* (env). */
@Slf4j
@Configuration
public class WxMaConfig {

    @Bean
    public WxMaService wxMaService(AppProperties props) {
        String appId = props.wechat().miniapp().appId();
        boolean configured = StringUtils.hasText(appId) && StringUtils.hasText(props.wechat().miniapp().appSecret());
        log.info("WeChat mini-program credentials: {} (appId {})",
                configured ? "present" : "absent — real wx-login disabled",
                StringUtils.hasText(appId) ? appId : "none");

        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(appId);
        config.setSecret(props.wechat().miniapp().appSecret());
        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
