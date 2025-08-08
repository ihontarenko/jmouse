package org.jmouse.mvc.mapping.annotation;

import org.jmouse.web.request.http.HttpMethod;

import java.lang.annotation.*;

/**
 * 🗺️ Core annotation for declaring route mappings in jMouse MVC.
 *
 * <p>This annotation can be applied to controller methods or types (classes)
 * to bind them to specific request paths and HTTP methods.
 *
 * <pre>{@code
 * @Mapping(path = "/users/{id}", httpMethod = HttpMethod.GET)
 * public User findUser(@PathVariable int id) { ... }
 * }</pre>
 *
 * 🔧 Supports:
 * <ul>
 *   <li>HTTP method matching via {@link #httpMethod()}</li>
 *   <li>Header conditions via {@link #headers()}</li>
 *   <li>Query parameter conditions via {@link #queryParameters()}</li>
 *   <li>Content negotiation: {@code consumes} and {@code produces}</li>
 * </ul>
 *
 * 💡 Can be used on a class level for base path and method level for sub-paths.
 *
 * @see org.jmouse.mvc.mapping.annotation.Header
 * @see org.jmouse.mvc.mapping.annotation.QueryParameter
 * @see org.jmouse.web.request.RequestRoute
 * @see org.jmouse.mvc.Route
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mapping {

    /**
     * 📍 Path pattern for the mapping (e.g. {@code /users/{id}}).
     */
    String path() default "";

    /**
     * 📬 HTTP method this mapping handles.
     * Default is GET.
     */
    HttpMethod httpMethod() default HttpMethod.GET;

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
