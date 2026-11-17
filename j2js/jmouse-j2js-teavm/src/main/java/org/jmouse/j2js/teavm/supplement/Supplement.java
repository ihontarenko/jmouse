package org.jmouse.j2js.teavm.supplement;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🧩 A static method of a {@link Supplements} class that becomes a method of the JDK class, under the same name: an
 * {@link SupplementKind#INSTANCE instance} method takes the object as its first parameter, a {@link SupplementKind#STATIC static}
 * one has the JDK method's own parameters.
 *
 * <p>⚠️ A JDK class that already has the method keeps its own unless the supplement says it takes the {@link SupplementPlace#BODY
 * body} too: by default a supplement only ever fills a {@link SupplementPlace#GAP gap} — a method the class lacks, or a native one
 * it declares with no body, which TeaVM reports as having no implementation.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Supplement {

    /** Whether the JDK method is called on an object or on the class. */
    SupplementKind value();

    /** What it takes the place of: only a gap, or the body TeaVM has as well. */
    SupplementPlace place() default SupplementPlace.GAP;
}
