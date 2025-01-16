package svit.web.server;

import svit.web.ApplicationInitializer;

public interface WebServerFactory {

    WebServer getWebServer(ApplicationInitializer... initializers);

}
