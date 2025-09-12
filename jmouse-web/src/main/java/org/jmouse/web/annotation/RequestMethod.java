package org.jmouse.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🚦 Bind the HTTP request method to a controller method parameter.
 *
 * <p>Injects the current {@link org.jmouse.web.http.HttpMethod}
 * (e.g. {@code GET}, {@code POST}) into the annotated parameter.</p>
 *
 * <p>💡 Example:</p>
 * <pre>{@code
 * public String echo(@RequestMethod HttpMethod method) {
 *     return "Handled via " + method;
 * }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMethod {
}
