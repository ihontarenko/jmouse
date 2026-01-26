package org.jmouse.core.mapping.examples;

import org.jmouse.core.bind.BinderConversion;
import org.jmouse.core.mapping.DefaultObjectMapper;
import org.jmouse.core.mapping.bindings.MappingRulesRegistry;
import org.jmouse.core.mapping.bindings.TypeMappingBuilder;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.config.MappingPolicy;

import java.util.Date;
import java.util.Map;

public class Smoke1 {

    public static void main(String... arguments) {
        var rules = MappingRulesRegistry.builder()
                .register(
                        new TypeMappingBuilder<>(Map.class, Smoke1.UserDTO.class)
                                .bind("user", "username")
                                .ignore("password")
                                .constant("password", "masked")
                                .build()
                )
                .build();

        var config = MappingConfig.builder()
                .policy(MappingPolicy.defaults())
                .conversion(new BinderConversion())
                .rules(rules)
                .build();

        var mapper = new DefaultObjectMapper(config);

        Map<String, Object> source = Map.of(
                "user", "john",
                "password", "secret",
                "details", Map.of(
                        "status", "BLOCKED",
                        "dateOfBirth", Map.of("dateOfBirth", new Date())
                )
        );

        UserDTO dto = mapper.map(source, UserDTO.class);
        System.out.println(dto);
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
