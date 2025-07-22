package org.jmouse.web;

import org.jmouse.context.AbstractApplicationFactory;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.core.env.*;
import org.jmouse.mvc.jMouseWebMvcRoot;
import org.jmouse.web.context.WebApplicationBeanContext;
import org.jmouse.web.context.WebBeanContext;

import java.util.HashSet;
import java.util.Set;

public class WebApplicationFactory extends AbstractApplicationFactory<WebBeanContext> {

    @Override
    public Environment createDefaultEnvironment() {
        return super.createDefaultEnvironment();
    }

    @Override
    public WebBeanContext createContext(String contextId, Class<?>... baseClasses) {
        return createContext(contextId, null, baseClasses);
    }

    @Override
    public WebBeanContext createContext(String contextId, WebBeanContext rootContext, Class<?>... classes) {
        Set<Class<?>> baseClasses = new HashSet<>(Set.of(classes));

        WebBeanContext context = createContextInstance(contextId, baseClasses.toArray(Class<?>[]::new));

        context.refresh();
        context.setParentContext(rootContext);

        return context;
    }

    @Override
    public WebBeanContext createRootContext() {
        WebBeanContext context = createContext(WebBeanContext.DEFAULT_ROOT_WEB_CONTEXT_NAME,
                jMouseWebRoot.class, jMouseWebMvcRoot.class);

        context.registerBean(Environment.class, createDefaultEnvironment());
        context.registerBean(ApplicationFactory.class, this);

        return context;
    }

    private WebBeanContext createContextInstance(String contextId, Class<?>... baseClasses) {
        WebBeanContext context = new WebApplicationBeanContext(baseClasses);
        context.setContextId(contextId);
        return context;
    }

}
