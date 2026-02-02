package org.jmouse.core.mapping.examples;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.mapping.MappingScope;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.errors.ErrorsPolicy;
import org.jmouse.core.reflection.InferredType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Smoke2 {

    private static final InferredType MAP_TYPE = InferredType.forParametrizedClass(
            Map.class, String.class, Object.class
    );

    public static Mapper getMapper() {
        TypeMappingRegistry mappingRegistry = TypeMappingRegistry.builder()
                .mapping(Map.class, Map.class, m -> m
                        .bind("name", "name"))
                // String -> Group record
                .mapping(String.class, Group.class, m -> m
//                                 .bind("name", (String s) -> s)
                         .compute("name", (src, ctx) -> {

                             MappingScope          scope    = ctx.scope();
                             ObjectAccessorWrapper wrapper  = ctx.wrapper();
                             ObjectAccessor        accessor = wrapper.wrap(scope.root());

                             System.out.println(scope.path());

                             return src;
                         })
                )
                .build();

        return Mappers.builder()
                .config(
                        MappingConfig.builder()
                                .errorsPolicy(
                                        ErrorsPolicy.builder()
                                                .build()
                                )
                                .build()
                )
                .registry(mappingRegistry)
                .build();
    }

    public static void main(String... arguments) {

        Map<String, Object> source = Map.of(
                "name", "John Doe",
                "id", 123,
                "nested", Map.of(
                        "userA", Map.of(
                                "name", "A",
                                "user", Map.of(
                                        "id", "334411",
                                        "groups", List.of("admin", "guest") // how to add mapping rules?
                                )
                        ),
                        "userB", Map.of("name", "A")
                )
        );

        Map<String, Object> target = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        Map<String, Object> nestedData = new LinkedHashMap<>();
        target.put("nested", nested);
        nested.put("userA", nestedData);
        nestedData.put("id", "123");
        nestedData.put("user", new User() {{ setName("Jane"); }});

        getMapper().map(source, MAP_TYPE, target);

    }

    public static class User {

        private String  name;
        private int     id;
        private Group[] groups; // how to add mapping rules?

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Group[] getGroups() {
            return groups;
        }

        public void setGroups(Group[] groups) {
            this.groups = groups;
        }
    }

    public record Group(String name) {}

}
