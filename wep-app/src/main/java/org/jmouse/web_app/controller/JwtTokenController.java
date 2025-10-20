package org.jmouse.web_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.core.MediaType;
import org.jmouse.security.jwt.Jwt;
import org.jmouse.security.jwt.JwtCodec;
import org.jmouse.security.jwt.codec.HS256JwtCodec;
import org.jmouse.security.web.jwt.JacksonJsonMapper;
import org.jmouse.web.annotation.Controller;
import org.jmouse.web.annotation.Mapping;
import org.jmouse.web.annotation.PathVariable;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.mvc.method.converter.JacksonObjectMapperResolver;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class JwtTokenController {

    private static final String ISSUER   = "https://auth.jmouse.org";
    private static final long   LIFETIME = 3600;

    private final JacksonObjectMapperResolver objectMapperResolver;

    @BeanConstructor
    public JwtTokenController(JacksonObjectMapperResolver objectMapperResolver) {
        this.objectMapperResolver = objectMapperResolver;
    }

    @Mapping(
            httpMethod = HttpMethod.GET,
            path = "/shared/jwt/{algorithm:\\w+}/generate",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String generateJwtToken(@PathVariable("algorithm") JwtCodec.Algorithm algorithm) {
        ObjectMapper objectMapper = objectMapperResolver.resolveObjectMapper(MediaType.APPLICATION_JSON);
        JwtCodec     jwtCodec     = new HS256JwtCodec(new JacksonJsonMapper(objectMapper), getClass().getName().getBytes());
        Instant      now          = Instant.now();
        Instant      expired      = now.plusSeconds(LIFETIME);

        Jwt unsigned = Jwt.builder()
                .contentType("JWT")
                .issuer(ISSUER)
                .audience("api", "dashboard")
                .issuedAt(now)
                .expiresAt(expired)
                .jwtId(UUID.randomUUID().toString())
                .roles(List.of("ADMIN", "USER"))
                .claim("authorities", List.of("ADMIN", "USER"))
                .build();

        return jwtCodec.encode(unsigned);
    }

}
