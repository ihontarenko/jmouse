package org.jmouse.petgallery.config;

import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.http.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.mapping.ResourceHttpMapping;
import org.jmouse.web.mvc.resource.*;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.server.tomcat.TomcatWebServerFactory;

@BeanFactories
public class PetGalleryConfiguration implements InitializingBeanSupport<WebBeanContext> {

    @Bean("webServerFactory")
    public WebServerFactory createWebServerFactory() {
        return new TomcatWebServerFactory() {};
    }

    @Override
    public void doInitialize(WebBeanContext context) {
        ResourceHttpMapping     mapping  = context.getBean(ResourceHttpMapping.class);
        ResourceHandlerRegistry registry = context.getBean(ResourceHandlerRegistry.class);

        registry.registerHandler("/assets/**")
                .addResourceLocations("classpath:static/")
                .setAllow(Allow.of(HttpMethod.GET, HttpMethod.HEAD))
                .setVary(Vary.of(HttpHeader.ACCEPT_LANGUAGE, HttpHeader.ACCEPT_CHARSET))
                .setCacheControl(CacheControl.empty().cachePublic())
                .getChainRegistration()
                .addResolvers(
                        new PathNormalizationResolver(),
                        getVersionalResourceResolver(),
                        new LocationScanningResolver()
                );

        mapping.handleRegistrations(
                registry.getRegistrations()
        );
    }

    private VersionalResourceResolver getVersionalResourceResolver() {
        return new VersionalResourceResolver()
                .addStrategy(
                        new ContentHashVersionStrategy("SHA-256", 8), "/assets/**"
                );
    }

}
