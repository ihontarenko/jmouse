package org.jmouse.mvc.mapping.annnotation;

import org.jmouse.core.reflection.annotation.MapTo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🖼️ Maps a controller method to a named view.
 *
 * <p>Supports aliasing via {@link MapTo} for flexible attribute resolution.
 *
 * <pre>{@code
 * @ViewMapping("home")
 * public String homepage() {
 *     return "viewData";
 * }
 * }</pre>
 *
 * @see org.jmouse.mvc.adapter.return_value.ViewReturnValueHandler
 * @see org.jmouse.mvc.ViewResolver
 * @see MapTo
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ViewMapping {

    /**
     * 🏷️ View name.
     */
    @MapTo(attribute = "name")
    String value();

    /**
     * 🏷️ Alias for view name (interchangeable with {@link #value()}).
     */
    @MapTo(attribute = "value")
    String name() default "";

}
