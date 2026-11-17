package org.jmouse.settings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🎨 A word of a structured section that is a colour, written {@code #rrggbb} or {@code #rrggbbaa} — on a record's component: a window
 * offers a picker for it, and any other word is refused before anything is kept ({@link RecordChecks}). On a list of words, it holds
 * each.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface Colour {
}
