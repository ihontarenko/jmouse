package org.jmouse.web.context;

import jakarta.servlet.ServletContext;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.testing_ground.beans.BeanContext;

/**
 * Represents a web-specific extension of the {@link BeanContext}.
 * Supports servlet-specific configurations and scoped beans.
 */
public interface WebBeanContext extends ApplicationBeanContext {

    /**
     * Default name for root web context
     */
    String DEFAULT_ROOT_WEB_CONTEXT_NAME = "WEB-ROOT";

    /**
     * Default name for child (application) web context
     */
    String DEFAULT_WEB_CONTEXT_NAME      = "WEB-APPLICATION";

    /**
     * Context attribute to bind {@link WebBeanContext} on {@link ServletContext}
     */
    String ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE = WebBeanContext.class.getName() + ".ROOT";

    /**
     * Context attribute to bind {@link jakarta.servlet.http.HttpServletRequest} to {@link WebBeanContext}
     */
    String CURRENT_REQUEST = WebBeanContext.class.getName() + ".CURRENT_REQUEST";

    /**
     * Sets the {@link ServletContext} for this web context.
     *
     * @param servletContext the servlet context
     */
    void setServletContext(ServletContext servletContext);

    /**
     * Retrieves the {@link ServletContext}.
     *
     * @return the servlet context
     */
    ServletContext getServletContext();

}
