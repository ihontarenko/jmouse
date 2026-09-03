package org.jmouse.validator.el.runtime;

/**
 * One complaint.
 *
 * @param field   the field it is about, or {@code null} for an invariant, which belongs to the record
 * @param message what to tell whoever submitted it
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ValidationError(String field, String message) {

    @Override
    public String toString() {
        return (field == null ? "<record>" : field) + ": " + message;
    }
}
