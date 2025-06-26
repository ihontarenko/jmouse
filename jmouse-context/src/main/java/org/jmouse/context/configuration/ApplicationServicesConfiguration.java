package org.jmouse.context.configuration;

import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.core.convert.PredefinedConversion;
import org.jmouse.core.convert.converter.*;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.core.convert.Conversion;
import org.jmouse.common.mapping.Mapping;
import org.jmouse.common.mapping.MappingFactory;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.core.i18n.MessageSource;
import org.jmouse.core.i18n.StandardMessageSourceBundle;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.extension.Extension;

import java.util.Set;

@BeanFactories
public class ApplicationServicesConfiguration {

    @Bean
    public Mapping mappingService(ApplicationBeanContext context) {
        return MappingFactory.create(context.getBaseClasses());
    }

    @Bean(proxied = true)
    public Conversion conversionService() {
        Conversion conversion = new PredefinedConversion() {
        };

        DateAndTimeConverters.getConverters().forEach(conversion::registerConverter);
        CollectionConverters.getConverters().forEach(conversion::registerConverter);

        return conversion;
    }

    @Bean
    public ExpressionLanguage expressionLanguage(@AggregatedBeans Set<Extension> extensions) {
        ExpressionLanguage el = new ExpressionLanguage();

        if (extensions != null) {
            extensions.forEach(el.getExtensions()::importExtension);
        }

        return el;
    }

    @Bean
    public Extension getDefaultExtension() {
        return new Extension() { };
    }

    @Bean
    public MessageSource messageSource() {
        StandardMessageSourceBundle sourceBundle = new StandardMessageSourceBundle(ApplicationServicesConfiguration.class.getClassLoader());

        sourceBundle.setFallbackPattern("{? %s ?}");
        sourceBundle.setFallbackWithCode(true);
        sourceBundle.addNames("i18n.messages");

        return sourceBundle;
    }

}
