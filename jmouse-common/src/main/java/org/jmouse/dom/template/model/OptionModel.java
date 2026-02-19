package org.jmouse.dom.template.model;

/**
 * Option for optionable controls (select, radio group, checkbox group).
 */
public record OptionModel(
        String key,
        String value
) { }
