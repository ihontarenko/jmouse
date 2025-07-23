package org.jmouse.web;

import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.context.AbstractApplicationFactory;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.context.conversion.ContextConversion;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.env.*;
import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.ResourceLoader;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.mvc.context.WebMvcControllersInitializer;
import org.jmouse.mvc.context.WebMvcInfrastructureInitializer;
import org.jmouse.mvc.jMouseWebMvcRoot;
import org.jmouse.web.context.WebApplicationBeanContext;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;

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
        context.registerBean(Conversion.class, new ContextConversion());
        context.registerBean(ResourceLoader.class, new CompositeResourceLoader());
        context.registerBean(ExpressionLanguage.class, new ExpressionLanguage());

        return context;
    }

    @Override
    public WebBeanContext createApplicationContext(String contextId, WebBeanContext rootContext, Class<?>... classes) {
        WebBeanContext context = createContext(contextId, rootContext, classes);

        context.addInitializer(new BeanScanAnnotatedContextInitializer());
        context.addInitializer(new ApplicationContextBeansScanner());
        context.addInitializer(new StartupRootApplicationContextInitializer(rootContext.getEnvironment()));
        context.addInitializer(new WebMvcControllersInitializer());

        context.refresh();

        new WebMvcInfrastructureInitializer(jMouseWebMvcRoot.class).initialize(context);

        return context;
    }

    private WebBeanContext createContextInstance(String contextId, Class<?>... baseClasses) {
        WebBeanContext context = new WebApplicationBeanContext(baseClasses);
        context.setContextId(contextId);
        return context;
    }

}
