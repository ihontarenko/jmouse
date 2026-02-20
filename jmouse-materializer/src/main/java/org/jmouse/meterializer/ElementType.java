package org.jmouse.meterializer;

/**
 * Predefined element kinds used for template selection. 🏷️
 *
 * <p>
 * {@code ElementType} represents logical UI/input categories
 * rather than concrete HTML tags. The rendering engine may use
 * these values to choose an appropriate {@link NodeTemplate}
 * or rendering strategy.
 * </p>
 *
 * <h3>Typical mapping examples</h3>
 *
 * <pre>{@code
 * ElementType.TEXT      → <input type="text">
 * ElementType.NUMBER    → <input type="number">
 * ElementType.SELECT    → <select>
 * ElementType.RADIO     → <input type="radio">
 * ElementType.CHECKBOX  → <input type="checkbox">
 * ElementType.TEXTAREA  → <textarea>
 * ElementType.SUBMIT    → <button type="submit">
 * }</pre>
 *
 * <p>
 * These are built-in defaults. Custom element types can be introduced
 * by extending the template selection mechanism.
 * </p>
 */
public enum ElementType {

    /**
     * Text input element.
     */
    TEXT,

    /**
     * Numeric input element.
     */
    NUMBER,

    /**
     * Select/dropdown element.
     */
    SELECT,

    /**
     * Radio button input.
     */
    RADIO,

    /**
     * Checkbox input.
     */
    CHECKBOX,

    /**
     * Multiline text area element.
     */
    TEXTAREA,

    /**
     * Submit button element.
     */
    SUBMIT
}
