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

        binder.bind("application.clients.default", Bindable.of(String.class));
        binder.bind("application.users", Bindable.of(JavaType.forParametrizedClass(List.class, Customer.class)));

        System.out.println("end!");

    }


}
