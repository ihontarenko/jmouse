package svit.web;

import org.apache.catalina.startup.Tomcat;
import svit.web.context.WebBeanContextLoaderInitializer;
import svit.web.server.WebServer;
import svit.web.server.tomcat.TomcatWebServer;
import svit.web.server.tomcat.TomcatWebServerConfigurer;

public class StartApplication {

    public static void main(String... arguments) {
        WebServer<Tomcat> webServer = new TomcatWebServer();

        webServer.configure(new TomcatWebServerConfigurer(
                9090, new FrameworkInitializer(), WebBeanContextLoaderInitializer.class));

        webServer.start();
    }

}
