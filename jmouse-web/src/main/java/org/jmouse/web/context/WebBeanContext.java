package org.jmouse.web.context;

import jakarta.servlet.ServletContext;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.beans.BeanContext;

import java.util.Enumeration;

/**
 * Represents a web-specific extension of the {@link BeanContext}.
 * Supports servlet-specific configurations and scoped beans.
 */
public interface WebBeanContext extends ApplicationBeanContext {

    /**
     * DirectAccess name for root web context
     */
    String DEFAULT_ROOT_WEB_CONTEXT_NAME = "WEB-ROOT";

    /**
     * DirectAccess name for child (application) web context
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

    static WebBeanContext getRequiredWebBeanContext(ServletContext servletContext) {
        WebBeanContext rootContext = getRootWebBeanContext(servletContext);

        if (rootContext == null) {
            throw new WebContextException("No WebBeanContext[%s] found.".formatted(DEFAULT_ROOT_WEB_CONTEXT_NAME));
        }

        return rootContext;
    }

    static WebBeanContext getRootWebBeanContext(ServletContext servletContext) {
        return (WebBeanContext) servletContext.getAttribute(ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE);
    }

    static WebBeanContext findWebBeanContext(ServletContext servletContext) {
        WebBeanContext webBeanContext = getRootWebBeanContext(servletContext);

        if (webBeanContext == null) {
            Enumeration<String> attributeNames = servletContext.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                if (servletContext.getAttribute(attributeNames.nextElement()) instanceof WebBeanContext context) {
                    webBeanContext = context;
                    break;
                }
            }
        }

        return webBeanContext;
    }

}
