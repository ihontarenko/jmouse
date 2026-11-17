package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

/**
 * 🧩 What TeaVM's {@code java.lang.invoke.CallSite} lacks. A page compiles no method handle — TeaVM spins none at run time —, so each of these refuses with
 * {@link MethodHandleRefusal#refused()}: whoever asks for a compiled call falls back as it would where the JDK refuses one.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(CallSite.class)
public final class CallSiteSupplements {

    private CallSiteSupplements() {
    }

    @Supplement(SupplementKind.INSTANCE)
    public static MethodHandle getTarget(CallSite callSite) {
        throw MethodHandleRefusal.refused();
    }
}
