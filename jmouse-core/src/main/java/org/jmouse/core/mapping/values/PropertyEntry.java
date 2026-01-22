package org.jmouse.core.mapping.values;

/**
 * A named property entry of an object-like source (bean/map/record).
 */
public record PropertyEntry(String name, SourceValueView value) {}
