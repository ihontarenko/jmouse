package org.jmouse.settings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 📏 What a number of a structured section runs between, and the step it moves by — on a record's component:
 * {@code @Bounds(minimum = 0, maximum = 1, step = 0.01)}. Said once, beside the value: a window draws a slider for it, and a value
 * outside it, or off its step, is refused before anything is kept ({@link RecordChecks}). On a list of numbers, it holds each.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface Bounds {

    /** The least the value may be. */
    double minimum();

    /** The most the value may be. */
    double maximum();

    /** The step it moves by from its least; 0 for any value between. */
    double step() default 0;
}
