package org.jmouse.core;


public interface PlaceholderReplacer {

    String PLACEHOLDER_PREFIX = "${";
    String PLACEHOLDER_SUFFIX = "}";

    String replace(String value, PlaceholderResolver resolver);

}