package org.jmouse.mapper.examples;

import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.strategy.MappingStrategyRegistry;
import org.jmouse.mapper.strategy.direct.TypeMapperStrategyContributor;
import org.jmouse.mapper.typed.TypeMapper;
import org.jmouse.core.reflection.InferredType;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class Smoke3 {

    public static void main(String... arguments) {
        Mapper mapper = Mappers.builder()
                .rules(rules -> rules
                        .mapping("user", new UserBA())
                        .mapping(UserA.class, UserB.class, user -> user
                                .rename("dateOfBirth", "birthDay")
                                .transformer("name", (value, context) -> String.valueOf(value).toUpperCase())
                        )
                        .mapping(UserB.class, UserA.class, user -> user
                                .rename("birthDay", "dateOfBirth")
                        )
                )
                .strategyRegistry(
                        new MappingStrategyRegistry(Mappers.DEFAULT_CONTRIBUTORS)
                                .register(new TypeMapperStrategyContributor(
                                        new UserBA()
                                ))
                )
                .build();

        UserA userA = new UserA();
        userA.setId(123);
        userA.setDateOfBirth(Instant.now());
        userA.setName("John Doe");

        UserB userB = mapper.map(userA, UserB.class);
        UserA user = mapper.map(userB, new UserA());

        DataObject dataObject = new DataObject("parent_url");
        dataObject.setDepth(1);
        dataObject.setUrl("https://google.com/");

        DataObject object = mapper.map(Map.of("parent", "parent value!"), TypedValue.of(DataObject.class));
        object.getParent(); // parent value!
        mapper.map(user, InferredType.forParametrizedClass(Map.class, String.class, Object.class));

        System.out.println(userB);

    }

    public static class UserBA implements TypeMapper<UserB, UserA> {

        @Override
        public Class<UserB> sourceType() {
            return UserB.class;
        }

        @Override
        public Class<UserA> targetType() {
            return UserA.class;
        }

        @Override
        public UserA map(UserB source) {
            UserA target = new UserA();

            map(source, target);

            return target;
        }

        @Override
        public void map(UserB source, UserA target) {
            target.setId(source.getId());
            target.setName(source.getName());
        }

        @Override
        public boolean supportsInPlace() {
            return true;
        }
    }

    public static class DataObject {

        private String url;
        private String parent;
        private int    depth;

        public DataObject() {}

        public DataObject(String parent) {
            this.parent = parent;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }
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
