package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.web.http.request.CacheControl;
import org.jmouse.web.mvc.BeanConfigurer;
import org.jmouse.web.mvc.resource.*;

@Bean
public class ResourcesConfig implements BeanConfigurer<ResourceHandlerRegistry> {

    private final ResourceVersionProperties properties;

    @BeanConstructor
    public ResourcesConfig(ResourceVersionProperties properties) {
        this.properties = properties;
    }

    @Override
    public void configure(ResourceHandlerRegistry registry) {
        VersionalResourceResolver resolver = new VersionalResourceResolver()
                .addStrategy(new FixedVersionStrategy("v2025.09"), "/static/**")
                .addStrategy(new FixedVersionStrategy(properties.getIconPrefix()), "/icon/**")
                .addStrategy(new ContentHashVersionStrategy("SHA-256", 16), "/assets/**");

        registry.registerHandler("/assets/{*filepath}")
                .addResourceLocations("classpath:static/", "file:/C:/Users/Ivan_Hontarenko/Git/")
                .setCacheControl(CacheControl.empty().cachePublic())
                .getChainRegistration()
                .addResolvers(new PathNormalizationResolver(), resolver, new LocationScanningResolver());

        registry.registerHandler("/icon/**")
                .addResourceLocations("classpath:static/icon/")
                .setCacheControl(CacheControl.empty().cachePublic())
                .setCachePeriod(60 * 60 * 24 * 365)
                .getChainRegistration()
                .addResolvers(
                        new PathNormalizationResolver(),
                        new VersionalResourceResolver()
                                .addStrategy(new QueryParameterVersionStrategy("_v"), "/icon/**"),
                        new LocationScanningResolver()
                );

        registry.registerHandler("/static_html/**")
                .addResourceLocations("classpath:HTML/")
                .setCacheControl(CacheControl.empty().cachePublic())
                .setCachePeriod(60 * 60 * 24 * 365)
                .getChainRegistration()
                .addResolvers(
                        new PathNormalizationResolver(),
                        new VersionalResourceResolver()
                                .addStrategy(new QueryParameterVersionStrategy("_v"), "/static_html/**"),
                        new LocationScanningResolver()
                );

        registry.registerHandler("/static/**")
                .addResourceLocations("classpath:HTML/")
                .setCachePeriod(3344411)
                .getChainRegistration()
                .addResolvers(
                        new PathNormalizationResolver(),
                        new VersionalResourceResolver()
                                .addStrategy(new ContentHashVersionStrategy("SHA-256", 16), "/static/**"),
                        new LocationScanningResolver()
                );
    }

}
