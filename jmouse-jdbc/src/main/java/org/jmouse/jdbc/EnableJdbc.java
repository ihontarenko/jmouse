package org.jmouse.jdbc;

import org.jmouse.context.feature.FeatureImport;
import org.jmouse.jdbc.configuration.JdbcTemplateConfiguration;
import org.jmouse.jdbc.configuration.JdbcTransactionConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@FeatureImport({
        JdbcTemplateConfiguration.class,
        JdbcTransactionConfiguration.class
})
public @interface EnableJdbc {

}
