package org.jmouse.context.binding.test;

import org.jmouse.context.binding.*;
import org.jmouse.core.env.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {

    public static final List<User> CLIENTS = new ArrayList<>();

    public static void main(String[] args) {
//
//        JavaType javaType = JavaType.forTypeReference(new TypeReference<List<Map<String, Integer>>>() {});
//
//        Map<String, Object> source = new HashMap<>() {{
//            put("application", Map.of(
//                    "names", List.of("app1", "app2"),
//                    "clients", List.of(
//                            Map.of("name", "Default Client"),
//                            Map.of("name", "Client B")
//                    ),
//                    "users", List.of(
//                            new User("John", 13),
//                            new User("Joe", 33)
//                    )
//                )
//            );
//        }};
//
//        DataSource dataSource = new JavaTypeDataSource(source);
//
//        System.out.println(dataSource.get(NamePath.of("application.users[0]")));
//
//        Binder binder = new Binder(dataSource);
//
//        // binder.bind("application.names", Bindable.of(Object.class));
////        binder.bind("application.clients", Bindable.of(JavaType.forParametrizedClass(List.class, User.class)).withInstance(CLIENTS));
////        binder.bind("application.names", Bindable.of(List.class));
////        binder.bind("application.names", Bindable.of(String[].class));
////        binder.bind("application.clients.default", Bindable.of(String.class));
////        binder.bind("application.names", Bindable.of(JavaType.forParametrizedClass(List.class, String.class)));
//
//
//        User user = new User("PARENT!!!", 13);
//
//        user.getAccess().add(123);
//
//        Map<String, Object> data = Map.of(
//                "server", Map.of(
//                        "port", 8088,
//                        "name", "Embedded Server",
//                        "customer", Map.of(
//                                "name", "John Smith",
//                                "user", user
//                        )
//                ));
//

        PropertyResolver    resolver = createPropertyResolver();
        Map<String, Object> flatMap  = new HashMap<>();

        for (PropertySource<?> source : resolver.getRegistry().getPropertySources()) {
            if (source.getName().equals("default")) {
                for (String name : source.getPropertyNames()) {
                    flatMap.put(name, source.getProperty(name));
                }
            }
        }

        NamePath name = NamePath.of(null);

        name.prefix("app.server");

        System.out.println(name);

        Map<String, Object> nested = PropertiesTransformer.transform(flatMap);
        Binder b = new Binder(DataSource.of(nested));
        DataSource source1 = DataSource.of(nested);

        new Binder(DataSource.of("123")).bind(null, Bindable.of(Integer.class));

        UserContext userContext = new UserContext(source1);
        userContext.initialize();

        System.out.println("end!");

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

    static class UserContext {

        private       int           port;
        private final Binder        binder;
        private       List<Service> services;
        private String        javaHome;

        public UserContext(DataSource source) {
            binder = new Binder(source);
        }

        public void initialize() {
            binder.bind("app.context", Bindable.ofInstance(this));
            binder.bind(null, Bindable.ofInstance(this));
            binder.bind("JAVA_HOME", Bindable.of(String.class)).ifPresent(this::setJavaHome);
        }

        public String getJavaHome() {
            return javaHome;
        }

        public void setJavaHome(String javaHome) {
            this.javaHome = javaHome;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public List<Service> getServices() {
            return services;
        }

        public void setServices(List<Service> services) {
            this.services = services;
        }
    }


}
