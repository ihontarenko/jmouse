package org.jmouse.jdbc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.beans.EventBridgeContextInitializer;
import org.jmouse.beans.events.BeanContextEvents;

final public class Demo {

    public static void main(String... arguments) {
        BeanContext context = new DefaultBeanContext(JdbcSupport.class);
        context.setContextId("JDBC-CONTEXT");
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.addInitializer(new EventBridgeContextInitializer());
        context.refresh();

        if (context instanceof BeanContextEvents contextEvents) {
            contextEvents.onBeanCreated(p -> {
                System.out.println("Created bean: " + p.definition().getBeanName() + " -> " + p.instance());
            });

            contextEvents.onBeanNotFound(p -> {
                System.out.println("Missing bean: " + p.beanName());
            });

            contextEvents.onContextError(p -> {
                System.out.println("Context error at stage " + p.stage() + ": " + p.error());
            });
        }

        SimpleOperations simple = context.getBean(SimpleOperations.class);

        System.out.println(context);
    }

}
