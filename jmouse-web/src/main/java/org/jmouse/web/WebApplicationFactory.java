package org.jmouse.web;

import org.jmouse.context.AbstractApplicationFactory;
import org.jmouse.core.env.*;
import org.jmouse.web.context.WebApplicationBeanContext;
import org.jmouse.web.context.WebBeanContext;

import java.util.HashSet;
import java.util.Set;

public class WebApplicationFactory extends AbstractApplicationFactory<WebBeanContext> {

    @Override
    public Environment createDefaultEnvironment() {
        Environment environment = super.createDefaultEnvironment();

//        loadApplicationProperties("classpath:webserver.properties", environment);

        return environment;
    }

    @Override
    public WebBeanContext createContext(String contextId, Class<?>... baseClasses) {
        return createContext(contextId, null, baseClasses);
    }

    @Override
    public WebBeanContext createContext(String contextId, WebBeanContext rootContext, Class<?>... classes) {
        Set<Class<?>> baseClasses = new HashSet<>(Set.of(classes));

        if (rootContext != null) {
            baseClasses.addAll(Set.of(rootContext.getBaseClasses()));
        }

        WebBeanContext context = createContextInstance(contextId, baseClasses.toArray(Class<?>[]::new));

        context.refresh();
        context.setParentContext(rootContext);

        return context;
    }

    private WebBeanContext createContextInstance(String contextId, Class<?>... baseClasses) {
        WebBeanContext context = new WebApplicationBeanContext(baseClasses);
        context.setContextId(contextId);
        return context;
    }

}
