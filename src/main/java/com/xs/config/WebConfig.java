package com.xs.config;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author xueshuai
 * @date 2020/9/10 10:42
 * @description 资源配置：
 *          1.跨域（注解--》配置）
 *          2.资源设置
 *
 */
public class WebConfig extends WebMvcConfigurerAdapter {

    //跨域处理
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }
    /**
     * //boot中swagger 静态资源设置 xueshuai
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");

        registry.addResourceHandler("/**").addResourceLocations("classpath:/resources/temp");
    }
}
