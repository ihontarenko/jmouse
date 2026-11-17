package org.jmouse.j2js.teavm.supplement.jdk;

/**
 * 🧩 Why a page answers no request for a method handle: TeaVM compiles every call ahead of time and spins none at run time.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class MethodHandleRefusal {

    private MethodHandleRefusal() {
    }

    static UnsupportedOperationException refused() {
        return new UnsupportedOperationException("A page compiles no method handle: TeaVM spins no call at run time");
    }
}
