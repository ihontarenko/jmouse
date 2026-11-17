package org.jmouse.j2js.teavm.supplement;

/**
 * 🧩 How a {@link Supplement supplement} stands in its JDK class.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum SupplementKind {

    /** Called on an object: the supplement's first parameter is that object. */
    INSTANCE,
    /** Called on the class: the supplement's parameters are the method's. */
    STATIC
}
