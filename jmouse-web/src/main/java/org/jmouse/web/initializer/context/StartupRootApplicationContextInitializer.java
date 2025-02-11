package org.jmouse.web.initializer.context;

import org.jmouse.beans.BeanContainer;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.beans.BeanScope;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.env.Environment;
import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.ResourceLoader;
import org.jmouse.common.mapping.Mapping;
import org.jmouse.util.Priority;
import org.jmouse.util.Sorter;
import org.jmouse.context.ApplicationConfigurer;

import java.util.List;

@Priority(-1)
public class StartupRootApplicationContextInitializer implements BeanContextInitializer {

    private final Environment environment;

    public StartupRootApplicationContextInitializer(Environment environment) {
        this.environment = environment;
    }

    /**
     * Initializes the given {@link BeanContext}.
     *
     * @param context the {@link BeanContext} to initialize.
     */
    @Override
    public void initialize(BeanContext context) {
        context.registerBean(Environment.class, environment);
        context.registerBean(ResourceLoader.class, new CompositeResourceLoader());

        performConfigurers(context);
    }

    public void performConfigurers(BeanContext context) {
        List<ApplicationConfigurer> configurers = context.getBeans(ApplicationConfigurer.class);

        if (!configurers.isEmpty()) {
            configurers.sort(Sorter.PRIORITY_COMPARATOR);
            BeanContainer container = context.getBeanContainer(BeanScope.SINGLETON);
            for (ApplicationConfigurer configurer : configurers) {
                configurer.registerSingleton(container);
                configurer.configureEnvironment(environment);
                configurer.configureConversion(context.getBean(Conversion.class));
                configurer.configureMapping(context.getBean(Mapping.class));
            }
        }
    }

}
