package org.jmouse.web.server;

import org.jmouse.core.bind.BindName;

import java.util.Map;

public class WebServerConfigHolder {

    private Map<WebServers, WebServerConfig> webserversConfiguration;
    private WebServers defaultWebServer;

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
