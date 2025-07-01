package org.jmouse.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🔍 Triggers additional bean scanning on specified classes/packages.
 *
 * <p>
 * Used to explicitly include types or configuration classes in the scanning process.
 * </p>
 *
 * 🧠 Useful when auto-scanning is insufficient or needs fine-tuning.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanScan {

    /**
     * 👁️ Classes to include in additional scanning.
     *
     * @return array of classes or package references
     */
    Class<?>[] value() default {};
}
