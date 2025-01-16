package svit.web.configuration;

import jakarta.servlet.ServletContainerInitializer;
import svit.beans.annotation.Configuration;
import svit.beans.annotation.Provide;
import svit.web.server.WebServerFactory;
import svit.web.server.tomcat.TomcatWebServerFactory;

@Configuration(name = "webServerFactoryConfiguration")
public class ServletWebServerFactoryConfiguration {

    @Provide
    public WebServerFactory createWebServerFactory(ServletContainerInitializer containerInitializer) {
        return new TomcatWebServerFactory();
    }

}
