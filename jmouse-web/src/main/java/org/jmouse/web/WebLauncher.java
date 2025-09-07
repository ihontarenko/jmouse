package org.jmouse.web;

import org.jmouse.beans.BeanContext;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.server.WebServer;

/**
 * 🚀 Entry point contract for launching a web application.
 *
 * <p>Extends {@link Launcher} with web-specific bootstrapping:
 * creating and starting a {@link WebServer} bound to a {@link WebBeanContext}.</p>
 *
 * @param <C> the type of {@link BeanContext} used as application root
 */
public interface WebLauncher<C extends BeanContext> extends Launcher<C> {

    /**
     * 🌐 Create a {@link WebServer} instance for the given root context.
     *
     * @param rootContext initialized {@link WebBeanContext}
     * @return created web server ready to start
     */
    WebServer createWebServer(WebBeanContext rootContext);

}
