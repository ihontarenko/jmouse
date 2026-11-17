package org.jmouse.j2js;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🌐 A method a page may call: a {@code public static} method whose parameters and result each cross the boundary as one of the
 * {@link BoundaryValue}s — numbers, truth values, text and arrays of them; anything structured crosses as JSON text.
 *
 * <p>The page calls it by its Java name. The compiler binding turns every exported method of a class into one JavaScript
 * module — on TeaVM, {@code jmouse-j2js-teavm} writes it while the class compiles — so a module that exports names no
 * compiler.</p>
 *
 * <p>⚠️ A method that is not {@code public static}, or one with a value that cannot cross, is refused while the class
 * compiles, not when the page calls it.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Export {
}
