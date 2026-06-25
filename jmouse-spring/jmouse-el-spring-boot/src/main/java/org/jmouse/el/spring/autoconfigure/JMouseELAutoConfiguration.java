package org.jmouse.el.spring.autoconfigure;

import org.jmouse.el.renderable.TemplateEngine;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.spring.JMouseELProperties;
import org.jmouse.el.spring.JMouseViewResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({TemplateEngine.class, DispatcherServlet.class})
@EnableConfigurationProperties(JMouseELProperties.class)
public class JMouseELAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TemplateEngine.class)
    public TemplateEngine jmouseTemplateEngine(JMouseELProperties properties) {
        TemplateEngine engine = new TemplateEngine();
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix(properties.getPrefix());
        loader.setSuffix(properties.getSuffix());
        engine.setLoader(loader);
        return engine;
    }

    @Bean
    @ConditionalOnMissingBean(JMouseViewResolver.class)
    public JMouseViewResolver jmouseViewResolver(TemplateEngine engine, JMouseELProperties properties) {
        JMouseViewResolver resolver = new JMouseViewResolver(engine);
        resolver.setContentType(properties.getContentType());
        resolver.setOrder(properties.getOrder());
        return resolver;
    }
}
