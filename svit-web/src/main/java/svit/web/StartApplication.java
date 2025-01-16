package svit.web;

import svit.web.context.ApplicationContext;
import svit.web.server.WebServer;

public class StartApplication {

    public static void main(String... arguments) {
        ApplicationContext context = WebApplicationLauncher.launch(StartApplication.class);

        WebServer webServer = context.getBean(WebServer.class);

        System.out.println("test");
    }

}
