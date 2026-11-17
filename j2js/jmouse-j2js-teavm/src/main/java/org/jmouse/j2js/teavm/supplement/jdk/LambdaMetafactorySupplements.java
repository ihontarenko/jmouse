package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * 🧩 The body TeaVM's {@code java.lang.invoke.LambdaMetafactory} lacks for a call at run time: TeaVM turns every lambda of the
 * source into a class while it compiles, and spins none when one is asked for at run time — so that is refused with
 * {@link MethodHandleRefusal#refused()}, and whoever asks falls back as it would where the JDK refuses one.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(LambdaMetafactory.class)
public final class LambdaMetafactorySupplements {

    private LambdaMetafactorySupplements() {
    }

    @Supplement(SupplementKind.STATIC)
    public static CallSite metafactory(MethodHandles.Lookup caller, String interfaceMethodName, MethodType factoryType,
                                       MethodType interfaceMethodType, MethodHandle implementation, MethodType dynamicMethodType) {
        throw MethodHandleRefusal.refused();
    }
}
