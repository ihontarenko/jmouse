package org.jmouse.jdbc.smoke;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.beans.EventBridgeContextInitializer;
import org.jmouse.beans.events.BeanEventDeduplicateKeyStrategy;
import org.jmouse.core.events.DeduplicatingPublishPolicy;
import org.jmouse.core.events.EventPublishPolicy;
import org.jmouse.jdbc.JdbcTemplate;
import org.jmouse.jdbc.JdbcSupport;
import org.jmouse.jdbc.NamedTemplate;
import org.jmouse.jdbc.connection.datasource.DataSourceContributor;
import org.jmouse.jdbc.connection.datasource.DataSourceKeyHolder;
import org.jmouse.jdbc.connection.datasource.DataSourceSpecification;

public final class SmokeB {

    public static void main(String... arguments) throws Exception {
        BeanContext context = new DefaultBeanContext(JdbcSupport.class);
        context.setContextId("JDBC-CONTEXT");
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.addInitializer(new EventBridgeContextInitializer());

        context.onBeanLookupStarted(payload -> {
            System.out.println("EVENT:Lookup: Bean: " + payload.requiredType() + " | " + payload.beanName());
        });

        DataSourceKeyHolder.use("mysql");

        ((DefaultBeanContext) context).setPublishPolicy(
                new DeduplicatingPublishPolicy(
                        EventPublishPolicy.publishAll(),
                        new BeanEventDeduplicateKeyStrategy()
                )
        );

        context.refresh();

        context.registerBean(DataSourceContributor.class, (DataSourceContributor) registry ->
                registry.register(new DataSourceSpecification(
                        "mysql",
                        null,
                        "jdbc:mysql://127.0.0.1:3306/jmouse?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC",
                        "jmouse",
                        "jmouse",
                        "jmouse",
                        "mysql",
                        null,
                        null
                ))
        );

        JdbcTemplate  jdbcOperations  = context.getBean(JdbcTemplate.class);
        NamedTemplate namedOperations = context.getBean(NamedTemplate.class);

        System.out.println(context);
    }

}