package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 🧩 What TeaVM's {@code java.lang.invoke.MethodHandles.Lookup} lacks. A page compiles no method handle — TeaVM spins none at run time —, so each of these refuses with
 * {@link MethodHandleRefusal#refused()}: whoever asks for a compiled call falls back as it would where the JDK refuses one.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(MethodHandles.Lookup.class)
public final class LookupSupplements {

    private LookupSupplements() {
    }

    @Supplement(SupplementKind.INSTANCE)
    public static MethodHandle unreflect(MethodHandles.Lookup lookup, Method method) {
        throw MethodHandleRefusal.refused();
    }

    @Supplement(SupplementKind.INSTANCE)
    public static MethodHandle unreflectConstructor(MethodHandles.Lookup lookup, Constructor<?> constructor) {
        throw MethodHandleRefusal.refused();
    }
}
