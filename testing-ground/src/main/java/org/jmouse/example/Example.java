package org.jmouse.example;


import org.jmouse.core.bind.*;
import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.util.StandardPlaceholderReplacer;
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

        Bind.with(binder).toMap("package", Package.class);

        Bind.with(binder).toMap("services", Object.class);
    }

    public record Package(String name, String version) {}

    private static PropertyResolver createPropertyResolver() {

        PatternMatcherResourceLoader resourceLoader = new CompositeResourceLoader();

        MapPropertySource mapSource = new MapPropertySource("rawMaps", Map.of(
                "app.name", "Svit",
                "app.full-name", "jMouse - Svit Framework",
                "app.java.version", "21",
                "app.description", "Application: '${app.name}' Java ${app.java.version} ${app.userName:Default}"
        ));

        PropertySourceRegistry registry = new DefaultPropertySourceRegistry();
        PropertyResolver       resolver = new StandardPropertyResolver(registry);

        registry.addPropertySource(mapSource);


        for (Resource resource : resourceLoader.findResources("classpath:**/package.properties")) {
            registry.addPropertySource(new ResourcePropertySource("source-" + resource.getName(), resource));
        }

        return resolver;
    }



}
