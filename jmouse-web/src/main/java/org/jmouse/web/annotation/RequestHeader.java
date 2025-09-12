package org.jmouse.web.annotation;

import org.jmouse.core.reflection.annotation.MapTo;
import org.jmouse.web.http.HttpHeader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 📑 Bind an HTTP request header to a controller method parameter.
 *
 * <p>Resolves the value of the specified {@link HttpHeader} from the
 * current request and injects it into the annotated parameter.</p>
 *
 * <p>💡 Example:</p>
 * <pre>{@code
 * public String agent(@RequestHeader(HttpHeader.USER_AGENT) String userAgent) {
 *     return "User-Agent: " + userAgent;
 * }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHeader {

    /**
     * 🏷️ Header to bind, mapped to {@code name}.
     */
    @MapTo(attribute = "name")
    HttpHeader value();

    /**
     * 🏷️ Alternative alias for {@link #value()}.
     * <p>Defaults to {@link HttpHeader#CONTENT_TYPE}.</p>
     */
    @MapTo(attribute = "value")
    HttpHeader name() default HttpHeader.CONTENT_TYPE;
}
