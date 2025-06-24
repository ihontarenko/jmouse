package org.jmouse.beans.conditions;

import java.lang.annotation.*;

/**
 * Registers a bean only if a specific property is present (and optionally has a specific value).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@OnlyIf(PropertyValueCondition.class)
public @interface OnlyIfProperty {

    String name();

    String value() default "";

    boolean matchIfMissing() default false;

    ComparisonOperator operator() default ComparisonOperator.EQ;

    enum ComparisonOperator {
        EQ, GTE, LT, LTE, GT, CONTAINS, NOT_CONTAINS;
    }

}
