package svit.web.context.initializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.util.Priority;
import svit.web.AbstractApplicationInitializer;
import svit.web.servlet.RequestContextLoaderListener;
import svit.web.servlet.WebBeanContextLoaderListener;

import java.util.EventListener;

import static svit.reflection.Reflections.getShortName;

/**
 * Initializer for configuring the {@link ServletContext} with the {@link WebBeanContextLoaderListener}.
 * Extends {@link AbstractApplicationInitializer} to include additional setup logic.
 */
@Priority(Integer.MIN_VALUE)
public class WebBeanContextServletInitializer extends AbstractApplicationInitializer {

    /**
     * Logger for logging initialization process
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebBeanContextServletInitializer.class);

    /**
     * Configures the {@link ServletContext} by adding the {@link WebBeanContextLoaderListener}.
     *
     * @param servletContext the {@link ServletContext} to configure.
     * @throws ServletException if any servlet-related error occurs during initialization.
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);

        registerServletContextListeners(servletContext);
        registerRequestContextListeners(servletContext);
    }

    private void registerServletContextListeners(ServletContext servletContext) {
        // Add the WebBeanContextLoaderListener to the servlet context
        registerServletContextListener(servletContext, new WebBeanContextLoaderListener());
    }

    private void registerRequestContextListeners(ServletContext servletContext) {
        // Add the RequestContextLoaderListener to the servlet context
        registerServletContextListener(servletContext, new RequestContextLoaderListener());
    }

    private void registerServletContextListener(ServletContext servletContext, EventListener listener) {
        if (listener != null) {
            servletContext.addListener(listener);
            LOGGER.info("Event listener '{}' registered to ServletContext", getShortName(listener.getClass()));
        }
    }
}
