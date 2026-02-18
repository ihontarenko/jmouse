package org.jmouse.dom.blueprint.model;

/**
 * Option for optionable controls (select, radio group, checkbox group).
 */
public record OptionModel(
        String key,
        String value
) { }
