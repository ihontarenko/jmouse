package org.jmouse.mvc.mapping.annnotation;

import org.jmouse.core.reflection.annotation.MapTo;
import org.jmouse.web.request.http.HttpMethod;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Mapping(httpMethod = HttpMethod.POST)
public @interface PostMapping {

    @MapTo(attribute = "path", annotation = Mapping.class)
    String requestPath();

    /**
     * 🎯 Optional list of header-based conditions.
     */
    @MapTo(annotation = Mapping.class)
    Header[] headers() default {};

    /**
     * ❓ Optional list of query parameter-based conditions.
     */
    @MapTo(attribute = "queryParameters", annotation = Mapping.class)
    QueryParameter[] queryParameters() default {};

    /**
     * 🍽️ Media types the handler consumes (e.g. {@code application/json}).
     */
    @MapTo(attribute = "consumes", annotation = Mapping.class)
    String[] consumes() default {};

    /**
     * 📤 Media types the handler produces (e.g. {@code application/json}).
     */
    @MapTo(attribute = "produces", annotation = Mapping.class)
    String[] produces() default {};

}
