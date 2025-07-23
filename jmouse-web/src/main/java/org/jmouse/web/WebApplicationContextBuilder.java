package org.jmouse.web;

import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.mvc.context.WebMvcControllersInitializer;
import org.jmouse.mvc.context.WebMvcInfrastructureInitializer;
import org.jmouse.mvc.jMouseWebMvcRoot;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Bean(scope = BeanScope.PROTOTYPE)
public class WebApplicationContextBuilder implements WebContextBuilder {

    private final WebApplicationFactory        factory;
    private final Set<Class<?>>                baseClasses  = new HashSet<>();
    private final List<BeanContextInitializer> initializers = new ArrayList<>();
    private       String                       contextId;
    private       WebBeanContext               parent;
    private       boolean                      useDefault   = false;
    private       boolean                      useWebMvc    = false;
    private       Consumer<WebBeanContext>     customizer   = ctx -> {
    };

    @BeanConstructor
    public WebApplicationContextBuilder(WebApplicationFactory factory) {
        this.factory = factory;
    }

    @Override
    public WebContextBuilder name(String contextId) {
        this.contextId = contextId;
        return this;
    }

    @Override
    public WebContextBuilder baseClasses(Class<?>... baseClasses) {
        this.baseClasses.addAll(List.of(baseClasses));
        return this;
    }

    @Override
    public WebContextBuilder parent(WebBeanContext parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public WebContextBuilder addInitializer(BeanContextInitializer initializer) {
        this.initializers.add(initializer);
        return this;
    }

    @Override
    public WebContextBuilder useDefaultInitializers() {
        this.useDefault = true;
        return this;
    }

    @Override
    public WebContextBuilder useWebMvcInitializers() {
        this.useWebMvc = true;
        return this;
    }

    @Override
    public WebContextBuilder customize(Consumer<WebBeanContext> customizer) {
        this.customizer = customizer;
        return this;
    }

    @Override
    public WebBeanContext build() {
        WebBeanContext context = factory.createContext(contextId, baseClasses.toArray(Class<?>[]::new));

        context.setParentContext(parent);
        context.setContextId(contextId);

        if (useDefault) {
            context.addInitializer(new BeanScanAnnotatedContextInitializer());
            context.addInitializer(new ApplicationContextBeansScanner());
            if (parent != null) {
                context.addInitializer(new StartupRootApplicationContextInitializer(parent.getEnvironment()));
            }
        }

        if (useWebMvc) {
            context.addInitializer(new WebMvcControllersInitializer());
        }

        for (BeanContextInitializer initializer : initializers) {
            context.addInitializer(initializer);
        }

        customizer.accept(context);

        context.refresh();

        if (useWebMvc) {
            new WebMvcInfrastructureInitializer(jMouseWebMvcRoot.class).initialize(context);
        }

        return context;
    }
}
