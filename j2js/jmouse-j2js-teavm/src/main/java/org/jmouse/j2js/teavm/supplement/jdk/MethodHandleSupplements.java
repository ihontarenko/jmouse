package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.lang.invoke.MethodHandle;

/**
 * 🧩 What TeaVM's {@code java.lang.invoke.MethodHandle} lacks. A page compiles no method handle — TeaVM spins none at run
 * time —, so a call through one is refused with {@link MethodHandleRefusal#refused()}. {@code invoke} is signature
 * polymorphic: each shape a call is compiled to is a method of its own; the one supplied is the shape the engine calls,
 * no argument and an object back.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(MethodHandle.class)
public final class MethodHandleSupplements {

    private MethodHandleSupplements() {
    }

    @Supplement(SupplementKind.INSTANCE)
    public static Object invoke(MethodHandle handle) {
        throw MethodHandleRefusal.refused();
    }
}
