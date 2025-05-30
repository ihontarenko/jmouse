package org.jmouse.web.configuration;

import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.BindName;
import org.jmouse.web.server.WebServerConfig;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.server.WebServers;
import org.jmouse.web.server.tomcat.TomcatWebServerFactory;

import java.util.Map;

@Configuration(name = "webServerFactoryConfiguration")
public class WebServerFactoryConfiguration {

    @Provide(proxied = true, value = "webServerFactory")
    public WebServerFactory createWebServerFactory() {
        return new TomcatWebServerFactory();
    }

    @BeanProperties("jmouse.web.server")
    public static class WebServerConfigHolder {

        private Map<WebServers, WebServerConfig> webserversConfiguration;
        private WebServers                       defaultWebServer;

        @BindName("configuration")
        public void setWebServerConfigs(Map<WebServers, WebServerConfig> webserversConfiguration) {
            this.webserversConfiguration = webserversConfiguration;
        }

        public WebServerConfig getWebServerConfig(WebServers serverType) {
            return webserversConfiguration.get(serverType);
        }

        public WebServers getDefaultWebServer() {
            return defaultWebServer;
        }

        @BindName("default")
        public void setDefaultWebServer(WebServers defaultWebServer) {
            this.defaultWebServer = defaultWebServer;
        }

        public WebServerConfig getDefaultWebServerConfiguration() {
            return getWebServerConfig(getDefaultWebServer());
        }

    }

}
