package org.jmouse.web.configuration;

import org.jmouse.testing_ground.beans.annotation.Configuration;
import org.jmouse.testing_ground.beans.annotation.Provide;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.server.tomcat.TomcatWebServerFactory;

@Configuration(name = "webServerFactoryConfiguration")
public class WebServerFactoryConfiguration {

    @Provide(proxied = true, value = "webServerFactory")
    public WebServerFactory createWebServerFactory() {
        return new TomcatWebServerFactory();
    }

}
