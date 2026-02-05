package org.jmouse.jdbc;

import org.jmouse.beans.*;
import org.jmouse.beans.events.BeanEventDeduplicateKeyStrategy;
import org.jmouse.core.events.DeduplicatingPublishPolicy;
import org.jmouse.core.events.EventPublishPolicy;
import org.jmouse.jdbc.connection.datasource.DataSourceContributor;
import org.jmouse.jdbc.connection.datasource.DataSourceKeyHolder;
import org.jmouse.jdbc.connection.datasource.DataSourceSpecification;
import org.jmouse.jdbc.connection.datasource.DataSourceSpecificationRegistry;

import java.text.ParseException;
import java.text.SimpleDateFormat;

final public class Demo {

    public static void main(String... arguments) {
        BeanContext context = new DefaultBeanContext(JdbcSupport.class);
        context.setContextId("JDBC-CONTEXT");
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.addInitializer(new EventBridgeContextInitializer());

        context.onBeanLookupStarted(p -> {
            System.out.println("EVENT:Lookup: Bean: " + p.requiredType() + " | " + p.beanName());
        });

        DataSourceKeyHolder.use("mysql");

        ((DefaultBeanContext) context).setPublishPolicy(
                new DeduplicatingPublishPolicy(
                        EventPublishPolicy.publishAll(),
                        new BeanEventDeduplicateKeyStrategy()
                )
        );

        context.refresh();

        context.registerBean(DataSourceContributor.class, (DataSourceContributor) registry
                -> registry.register(new DataSourceSpecification(
                "mysql", null, "aaa:aaa", "username", "password", "catalog", "mysql", null, null
        )));

        SimpleOperations simple = context.getBean(SimpleOperations.class);

        System.out.println(context);
    }

}
