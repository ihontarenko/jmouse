package org.jmouse.beans.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🏷️ Qualifies injection by name when multiple candidates exist.
 * <p>
 * Used to disambiguate between beans of the same type.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Qualifier {

    /**
     * ✏️ Name of the target bean.
     *
     * @return bean name
     */
    String value();
}
