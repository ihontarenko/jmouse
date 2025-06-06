package org.jmouse.web.initializer.application;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassMatchers;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.servlet.registration.RegistrationBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class WebApplicationInitializerProvider {

    private final WebBeanContext context;

    public WebApplicationInitializerProvider(WebBeanContext context) {
        this.context = context;
    }

    public List<WebApplicationInitializer> getWebApplicationInitializers() {
        return List.copyOf(context.getBeans(WebApplicationInitializer.class));
    }

    public List<WebApplicationInitializer> getWebApplicationInitializersExcluding(Class<?>... types) {
        List<WebApplicationInitializer> initializers = getWebApplicationInitializers();

        if (types != null && types.length > 0) {
            Optional<Matcher<Class<?>>> optional = Arrays.stream(types)
                    .map(ClassMatchers::isSupertype).reduce(Matcher::logicalOr);

            Matcher<Class<?>> matcher = optional.get();
            initializers = initializers.stream()
                    .filter(initializer -> !matcher.matches(initializer.getClass())).toList();
        }

        return initializers;
    }

    public List<WebApplicationInitializer> getWebApplicationInitializersWithoutRegistration() {
        return getWebApplicationInitializersExcluding(RegistrationBean.class);
    }

}
