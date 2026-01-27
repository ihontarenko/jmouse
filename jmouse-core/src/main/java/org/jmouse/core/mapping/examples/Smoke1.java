package org.jmouse.core.mapping.examples;

import org.jmouse.core.bind.BinderConversion;
import org.jmouse.core.bind.StandardAccessorWrapper;
import org.jmouse.core.mapping.binding.MappingRulesRegistry;
import org.jmouse.core.mapping.binding.TypeMappingBuilder;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.plan.MappingPlanRegistry;
import org.jmouse.core.mapping.plan.array.ArrayPlanContributor;
import org.jmouse.core.mapping.plan.bean.JavaBeanPlanContributor;
import org.jmouse.core.mapping.plan.collection.CollectionPlanContributor;
import org.jmouse.core.mapping.plan.map.MapPlanContributor;
import org.jmouse.core.mapping.plan.record.RecordPlanContributor;
import org.jmouse.core.mapping.plan.scalar.ScalarPlanContributor;
import org.jmouse.core.mapping.runtime.Mapper;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.mapping.runtime.ObjectMapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Smoke1 {

    public static void main(String... arguments) {
        var registry = MappingRulesRegistry.builder()
                .register(
                        new TypeMappingBuilder<>(Map.class, Smoke1.UserDTO.class)
                                .bind("user", "username")
                                .ignore("password")
                                .constant("password", "masked")
                                .build()
                )
                .build();

        AtomicReference<Mapper> reference = new AtomicReference<>();

        MappingContext context = new MappingContext(
                reference::get,
                new MappingPlanRegistry(List.of(
                        new JavaBeanPlanContributor(),
                        new RecordPlanContributor(),
                        new ScalarPlanContributor(),
                        new MapPlanContributor(),
                        new CollectionPlanContributor(),
                        new ArrayPlanContributor()
                )),
                new StandardAccessorWrapper(),
                new BinderConversion(),
                registry,
                MappingPolicy.defaults()
        );

        ObjectMapper mapper = new ObjectMapper(context);
        reference.set(mapper);

        Map<String, Object> source = Map.of(
                "user", "john",
                "password", "secret",
                "details", Map.of(
                        "status", "BLOCKED",
                        "dateOfBirth", Map.of("dateOfBirth", new Date())
                )
        );

        User user = mapper.map(source, User.class);
        UserDTO userDTO = mapper.map(source, UserDTO.class);

        Map<User, String> sourceMap = Map.of(new User("John"), "User-123");



        System.out.println(userDTO);
    }

    static class User {
        private String user;

        public User(String user) {
            this.user = user;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }
    }

    record UserDTO(String user, String password, UserDetailsDTO details) {}

    record UserDetailsDTO(DateOfBirth dateOfBirth, Status status) {}

    enum Status { BLOCKED, UNBLOCKED }

    static class DateOfBirth {
        private Date dateOfBirth;
        public Date getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    }
}
