package svit.web;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * An abstract implementation of the {@link ApplicationInitializer} interface.
 * Provides a default empty implementation of the {@code onStartup} method for subclasses to override.
 */
public abstract class AbstractApplicationInitializer implements ApplicationInitializer {

    /**
     * Called during the startup phase of the servlet context.
     * Subclasses can override this method to provide specific initialization logic.
     *
     * @param servletContext the {@link ServletContext} being initialized.
     * @throws ServletException if any error occurs during initialization.
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

    }
}
