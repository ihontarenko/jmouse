package org.jmouse.context.binding.example;

import org.jmouse.context.binding.*;
import org.jmouse.core.env.*;

import java.util.*;

public class Example {

    public static void main(String[] args) {

//        PropertyResolver    resolver = createPropertyResolver();
//        Map<String, Object> flatMap  = new HashMap<>();
//
//        for (PropertySource<?> source : resolver.getRegistry().getPropertySources()) {
//            for (String name : source.getPropertyNames()) {
//                flatMap.put(name, source.getProperty(name));
//            }
//        }
//
//        Map<String, Object> nested = PropertiesTransformer.transform(flatMap);

        Map<String, Object> data = new HashMap<>();
        data.put("default", Map.of("data", new RandomProvider()));

        Binder binder = new Binder(DataSource.of(data));

        AppConfig appConfig = binder.bind("default", Bindable.of(AppConfig.class)).getValue();

        System.out.println(appConfig.getDefaultWebServerConfig().getPort());
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
