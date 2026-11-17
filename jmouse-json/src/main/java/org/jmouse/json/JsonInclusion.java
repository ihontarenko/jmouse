package org.jmouse.json;

/**
 * 🧾 Which of an object's members are written.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum JsonInclusion {

    /** Every member, one holding null written {@code null}. */
    ALL,

    /** Every member but one holding null: absent, as a reader that defaults it expects. */
    NON_NULL
}
