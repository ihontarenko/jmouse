package svit.web.context;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import svit.util.Priority;
import svit.web.ApplicationInitializer;

@Priority(-1000)
public class WebApplicationDefaultServicesInitializer implements ApplicationInitializer {

    /**
     * Called during the startup phase of the servlet context.
     * Implementations should provide initialization logic, such as configuring listeners, filters, or servlets.
     *
     * @param servletContext the {@link ServletContext} being initialized.
     * @throws ServletException if any error occurs during initialization.
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

    }

}
