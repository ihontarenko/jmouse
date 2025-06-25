package org.jmouse.beans.conditions;

import java.lang.annotation.*;

/**
 * Register bean only if the given condition(s) match.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanConstraint {
    Class<? extends BeanCondition>[] value();
}
