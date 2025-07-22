package org.jmouse.beans;

import org.jmouse.beans.annotation.BeanScan;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Priority;
import org.jmouse.util.helper.Arrays;

/**
 * 🔁 Initializes the context with additional scan targets from {@link BeanScan}.
 *
 * <p>
 * Scans the original {@code baseClasses} for types annotated with {@link BeanScan}
 * and updates the context to include all explicitly referenced classes.
 * </p>
 *
 * <pre>{@code
 * @BeanScan({ServiceConfig.class})
 * public class AppEntry {}
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Priority(-100500)
public class BeanScanAnnotatedContextInitializer implements BeanContextInitializer {

    @Override
    public void initialize(BeanContext context) {
        Class<?>[] rootTypes = context.getBaseClasses();
        Class<?>[] scanTypes = new Class[0];

        for (Class<?> annotatedClass : ClassFinder.findAnnotatedClasses(BeanScan.class, rootTypes)) {
            Class<?>[] types = Reflections.getAnnotationValue(
                    annotatedClass,
                    BeanScan.class,
                    BeanScan::value
            );
            scanTypes = Arrays.concatenate(scanTypes, types);
        }

        if (scanTypes.length > 0) {
            context.addBaseClasses(scanTypes);
        }
    }
}
