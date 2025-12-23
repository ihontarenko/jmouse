package org.jmouse.jdbc.connection;

import org.jmouse.beans.BeanContext;

import java.util.List;

final public class ConnectionProviderSelector {

    private final BeanContext context;

    public ConnectionProviderSelector(BeanContext context) {
        this.context = context;
    }

    public ConnectionProvider select() {
        List<String> providerNames = context.getBeanNames(ConnectionProvider.class);

        for (String beanName : providerNames) {
            try {
                return context.getBean(beanName);
            } catch (Exception ignored) {}
        }

        throw new IllegalStateException("No applicable connection providers found");
    }

}
