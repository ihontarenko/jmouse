package org.jmouse.testing_ground.example;

import org.jmouse.core.bind.PropertyPath;

import java.util.List;

public class AppConfig {

    private List<WebServerConfig> webServerConfig;
    private WebServerConfig defaultWebServerConfig;
    private List<String> hosts;
    private Data data;

    public List<WebServerConfig> getWebServerConfig() {
        return webServerConfig;
    }

    @PropertyPath("configs")
    public void setWebServerConfig(List<WebServerConfig> webServerConfig) {
        this.webServerConfig = webServerConfig;
    }

    public WebServerConfig getDefaultWebServerConfig() {
        return defaultWebServerConfig;
    }

    @PropertyPath("webserver")
    public void setDefaultWebServerConfig(WebServerConfig defaultWebServerConfig) {
        this.defaultWebServerConfig = defaultWebServerConfig;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }


    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
