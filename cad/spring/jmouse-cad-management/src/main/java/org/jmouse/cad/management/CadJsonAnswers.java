package org.jmouse.cad.management;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 🔌 The engine's answers keep their shape in whatever product mounts them.
 *
 * <p>⚠️ <strong>A field the engine has no value for is answered {@code null}, never left out</strong>, and its interface
 * tells the two apart. A host whose Jackson drops nulls ({@code default-property-inclusion: non_null}) took those fields
 * away, and a part's pin came out {@code undefined}: the sheet drew a labelled box for a resistor. So every answer of the
 * engine's routes, a plugin's included, is written with the host's own mapper — its modules and its dates — and this one
 * setting of the engine's. A tree is written as it stands, so the host's converter cannot change it again.</p>
 */
@ControllerAdvice(basePackages = "org.jmouse.cad")
final class CadJsonAnswers implements ResponseBodyAdvice<Object> {

    private final JsonMapper mapper;

    CadJsonAnswers(JsonMapper hostMapper) {
        this.mapper = hostMapper.rebuild()
                .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.ALWAYS)
                        .withContentInclusion(JsonInclude.Include.ALWAYS))
                .build();
    }

    /** An answer written as the engine writes every answer: a line of a stream is one, too. */
    String written(Object answer) {
        return mapper.writeValueAsString(answer);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return JacksonJsonHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null || body instanceof JsonNode) {
            return body;
        }
        return mapper.valueToTree(body);
    }
}
