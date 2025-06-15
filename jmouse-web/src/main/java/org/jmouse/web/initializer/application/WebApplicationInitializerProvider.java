package org.jmouse.web.initializer.application;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassMatchers;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.servlet.registration.RegistrationBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Provides access to registered {@link WebApplicationInitializer} beans
 * with filtering capabilities based on type.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * WebApplicationInitializerProvider provider = new WebApplicationInitializerProvider(context);
 * List<WebApplicationInitializer> initializers = provider.getRegistrationBeans();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
public class WebApplicationInitializerProvider {

    private final WebBeanContext context;

    public WebApplicationInitializerProvider(WebBeanContext context) {
        this.context = context;
    }

    /**
     * Returns all {@link WebApplicationInitializer} beans found in the context.
     *
     * @return list of initializers
     */
    public List<WebApplicationInitializer> getWebApplicationInitializers() {
        return List.copyOf(context.getBeans(WebApplicationInitializer.class));
    }

    /**
     * Returns all initializers excluding the given types or their subtypes.
     *
     * @param types classes to exclude (via isAssignableFrom)
     * @return filtered list of initializers
     */
    public List<WebApplicationInitializer> getExcluding(Class<?>... types) {
        return filterInitializers(createClassMatcherForTypes(types), false);
    }

    /**
     * Returns all initializers including only those matching the given types or their subtypes.
     *
     * @param types classes to include (via isAssignableFrom)
     * @return filtered list of initializers
     */
    public List<WebApplicationInitializer> getIncluding(Class<?>... types) {
        return filterInitializers(createClassMatcherForTypes(types), true);
    }

    /**
     * Returns all initializers that are subtypes of {@link RegistrationBean}.
     *
     * @return list of registration-related initializers
     */
    public List<WebApplicationInitializer> getRegistrationBeans() {
        return getIncluding(RegistrationBean.class);
    }

    private Matcher<Class<?>> createClassMatcherForTypes(Class<?>... types) {
        return Arrays.stream(types)
                .map(ClassMatchers::isSupertype)
                .reduce(Matcher::logicalOr)
                .orElse(Matcher.constant(true));
    }

    private List<WebApplicationInitializer> filterInitializers(Matcher<Class<?>> matcher, boolean include) {
        return getWebApplicationInitializers().stream()
                .filter(i -> include == matcher.matches(i.getClass()))
                .toList();
    }
}
