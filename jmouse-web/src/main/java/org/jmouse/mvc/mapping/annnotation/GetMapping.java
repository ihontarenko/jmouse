package org.jmouse.mvc.mapping.annnotation;

import org.jmouse.core.reflection.annotation.AttributeFor;
import org.jmouse.web.request.http.HttpMethod;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Mapping(httpMethod = HttpMethod.GET)
public @interface GetMapping {

    @AttributeFor(attribute = "path", annotation = Mapping.class)
    String requestPath();

    /**
     * 🎯 Optional list of header-based conditions.
     */
    Header[] headers() default {};

    /**
     * ❓ Optional list of query parameter-based conditions.
     */
    QueryParameter[] queryParameters() default {};

    /**
     * 🍽️ Media types the handler consumes (e.g. {@code application/json}).
     */
    String[] consumes() default {};

    /**
     * 📤 Media types the handler produces (e.g. {@code application/json}).
     */
    String[] produces() default {};

}
