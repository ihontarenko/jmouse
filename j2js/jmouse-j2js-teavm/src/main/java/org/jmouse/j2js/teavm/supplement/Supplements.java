package org.jmouse.j2js.teavm.supplement;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🧩 A class of {@link Supplement supplements}: the static methods that give a JDK class — {@link #value()} — what TeaVM's class
 * library lacks of it.
 *
 * <p>A class of supplements is found by being named in {@code META-INF/services/org.jmouse.j2js.teavm.supplement.Supplements},
 * so another module may supply its own beside these.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Supplements {

    /** The JDK class the methods are supplied to. */
    Class<?> value();
}
