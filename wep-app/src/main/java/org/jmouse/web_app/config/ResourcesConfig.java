package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.web.http.request.CacheControl;
import org.jmouse.web.mvc.BeanConfigurer;
import org.jmouse.web.mvc.resource.FixedVersionStrategy;
import org.jmouse.web.mvc.resource.ResourceHandlerRegistry;
import org.jmouse.web.mvc.resource.VersionalResourceResolver;

@Bean
public class ResourcesConfig implements BeanConfigurer<ResourceHandlerRegistry> {

    @Override
    public void configure(ResourceHandlerRegistry registry) {
        registry.registerHandler("/assets/{filepath:.+}")
                .addResourceLocations("classpath:static/", "file:/C:/Users/Ivan_Hontarenko/Git/")
                .setCacheControl(CacheControl.empty().cachePublic())
                .getChainRegistration()
                .addResolvers(new VersionalResourceResolver().addStrategy(
                        new FixedVersionStrategy("v1"), "/**"
                ));
        registry.registerHandler("/static/**")
                .addResourceLocations("classpath:HTML/");
    }

}
