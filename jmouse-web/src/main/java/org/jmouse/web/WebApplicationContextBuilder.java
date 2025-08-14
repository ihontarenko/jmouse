package org.jmouse.web;

import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.core.Bits;
import org.jmouse.mvc.context.WebMvcControllersInitializer;
import org.jmouse.mvc.context.WebMvcInfrastructureInitializer;
import org.jmouse.mvc.jMouseWebMvcRoot;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.context.StartupApplicationContextInitializer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.jmouse.mvc.context.WebMvcInfrastructureInitializer.*;

/**
 * 🔧 Builder for creating and customizing {@link WebBeanContext} instances.
 *
 * <pre>{@code
 * WebBeanContext context = new WebApplicationContextBuilder(factory)
 *     .name("web-app")
 *     .baseClasses(AppWebConfig.class)
 *     .useDefault().useWebMvc()
 *     .build();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Bean(scope = BeanScope.PROTOTYPE)
public class WebApplicationContextBuilder implements WebContextBuilder {

    private final WebApplicationFactory        factory;
    private final Set<Class<?>>                baseClasses  = new HashSet<>();
    private final Set<Class<?>>                coreClasses  = new HashSet<>(
            Set.of(jMouseWebMvcRoot.class, jMouseWebRoot.class));
    private final List<BeanContextInitializer> initializers = new ArrayList<>();
    private       String                       contextId;
    private       WebBeanContext               parent;
    private       boolean                      useDefault   = false;
    private       boolean                      useWebMvc    = false;
    private       Consumer<WebBeanContext>     customizer   = ctx -> {
    };

    /**
     * 🧱 Constructs a new builder with the provided factory.
     *
     * @param factory the web application factory
     */
    @BeanConstructor
    public WebApplicationContextBuilder(WebApplicationFactory factory) {
        this.factory = factory;
    }

    /**
     * 🏷 Sets the ID of the context.
     *
     * @param contextId the context name
     * @return this builder
     */
    @Override
    public WebContextBuilder name(String contextId) {
        this.contextId = contextId;
        return this;
    }

    /**
     * 📦 Sets the base classes for bean scanning.
     *
     * @param baseClasses the classes to scan
     * @return this builder
     */
    @Override
    public WebContextBuilder baseClasses(Class<?>... baseClasses) {
        this.baseClasses.addAll(List.of(baseClasses));
        return this;
    }

    /**
     * 📦 Sets the core classes for bean scanning.
     *
     * @param coreClasses the classes to scan
     * @return this builder
     */
    @Override
    public WebContextBuilder coreClasses(Class<?>... coreClasses) {
        this.coreClasses.addAll(List.of(coreClasses));
        return this;
    }

    /**
     * 🔗 Sets the parent context.
     *
     * @param parent the parent context
     * @return this builder
     */
    @Override
    public WebContextBuilder parent(WebBeanContext parent) {
        this.parent = parent;
        return this;
    }

    /**
     * ➕ Adds a custom initializer.
     *
     * @param initializer the initializer to add
     * @return this builder
     */
    @Override
    public WebContextBuilder addInitializer(BeanContextInitializer initializer) {
        this.initializers.add(initializer);
        return this;
    }

    /**
     * ✅ Enables default initializers like scanner, parent env, etc.
     *
     * @return this builder
     */
    @Override
    public WebContextBuilder useDefault() {
        this.useDefault = true;
        return this;
    }

    /**
     * 🌐 Enables Web MVC components (controllers, mappings).
     *
     * @return this builder
     */
    @Override
    public WebContextBuilder useWebMvc() {
        this.useWebMvc = true;
        return this;
    }

    /**
     * 🛠 Adds a customizer to be applied after context is created but before refresh.
     *
     * @param customizer the customizer consumer
     * @return this builder
     */
    @Override
    public WebContextBuilder customize(Consumer<WebBeanContext> customizer) {
        this.customizer = customizer;
        return this;
    }

    /**
     * 🧪 Builds and initializes the context.
     *
     * @return the configured {@link WebBeanContext}
     */
    @Override
    public WebBeanContext build() {
        WebBeanContext context = factory.createContext(contextId, parent, baseClasses.toArray(Class<?>[]::new));
        boolean        isRoot  = (parent == null);

        context.setContextId(contextId);

        if (useDefault) {
            context.addInitializer(new BeanScanAnnotatedContextInitializer());
            context.addInitializer(new ApplicationContextBeansScanner());
            context.addInitializer(new StartupApplicationContextInitializer(context.getEnvironment()));
        }

        for (BeanContextInitializer initializer : initializers) {
            context.addInitializer(initializer);
        }



        if (useWebMvc) {
            if (!isRoot) {
                // scan only in user classes
                new WebMvcInfrastructureInitializer(Bits.of()).initialize(context);
                // scan only in user+core classes
                new WebMvcInfrastructureInitializer(Bits.of(
                        HANDLER_MAPPING,
                        HANDLER_ADAPTER,
                        RETURN_VALUE_HANDLER,
                        ARGUMENT_RESOLVER,
                        EXCEPTION_RESOLVER,
                        ROUTING_MAPPING,
                        EXCEPTION_MAPPING
                ), coreClasses.toArray(Class<?>[]::new)).initialize(context);
            } else {
                context.addInitializer(new WebMvcInfrastructureInitializer());
            }

            context.addInitializer(new WebMvcControllersInitializer());
        }

        if (customizer != null) {
            customizer.accept(context);
        }

        context.refresh();

        return context;
    }
}
