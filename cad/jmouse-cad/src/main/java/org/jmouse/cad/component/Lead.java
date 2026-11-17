package org.jmouse.cad.component;

import org.jspecify.annotations.Nullable;

/**
 * The lead an instrument's pin is wired with: its colour, and its stripes' colour, or null.
 *
 * @param pin     the instrument's pin it is wired to
 * @param colour  the lead's colour
 * @param stripes its stripes' colour, or null for a plain lead
 */
public record Lead(String pin, String colour, @Nullable String stripes) {
}
