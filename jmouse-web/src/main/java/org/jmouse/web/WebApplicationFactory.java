package org.jmouse.web;

import org.jmouse.core.env.*;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.web.context.WebApplicationBeanContext;
import org.jmouse.web.context.WebBeanContext;

import java.util.HashSet;
import java.util.Set;

public class WebApplicationFactory implements ApplicationFactory<WebBeanContext> {

    public static final String SYSTEM_PROPERTIES     = "system-properties";
    public static final String SYSTEM_ENV_PROPERTIES = "system-env";

    @Override
    public Environment createDefaultEnvironment() {
        Environment            environment = new StandardEnvironment();
        PropertySourceRegistry registry    = environment.getRegistry();

        registry.addPropertySource(new SystemEnvironmentPropertySource(SYSTEM_ENV_PROPERTIES));
        registry.addPropertySource(new SystemPropertiesPropertySource(SYSTEM_PROPERTIES));

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
