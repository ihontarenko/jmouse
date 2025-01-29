package org.jmouse.web.configuration;

import svit.beans.annotation.Configuration;
import svit.beans.annotation.Provide;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.server.tomcat.TomcatWebServerFactory;

@Configuration(name = "webServerFactoryConfiguration")
public class ServletWebServerFactoryConfiguration {

    @Provide(proxied = true, value = "webServerFactory")
    public WebServerFactory createWebServerFactory() {
        return new TomcatWebServerFactory();
    }

}
