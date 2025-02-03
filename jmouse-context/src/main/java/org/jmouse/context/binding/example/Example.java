package org.jmouse.context.binding.example;

import org.jmouse.context.ValueFlow;
import org.jmouse.context.binding.*;
import org.jmouse.core.env.*;
import org.jmouse.core.reflection.JavaType;

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

        Map<String, Object> data = PropertiesTransformer.transform(flatMap);
        Binder binder = new Binder(DataSource.of(data));

        var type = Bindable.ofMap(String.class, WebServerConfig.class);

        ValueFlow.get()
                .create(binder.bind("default.configs", type)::getValue)
                .when(Objects::nonNull)
                .as(stringWebServerConfigMap -> stringWebServerConfigMap.get("tomcat"))
                .toConsume(System.out::println);
        System.out.println(false);
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
