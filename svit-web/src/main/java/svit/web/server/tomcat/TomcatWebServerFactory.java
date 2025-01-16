package svit.web.server.tomcat;

import svit.web.ApplicationInitializer;
import svit.web.server.WebServer;
import svit.web.server.WebServerFactory;

public class TomcatWebServerFactory implements WebServerFactory {

    @Override
    public WebServer getWebServer(ApplicationInitializer... initializers) {
        return new TomcatWebServer();
    }

}
