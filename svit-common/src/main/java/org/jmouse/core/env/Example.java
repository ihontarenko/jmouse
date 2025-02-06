package org.jmouse.core.env;

import java.util.Map;

public class Example {

    public static void main(String... arguments) {
        MapPropertySource source1 = new SystemEnvironmentPropertySource("sys.env");
        MapPropertySource source2 = new MapPropertySource("default", Map.of("app.name", "Svit", "app.server.port", "8899"));
        MapPropertySource source3 = new ClasspathPropertySource("package-info", "classpath:package-info.properties");

        PropertySourceRegistry registry = new DefaultPropertySourceRegistry();
        PropertyResolver       resolver = new StandardPropertyResolver(registry);

        Environment env = new StandardEnvironment(resolver);

        registry.addPropertySource(source1);
        registry.addPropertySource(source2);
        registry.addPropertySource(source3);

        env.getRegistry().addPropertySource(
                new MapPropertySource("extra", Map.of("site.name", "Svit Site", "site.port", "8080"))
        );

        System.out.println(env.getProperty("JAVA_HOME"));
        System.out.println(env.getProperty("app.name"));
        System.out.println(env.getProperty("package.name", "test"));
        System.out.println(env.getProperty("app.server.port", Integer.class, 8080));
        System.out.println(env.getProperty("site.port", Integer.class));

    }

}
