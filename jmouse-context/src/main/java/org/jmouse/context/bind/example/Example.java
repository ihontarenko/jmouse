package org.jmouse.context.bind.example;

import org.jmouse.context.bind.*;
import org.jmouse.core.StandardPlaceholderReplacer;
import org.jmouse.core.env.PropertyResolver;
import org.jmouse.core.env.*;
import org.jmouse.util.Strings;

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
        Binder              binder = Binder.with(data, new DefaultBindingCallback(new StandardPlaceholderReplacer()));

        Bind.with(binder).toMap(null, String.class);
        Bind.with(binder).to("JAVA_HOME", Object.class);
        Bind.with(binder).to("JAVA_HOME", String.class);
        Bind.with(binder).toInt("app.context.port").ifPresent(value -> {
            System.out.println(value * 2);
        });

        Bind.with(binder).toMap("services", Object.class);

        Bind.with(binder).to("app", Application.class);

        System.out.println(
                Strings.underscored("aaBbCc")
        );
    }


    public static class Application {
        private String name;
        private String fullName;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFullName() {
            return fullName;
        }

        @PropertyPath("full-name")
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    private static PropertyResolver createPropertyResolver() {
        MapPropertySource source2 = new MapPropertySource("rawMaps", Map.of(
                "app.name", "Svit",
                "app.full-name", "JMouse - Svit Framework",
                    "app.java.version", "21",
                "app.description", "Application: '${app.name}' Java ${app.java.version} ${app.userName:Default}"
        ));
        MapPropertySource source3 = new ClasspathPropertySource("default", "classpath:services.properties");

        PropertySourceRegistry registry = new DefaultPropertySourceRegistry();
        PropertyResolver       resolver = new StandardPropertyResolver(registry);

        Environment env = new StandardEnvironment(resolver);

//        registry.addPropertySource(new SystemEnvironmentPropertySource("sys.env"));
        registry.addPropertySource(source2);
        registry.addPropertySource(source3);

        return resolver;
    }



}
