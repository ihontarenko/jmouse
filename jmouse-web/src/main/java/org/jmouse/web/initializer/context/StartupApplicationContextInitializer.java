package org.jmouse.web.initializer.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.env.Environment;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.mvc.BeanConfigurer;
import org.jmouse.util.Priority;
import org.jmouse.util.Sorter;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.web.context.WebBeanContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧩 Default initializer for bootstrapping a {@link WebBeanContext}.
 * <p>
 * It performs:
 * <ul>
 *     <li>Calling all {@link ApplicationConfigurer} beans (in order of priority)</li>
 *     <li>Initializing beans via {@link BeanConfigurer}s (by matching bean type)</li>
 * </ul>
 * This initializer runs with the lowest priority by default.
 *
 * <pre>{@code
 * context.addInitializer(new StartupApplicationContextInitializer(environment));
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(Integer.MAX_VALUE)
public class StartupApplicationContextInitializer implements BeanContextInitializer {

    private final Environment environment;

    public StartupApplicationContextInitializer(Environment environment) {
        this.environment = environment;
    }

    /**
     * 🔧 Initializes the provided {@link BeanContext}.
     * <p>
     * Runs both {@link #performConfigurers(BeanContext)} and {@link #performBeanConfigurers(BeanContext)}.
     *
     * @param context the bean context to initialize
     */
    @Override
    public void initialize(BeanContext context) {
        performConfigurers(context);
        performBeanConfigurers(context);
    }

    /**
     * ⚙️ Applies all {@link BeanConfigurer}s declared in the local context.
     * <p>
     * For each initializer, finds all beans of the matching type and invokes {@code initialize()}.
     *
     * @param context the current bean context
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void performBeanConfigurers(BeanContext context) {
        List<BeanConfigurer> beanConfigurers = new ArrayList<>(
                WebBeanContext.getLocalBeans(
                        BeanConfigurer.class, (WebBeanContext) context)
        );

        if (!beanConfigurers.isEmpty()) {
            Sorter.sort(beanConfigurers);
            for (BeanConfigurer<?> beanConfigurer : beanConfigurers) {
                JavaType type = JavaType.forInstance(beanConfigurer)
                        .locate(BeanConfigurer.class).getFirst();
                handleBeanConfigurer(context, (BeanConfigurer<Object>) beanConfigurer, type.getClassType());
            }
        }
    }

    /**
     * 💡 Applies a single {@link BeanConfigurer} to all beans of the matching type.
     *
     * @param context     the bean context
     * @param initializer the initializer to apply
     * @param beanClass   the target class type to initialize
     */
    @SuppressWarnings("unchecked")
    private void handleBeanConfigurer(BeanContext context, BeanConfigurer<Object> initializer, Class<?> beanClass) {
        if (beanClass != Object.class) {
            List<Object> beans = context.getBeans((Class<Object>) beanClass);
            for (Object bean : beans) {
                initializer.configure(bean);
            }
        }
    }

    /**
     * 🛠 Runs all {@link ApplicationConfigurer}s in the local context.
     * <p>
     * This method:
     * <ul>
     *     <li>Invokes {@code configureEnvironment()} on each configurer</li>
     *     <li>Injects and configures the {@link Conversion} system</li>
     * </ul>
     *
     * @param context the bean context
     */
    public void performConfigurers(BeanContext context) {
        List<ApplicationConfigurer> configurers = new ArrayList<>(
                WebBeanContext.getLocalBeans(
                        ApplicationConfigurer.class, (WebBeanContext) context)
        );

        if (!configurers.isEmpty()) {
            Sorter.sort(configurers);
            for (ApplicationConfigurer configurer : configurers) {
                configurer.configureEnvironment(environment);
                configurer.configureConversion(context.getBean(Conversion.class));
            }
        }
    }
}
