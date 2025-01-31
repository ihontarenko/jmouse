package org.jmouse.context.binding.test;

import org.jmouse.context.binding.*;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {

    public static void main(String[] args) {

        JavaType javaType = JavaType.forTypeReference(new TypeReference<List<Map<String, Integer>>>() {});


        Map<String, Object> source = new HashMap<>() {{
            put("application", Map.of(
                    "names", List.of("app1", "app2"),
                    "clients", Map.of("default", "Default Client"),
                    "users", List.of(
                            new User("John", 13),
                            new User("Joe", 33)
                    )
                )
            );
        }};

        DataSource dataSource = new JavaTypeDataSource(source);

        System.out.println(dataSource.get(NamePath.of("application.users[0]")));

        Binder binder = new Binder(dataSource);

//        binder.bind("application.names", Bindable.of(List.class));
//        binder.bind("application.names", Bindable.of(String[].class));
//        binder.bind("application.clients.default", Bindable.of(String.class));
//        binder.bind("application.names", Bindable.of(JavaType.forParametrizedClass(List.class, String.class)));


        User user = new User("PARENT!!!", 13);

        user.getAccess().add(123);

        Map<String, Object> data = Map.of(
                "server", Map.of(
                        "port", 8088,
                        "name", "Embedded Server",
                        "customer", Map.of(
                                "name", "John Smith",
                                "user", user
                        )
                ));

        UserContext userContext = new UserContext(DataSource.of(data));

        userContext.initialize();

        user.getAccess().add(555);

        System.out.println("end!");

    }

    static class UserContext {

        private int port;
        private Binder binder;
        private Customer customer;

        public UserContext(DataSource source) {
            binder = new Binder(source);
        }

        public void initialize() {
            binder.bind("server", Bindable.ofInstance(this));
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public Customer getCustomer() {
            return customer;
        }

        public void setCustomer(Customer customer) {
            this.customer = customer;
        }
    }


}
