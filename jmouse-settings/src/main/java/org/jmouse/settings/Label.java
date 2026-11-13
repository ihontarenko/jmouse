package org.jmouse.settings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🏷️ What a reader is shown a value of a structured section as — on a record's component — or an item of a list as — on the
 * item's record: {@code @Label(value = "Gap between circuits", unit = "mm")}, {@code @Label("Paper")}. A key is where a value is
 * kept, never what it is called; what a value means is its Javadoc, read by the schema written from the record, never repeated here.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.TYPE})
public @interface Label {

    /** What a reader calls it. */
    String value();

    /** What a number counts — {@code mm}, {@code parts} —; empty for none. */
    String unit() default "";
}
