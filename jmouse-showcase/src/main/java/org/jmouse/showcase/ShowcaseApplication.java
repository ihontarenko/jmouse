package org.jmouse.showcase;

import java.util.Map;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.factories.WebServerFactoryConfiguration.WebServerConfigHolder;
import org.jmouse.web.mvc.WebApplicationLauncher;
import org.jmouse.web.mvc.servlet.FirewallBeanConfiguration;
import org.jmouse.web.security.firewall.RateLimitProperties;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerConfig;
import org.jmouse.web.server.WebServers;
import org.jmouse.web.server.tomcat.TomcatWebServer;
import org.jmouse.web.servlet.SessionProperties;

public final class ShowcaseApplication {

    private ShowcaseApplication() {}

    public static void main(String[] arguments) throws Exception {
        ShowcaseSettings settings = ShowcaseSettings.load();
        WebBeanContext context = new WebApplicationLauncher(ShowcaseApplication.class)
            .configureContext(root -> configureContext(root, settings))
            .launch(arguments);
        WebServer server = context.getBean(WebServer.class);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "Showcase-Shutdown"));
        System.out.println("Showcase: http://localhost:" + ((TomcatWebServer) server).port());
        System.out.println("Local credentials: " + settings.credentialFile());
        System.out.println("H2 file: " + settings.databaseFile());
    }

    private static void configureContext(WebBeanContext context, ShowcaseSettings settings) {
        context.registerBean(ShowcaseSettings.class, settings);
        context
            .getBean(SessionProperties.class)
            .setCookie(new SessionProperties.Cookie("JMOUSE_SHOWCASE", "/", -1, true, false));
        context
            .getBean(WebServerConfigHolder.class)
            .setWebServerConfigs(
                Map.of(
                    WebServers.TOMCAT,
                    new WebServerConfig(settings.port(), WebServers.TOMCAT, "target/tomcat")
                )
            );
        context
            .getBean(FirewallBeanConfiguration.Properties.class)
            .setTokenBucket(new RateLimitProperties(20, 60));
    }
}
