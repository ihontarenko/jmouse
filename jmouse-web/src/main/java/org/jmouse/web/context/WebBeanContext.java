package org.jmouse.web.context;

import jakarta.servlet.ServletContext;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.beans.BeanContext;

import java.util.Enumeration;

/**
 * 🌐 Web-specific {@link BeanContext}.
 * <p>
 * Adds servlet-related functionality and scoped beans for HTTP/web layers.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public interface WebBeanContext extends ApplicationBeanContext {

    /**
     * 🌍 Name of root web context.
     */
    String DEFAULT_ROOT_WEB_CONTEXT_NAME = "WEB-ROOT";

    /**
     * 🧩 Name of child web context.
     */
    String DEFAULT_WEB_CONTEXT_NAME = "WEB-APPLICATION";

    /**
     * 🪝 Servlet attribute key to store root context.
     */
    String ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE = WebBeanContext.class.getName() + ".ROOT";

    /**
     * 🔗 Servlet attribute key to bind request to context.
     */
    String CURRENT_REQUEST = WebBeanContext.class.getName() + ".CURRENT_REQUEST";

    /**
     * 🔧 Attach {@link ServletContext}.
     *
     * @param servletContext current servlet context
     */
    void setServletContext(ServletContext servletContext);

    /**
     * 📥 Get the attached {@link ServletContext}.
     *
     * @return servlet context instance
     */
    ServletContext getServletContext();

    /**
     * 🚨 Get required root web context or throw.
     */
    static WebBeanContext getRequiredWebBeanContext(ServletContext servletContext) {
        WebBeanContext rootContext = getRootWebBeanContext(servletContext);
        if (rootContext == null) {
            throw new WebContextException("No WebBeanContext[%s] found.".formatted(DEFAULT_ROOT_WEB_CONTEXT_NAME));
        }
        return rootContext;
    }

    /**
     * 🧱 Get root context by known attribute name.
     */
    static WebBeanContext getRootWebBeanContext(ServletContext servletContext) {
        return (WebBeanContext) servletContext.getAttribute(ROOT_WEB_BEAN_CONTEXT_ATTRIBUTE);
    }

    /**
     * 🔍 Try to find any {@link WebBeanContext} within attributes.
     */
    static WebBeanContext findWebBeanContext(ServletContext servletContext) {
        WebBeanContext webBeanContext = getRootWebBeanContext(servletContext);

        if (webBeanContext == null) {
            Enumeration<String> names = servletContext.getAttributeNames();
            while (names.hasMoreElements()) {
                String name      = names.nextElement();
                Object attribute = servletContext.getAttribute(name);

                if (attribute instanceof WebBeanContext context) {
                    webBeanContext = context;
                    break;
                }
            }
        }

        return webBeanContext;
    }
}
