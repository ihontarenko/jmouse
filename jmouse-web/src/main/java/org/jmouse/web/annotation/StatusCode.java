package org.jmouse.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 📡 Bind the current HTTP status code to a controller method parameter.
 *
 * <p>Used on method parameters in {@code @Controller} classes to
 * inject the {@link jakarta.servlet.http.HttpServletResponse#getStatus()}
 * value at runtime.</p>
 *
 * <p>💡 Example:</p>
 * <pre>{@code
 * public String showStatus(@StatusCode HttpStatus status) {
 *     return "Current status: " + status;
 * }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface StatusCode {
}
