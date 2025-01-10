package svit.web.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.web.context.WebApplicationBeanContext;
import svit.web.context.WebBeanContext;

import static svit.reflection.Reflections.getShortName;

/**
 * Listener for loading and managing the {@link WebBeanContext} within a {@link ServletContext}.
 * It initializes the web application bean context during the servlet context initialization phase
 * and cleans it up during the destruction phase.
 */
public class WebBeanContextLoaderListener implements ServletContextListener {

    /**
     * Logger for logging context loading process
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebBeanContextLoaderListener.class);

    /**
     * Initializes the {@link WebBeanContext} and sets it as a servlet context attribute.
     *
     * @param event the {@link ServletContextEvent} containing the servlet context.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        WebBeanContext webBeanContext = createWebBeanContext(servletContext);

        webBeanContext.setServletContext(servletContext);
    }

    /**
     * Cleans up the {@link WebBeanContext} by removing it from the servlet context attributes.
     *
     * @param event the {@link ServletContextEvent} containing the servlet context.
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        LOGGER.info("detaching '{}' bean context", WebBeanContext.ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE);
        ServletContext servletContext = event.getServletContext();
        servletContext.removeAttribute(WebBeanContext.ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE);
    }

    /**
     * Creates and configures a new {@link WebBeanContext} for the given {@link ServletContext}.
     * <p>
     * This method:
     * <ol>
     *   <li>Instantiates a new {@link WebApplicationBeanContext}.</li>
     *   <li>Attaches it to the {@code ServletContext} under the {@link WebBeanContext#ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE} key.</li>
     *   <li>Logs the attach process for debugging.</li>
     *   <li>Refreshes the {@link WebBeanContext} and logs both the start and finish of the refresh process.</li>
     * </ol>
     *
     * @param servletContext the {@link ServletContext} to which the {@link WebBeanContext} is attached.
     * @return the newly created and refreshed {@link WebBeanContext}.
     */
    private WebBeanContext createWebBeanContext(ServletContext servletContext) {
        WebBeanContext webBeanContext = new WebApplicationBeanContext();
        String         webContextName = getShortName(webBeanContext.getClass());

        // Set the WebBeanContext as a servlet context attribute
        servletContext.setAttribute(WebBeanContext.ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE, webBeanContext);

        LOGGER.info("Attach '{}' to the main ServletContext '{}' attribute",
                    webContextName, WebBeanContext.ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE);

        // Configure and refresh the WebBeanContext
        LOGGER.info("Refresh {} started!", webContextName);
        webBeanContext.refresh();
        LOGGER.info("Refresh {} finished!", webContextName);

        return webBeanContext;
    }

}
