package org.jmouse.web;

import org.jmouse.beans.BeanContext;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.server.WebServer;

public interface WebLauncher<C extends BeanContext> extends Launcher<C> {

    WebServer createWebServer(WebBeanContext rootContext);

}
