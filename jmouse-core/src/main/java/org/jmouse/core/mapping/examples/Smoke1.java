package org.jmouse.core.mapping.examples;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.ValueObject;
import org.jmouse.core.mapping.*;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.binding.annotation.AnnotationRuleSource;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.reflection.InferredType;

import java.util.*;

public class Smoke1 {

    public static void main(String... arguments) {
        Mapper mapper = Mappers.builder()
                .config(MappingConfig.builder()
                                .listFactory(LinkedList::new)
                                .setFactory(TreeSet::new)
                                .build())
                .registry(TypeMappingRegistry.builder()
                                  .ruleSource(new AnnotationRuleSource())
                          .mapping(Map.class, UserDTO.class, m -> m
                                  .reference("user", "user")
                                  .reference("groups", "group")
                                  .reference("groupSet", "group")
                                  .ignore("password")
                                  .constant("password", "masked")
                          ).mapping(Map.class, UserDetailsDTO.class, m -> m
//                                .bind("groupSet", "group")
                                .compute("groupSet", (source, context) -> {

                                    ObjectAccessor object = context.wrapper().wrap(
                                            context.scope().root()
                                    );

                                    if (object.length() > 0) {
                                        return object.get("group").unwrap();
                                    }

                                    return null;
                                })
                        ).build()).build();

        Map<String, Object> source = Map.of(
                "user", "john",
                "password", "secret",
                "group", List.of("a", "b", "c", "a"),
                "groups", new String[]{"B"},
                "details", Map.of(
                        "status", "BLOCKED",
                        "dateOfBirth", Map.of("dateOfBirth", new Date())
                )
        );

//        User    user    = mapper.map(source, User.class);
//        UserDTO userDTO = mapper.map(source, UserDTO.class);
        User userObject = mapper.map(source, User.class);

        Map<User, String> sourceMap = Map.of(new User("John"), "User-123");

        InferredType inferredType = InferredType.forParametrizedClass(Map.class, SimpleUserDTO.class, String.class);

        Map<Object, Object> mapped = new WeakHashMap<>();

        mapper.map(sourceMap, inferredType, mapped);
        InferredType mapType = InferredType.forParametrizedClass(Map.class, String.class, Object.class);

        Map<Object, Object> mapTarget = new WeakHashMap<>();
        Map<Object, Object> nestedA = new WeakHashMap<>();

        List<String> strings = new LinkedList<>();

        mapTarget.put("details", nestedA);
        nestedA.put("groupSet", strings);

        strings.add("x");

//        mapper.map(userDTO, TypedValue.of(mapType).withInstance(mapTarget));



        System.out.println("aaa");

//        System.out.println(userDTO);
    }

    public enum Group {
        A, B, C
    }

    public static class User {
        private String user;
        private Group[] groups = new Group[]{Group.A};

        public User() {
        }

        public User(String user) {
            this.user = user;
        }

        public Group[] getGroups() {
            return groups;
        }

        public void setGroups(Group[] groups) {
            this.groups = groups;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }
    }

    public record UserDTO(String user, String password, UserDetailsDTO details, Group[] groups) {}
    public record SimpleUserDTO(String user) {}
    public record UserDetailsDTO(DateOfBirth dateOfBirth, Status status, Set<Group> groupSet) {}

    public enum Status { BLOCKED, UNBLOCKED }

    public static class DateOfBirth {
        private Date dateOfBirth;
        public Date getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    }
}
