package org.jmouse.jmt.spring.autoconfigure;

import org.jmouse.el.template.TemplateEngine;
import org.jmouse.el.template.loader.ClasspathLoader;
import org.jmouse.jmt.spring.JMouseTemplateProperties;
import org.jmouse.jmt.spring.JMouseTemplateViewResolver;
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
@EnableConfigurationProperties(JMouseTemplateProperties.class)
public class JMouseTemplateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TemplateEngine.class)
    public TemplateEngine jmouseTemplateEngine(JMouseTemplateProperties properties) {
        TemplateEngine engine = new TemplateEngine();
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix(properties.getPrefix());
        loader.setSuffix(properties.getSuffix());
        engine.setLoader(loader);
        return engine;
    }

    @Bean
    @ConditionalOnMissingBean(JMouseTemplateViewResolver.class)
    public JMouseTemplateViewResolver jmouseTemplateViewResolver(TemplateEngine engine, JMouseTemplateProperties properties) {
        JMouseTemplateViewResolver resolver = new JMouseTemplateViewResolver(engine);
        resolver.setContentType(properties.getContentType());
        resolver.setOrder(properties.getOrder());
        return resolver;
    }
}
