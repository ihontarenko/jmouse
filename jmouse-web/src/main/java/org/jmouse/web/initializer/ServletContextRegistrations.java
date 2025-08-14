package org.jmouse.web.initializer;

import org.jmouse.beans.BeanContext;
import org.jmouse.core.Sorter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.RequestContextListener;
import org.jmouse.web.servlet.SessionConfigurationInitializer;
import org.jmouse.web.servlet.WebBeanContextListener;
import org.jmouse.web.servlet.registration.RegistrationBean;
import org.jmouse.web.servlet.registration.ServletListenerRegistrationBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Resolves all servlet-related {@link WebApplicationInitializer} instances from the bean context,
 * including predefined listeners for request and web bean scopes.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ServletContextRegistrations registrations = new ServletContextRegistrations();
 * Collection<WebApplicationInitializer> initializers = registrations.getRegistrationBeanInitializers(context);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
public class ServletContextRegistrations {

    public static final String WEB_BEAN_CONTEXT_LISTENER_NAME = "webBeanContextListener";
    public static final String REQUEST_CONTEXT_LISTENER_NAME  = "requestContextListener";

    /**
     * Gathers all {@link WebApplicationInitializer} beans that register servlets, filters, or listeners,
     * and appends predefined context listeners.
     *
     * @param context application context
     * @return sorted list of all applicable initializers
     */
    public Collection<WebApplicationInitializer> getRegistrationBeanInitializers(BeanContext context) {
        List<WebApplicationInitializer>   initializers = new ArrayList<>();
        WebApplicationInitializerProvider provider     = new WebApplicationInitializerProvider(
                (WebBeanContext) context);

        for (WebApplicationInitializer bean : provider.getRegistrationBeans()) {
            if (bean instanceof RegistrationBean registrationBean) {
                initializers.add((WebApplicationInitializer) registrationBean);
            }
        }

        initializePredefined(initializers, context);
        Sorter.sort(initializers);
        return initializers;
    }

    private void initializePredefined(List<WebApplicationInitializer> initializers, BeanContext context) {
        ServletListenerRegistrationBean<?> webBeanContextListener = new ServletListenerRegistrationBean<>(
                WEB_BEAN_CONTEXT_LISTENER_NAME, new WebBeanContextListener((WebBeanContext) context));

        ServletListenerRegistrationBean<?> requestContextListener = new ServletListenerRegistrationBean<>(
                REQUEST_CONTEXT_LISTENER_NAME, new RequestContextListener());

        webBeanContextListener.setEnabled(true);
        requestContextListener.setEnabled(true);

        initializers.add(requestContextListener);
        initializers.add(webBeanContextListener);
        initializers.add(context.getBean(SessionConfigurationInitializer.class));
    }
}
