package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.web.http.request.CacheControl;
import org.jmouse.web.mvc.BeanConfigurer;
import org.jmouse.web.mvc.resource.*;

@Bean
public class ResourcesConfig implements BeanConfigurer<ResourceHandlerRegistry> {

    @Override
    public void configure(ResourceHandlerRegistry registry) {
        VersionalResourceResolver resolver = new VersionalResourceResolver()
                .addStrategy(new FixedVersionStrategy("v2025.09"), "/static/**")
                .addStrategy(new ContentHashVersionStrategy("SHA-256", 16), "/assets/**");

        registry.registerHandler("/assets/{*filepath}")
                .addResourceLocations("classpath:static/", "file:/C:/Users/Ivan_Hontarenko/Git/")
                .setCacheControl(CacheControl.empty().cachePublic())
                .getChainRegistration()
                .addResolvers(new PathNormalizationResolver(), resolver, new LocationScanningResolver());

        registry.registerHandler("/static/**")
                .addResourceLocations("classpath:HTML/")
                .setCachePeriod(3344411)
                .getChainRegistration()
                .addResolvers(new LocationScanningResolver());
    }

}
