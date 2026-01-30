package org.jmouse.core.mapping.examples;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.reflection.InferredType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Smoke2 {

    private static final InferredType MAP_TYPE = InferredType.forParametrizedClass(
            Map.class, String.class, Object.class
    );

    public static Mapper getMapper() {
        TypeMappingRegistry mappingRegistry = TypeMappingRegistry.builder()
                .mapping(Map.class, Map.class, m -> m
                        .bind("name", "name"))
                .build();

        return Mappers.builder()
                .registry(mappingRegistry)
                .build();
    }

    public static void main(String... arguments) {

        Map<String, Object> source = Map.of(
                "name", "John Doe",
                "id", 123,
                "nested", Map.of(
                        "userA", Map.of("name", "A"),
                        "userB", Map.of("name", "A")
                )
        );

        Map<String, Object> target = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        Map<String, Object> nestedData = new LinkedHashMap<>();
        target.put("nested", nested);
        nested.put("userA", nestedData);
        nestedData.put("id", "123");

        getMapper().map(source, MAP_TYPE, target);

    }

    public static class User {

        private String  name;
        private int     id;
        private Group[] groups;

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
