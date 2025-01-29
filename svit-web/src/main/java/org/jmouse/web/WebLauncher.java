package org.jmouse.web;

import svit.beans.BeanContext;
import org.jmouse.web.server.WebServer;

public interface WebLauncher<C extends BeanContext> extends Launcher<C> {

    WebServer createWebServer();

}
