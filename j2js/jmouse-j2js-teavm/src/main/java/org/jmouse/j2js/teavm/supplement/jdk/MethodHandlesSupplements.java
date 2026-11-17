package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.lang.invoke.MethodHandles;

/**
 * 🧩 What TeaVM's {@code java.lang.invoke.MethodHandles} lacks. A page compiles no method handle — TeaVM spins none at run time —, so each of these refuses with
 * {@link MethodHandleRefusal#refused()}: whoever asks for a compiled call falls back as it would where the JDK refuses one.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(MethodHandles.class)
public final class MethodHandlesSupplements {

    private MethodHandlesSupplements() {
    }

    @Supplement(SupplementKind.STATIC)
    public static MethodHandles.Lookup lookup() {
        throw MethodHandleRefusal.refused();
    }

    @Supplement(SupplementKind.STATIC)
    public static MethodHandles.Lookup privateLookupIn(Class<?> targetClass, MethodHandles.Lookup caller) {
        throw MethodHandleRefusal.refused();
    }
}
