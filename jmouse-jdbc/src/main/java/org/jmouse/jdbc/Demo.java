package org.jmouse.jdbc;

import org.jmouse.beans.*;
import org.jmouse.beans.events.BeanEventDeduplicateKeyStrategy;
import org.jmouse.core.events.DeduplicatingPublishPolicy;
import org.jmouse.core.events.EventPublishPolicy;

final public class Demo {

    public static void main(String... arguments) {
        BeanContext context = new DefaultBeanContext(JdbcSupport.class);
        context.setContextId("JDBC-CONTEXT");
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.addInitializer(new EventBridgeContextInitializer());

        context.onBeanLookupStarted(p -> {
            System.out.println("EVENT:Lookup: Bean: " + p.requiredType() + " | " + p.beanName());
        });

        ((DefaultBeanContext) context).setPublishPolicy(
                new DeduplicatingPublishPolicy(
                        EventPublishPolicy.publishAll(),
                        new BeanEventDeduplicateKeyStrategy()
                )
        );

        context.refresh();

        SimpleOperations simple = context.getBean(SimpleOperations.class);

        System.out.println(context);
    }

}
