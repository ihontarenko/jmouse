package org.jmouse.web.initializer.application;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.context.ApplicationFactory;
import org.jmouse.core.bind.Bind;
import org.jmouse.core.bind.BindResult;
import org.jmouse.core.bind.Binder;
import org.jmouse.util.Priority;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.RequestContextListener;
import org.jmouse.web.servlet.WebBeanContextListener;
import org.jmouse.web.servlet.registration.RegistrationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EventListener;
import java.util.List;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * Initializer for configuring the {@link ServletContext} with the {@link WebBeanContextListener}.
 * Extends {@link AbstractWebApplicationInitializer} to include additional setup logic.
 */
@Priority(Integer.MIN_VALUE)
public class jMouseWebApplicationInitializer extends AbstractWebApplicationInitializer {

    public static final  String JMOUSE_WEB_SERVLET_REGISTRATION_PATH = "jmouse.web.servlet.registration";
    /**
     * Logger for logging initialization process
     */
    private static final Logger LOGGER                               = LoggerFactory.getLogger(
            jMouseWebApplicationInitializer.class);

    private final WebBeanContext context;

    @BeanConstructor
    public jMouseWebApplicationInitializer(WebBeanContext context) {
        this.context = context;
    }

    /**
     * Configures the {@link ServletContext} by adding the {@link WebBeanContextListener}.
     *
     * @param servletContext the {@link ServletContext} to configure.
     * @throws ServletException if any servlet-related error occurs during initialization.
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);

        registerServletContextListeners(servletContext);
        registerRequestContextListeners(servletContext);

        registerServlets(servletContext);
    }

    private void registerServlets(ServletContext servletContext) {

        context.getBean(ApplicationFactory.class);

        Binder                             binder        = Binder.withValueAccessor(context.getEnvironment());
        BindResult<List<RegistrationBean>> registrations = Bind.with(binder)
                .toList(JMOUSE_WEB_SERVLET_REGISTRATION_PATH, RegistrationBean.class);

        registrations.ifPresent(beanRegistrations -> {
            for (RegistrationBean registration : beanRegistrations) {
//                ServletRegistration.Dynamic dynamic = servletContext
//                        .addServlet(registration.getName(), new FrameworkDispatcherServlet(webBeanContext));
//                dynamic.addMapping(registration.mappings());
//                dynamic.setLoadOnStartup(1);
            }
        });
    }

    private void registerServletContextListeners(ServletContext servletContext) {
        // Add the WebBeanContextLoaderListener to the servlet context
        registerServletContextListener(servletContext, new WebBeanContextListener(context));
    }

    private void registerRequestContextListeners(ServletContext servletContext) {
        // Add the RequestContextLoaderListener to the servlet context
        registerServletContextListener(servletContext, new RequestContextListener());
    }

    private void registerServletContextListener(ServletContext servletContext, EventListener listener) {
        if (listener != null) {
            servletContext.addListener(listener);
            LOGGER.info("Event listener '{}' registered to ServletContext", getShortName(listener.getClass()));
        }
    }
}
