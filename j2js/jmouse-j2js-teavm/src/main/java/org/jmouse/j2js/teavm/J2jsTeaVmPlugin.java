package org.jmouse.j2js.teavm;

import org.jmouse.j2js.teavm.supplement.SupplementTransformer;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * 🌐 What jmouse-j2js adds to a TeaVM compilation, found by TeaVM on the class path it compiles
 * ({@code META-INF/services/org.teavm.vm.spi.TeaVMPlugin}): the methods TeaVM's class library lacks, supplied.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 * @see ClassLibrarySubstitution the classes it lacks
 */
public final class J2jsTeaVmPlugin implements TeaVMPlugin {

    @Override
    public void install(TeaVMHost host) {
        host.add(new SupplementTransformer(host.getClassLoader()));
    }
}
