package org.jmouse.context.bind.example;

import org.jmouse.context.bind.PropertyPath;

import java.util.List;
import java.util.Map;

public class WebServerConfig {

    private String              name;
    private int                 port;
    private String              host;
    private List<String>        alias;
    private Map<String, Object> properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<String> getAlias() {
        return alias;
    }

    public void setAlias(List<String> alias) {
        this.alias = alias;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @PropertyPath("props")
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "WebServerConfig{" +
                "name='" + name + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", alias=" + alias +
                ", properties=" + properties +
                '}';
    }
}
