package org.jmouse.core.mapping.examples;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;

import java.time.Instant;
import java.util.Date;

public class Smoke3 {

    public static void main(String... arguments) {
        Mapper mapper = Mappers.builder()
                .registry(TypeMappingRegistry.builder()
                                  .mapping(UserA.class, UserB.class, m -> m
                                          .property("birthDay", builder -> builder
                                                  .reference("dateOfBirth")
                                          )
                                          .property("name", p -> p
                                                  .transform((value, ctx) -> String.valueOf(value).toUpperCase())
                                          )
                                  )
                                  .mapping(UserB.class, UserA.class, m -> m
                                          .property("dateOfBirth", p -> p
                                                  .reference("birthDay")
                                          )
                                  )
                                  .build())
                .build();

        UserA userA = new UserA();
        userA.setId(123);
        userA.setDateOfBirth(Instant.now());
        userA.setName("John Doe");

        UserB userB = mapper.map(userA, UserB.class);
        UserA user = mapper.map(userB, UserA.class);

        System.out.println(userB);

    }

    public static class UserA {

        private String name;
        private Integer id;
        private Instant dateOfBirth;

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

        public Instant getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(Instant dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }
    }

    public static class UserB {

        private String name;
        private int  id = 0;
        private Date birthDay;

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

        public Date getBirthDay() {
            return birthDay;
        }

        public void setBirthDay(Date birthDay) {
            this.birthDay = birthDay;
        }
    }

}
