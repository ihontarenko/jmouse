package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.web.mvc.BeanConfigurer;
import org.jmouse.web.mvc.mapping.ResourceHttpMapping;

@Bean
public class ResourcesConfig implements BeanConfigurer<ResourceHttpMapping> {

    @Override
    public void configure(ResourceHttpMapping mapping) {
        mapping.addResourceResolver("/assets/{filepath:.+}")
                .addResourceLocations("classpath:static/", "local:C:\\Users\\Ivan_Hontarenko\\Git\\");
    }

}
