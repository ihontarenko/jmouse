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

import java.util.ArrayList;
import java.util.List;

@Priority(Integer.MAX_VALUE)
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
        performConfigurers(context);
    }

    public void performConfigurers(BeanContext context) {
        List<ApplicationConfigurer> configurers = new ArrayList<>(context.getBeans(ApplicationConfigurer.class));
        if (!configurers.isEmpty()) {
            configurers.sort(Sorter.PRIORITY_COMPARATOR);
            for (ApplicationConfigurer configurer : configurers) {
                configurer.configureEnvironment(environment);
                configurer.configureConversion(context.getBean(Conversion.class));
            }
        }
    }

}
