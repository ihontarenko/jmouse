package svit.web.configuration;

import svit.beans.annotation.Configuration;
import svit.beans.annotation.Provide;
import svit.web.server.WebServer;
import svit.web.server.WebServerFactory;

@Configuration
public class ServletWebServerConfiguration {

    @Provide
    public WebServer webServer(WebServerFactory webServerFactory) {
        return webServerFactory.getWebServer();
    }

}
