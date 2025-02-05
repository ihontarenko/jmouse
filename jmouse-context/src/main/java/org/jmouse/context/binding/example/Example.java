package org.jmouse.context.binding.example;

import org.jmouse.context.ValueFlow;
import org.jmouse.context.binding.*;
import org.jmouse.core.env.*;

import java.util.*;

public class Example {

    public static void main(String[] args) {

        PropertyResolver    resolver = createPropertyResolver();
        Map<String, Object> flatMap  = new HashMap<>();

        for (PropertySource<?> source : resolver.getRegistry().getPropertySources()) {
            for (String name : source.getPropertyNames()) {
                flatMap.put(name, source.getProperty(name));
            }
        }

        Map<String, Object> data   = PropertiesTransformer.transform(flatMap);
        Binder              binder = Binder.with(data);

        Bind.with(binder).toMap(null, String.class);
        Bind.with(binder).to("JAVA_HOME", Object.class);
        Bind.with(binder).to("JAVA_HOME", String.class);
        Bind.with(binder).toInt("app.context.port").ifPresent(value -> {
            System.out.println(value.byteValue());
        });

        UserKeeper userKeeper = new UserKeeper(new User("John Wik"), new User("John Doe"));

        Bind.with(userKeeper).toMap();

        Bind.with(binder).toMap("services", Object.class);

        System.out.println("Binding " + binder);
    }

    public static class UserKeeper {

        private User        admin;
        private User        client;

        public UserKeeper(User admin, User client) {
            this.admin = admin;
            this.client = client;
        }

        public User getAdmin() {
            return admin;
        }

        public void setAdmin(User admin) {
            this.admin = admin;
        }

        public User getClient() {
            return client;
        }

        public void setClient(User client) {
            this.client = client;
        }
    }

    public static class User {
        private String name;
        private Double port;

        public User() {
        }

        public User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getPort() {
            return port;
        }

        public void setPort(Double port) {
            this.port = port;
        }
    }

    private static PropertyResolver createPropertyResolver() {
        MapPropertySource source1 = new SystemEnvironmentPropertySource("sys.env");
        MapPropertySource source2 = new MapPropertySource("rawMaps", Map.of("app.name", "Svit", "app.server.port", "8899"));
        MapPropertySource source3 = new ClasspathPropertySource("default", "classpath:services.properties");

        PropertySourceRegistry registry = new DefaultPropertySourceRegistry();
        PropertyResolver       resolver = new StandardPropertyResolver(registry);

        Environment env = new StandardEnvironment(resolver);

        registry.addPropertySource(source1);
        registry.addPropertySource(source2);
        registry.addPropertySource(source3);

        return resolver;
    }



}
