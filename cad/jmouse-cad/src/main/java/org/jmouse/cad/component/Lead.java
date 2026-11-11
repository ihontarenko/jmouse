package org.jmouse.cad.component;

/** The lead an instrument's pin is wired with: its colour, and its stripes' colour, or null. */
public record Lead(String pin, String colour, String stripes) {
}
