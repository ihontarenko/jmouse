package org.jmouse.jdbc;

import org.jmouse.beans.*;

final public class Demo {

    public static void main(String... arguments) {
        BeanContext context = new DefaultBeanContext(JdbcSupport.class);
        context.setContextId("JDBC-CONTEXT");
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.addInitializer(new EventBridgeContextInitializer());

        context.onBeanLookupStarted(p -> {
            System.out.println("EVENT:Lookup: " + p);
        });

        context.refresh();

        SimpleOperations simple = context.getBean(SimpleOperations.class);

        System.out.println(context);
    }

}
