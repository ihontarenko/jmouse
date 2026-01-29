package org.jmouse.core.mapping.plugin;

/**
 * {@link MappingPlugin} that trims mapped {@link String} values.
 *
 * <p>If the current mapped value is a {@link String}, this plugin applies {@link String#trim()}
 * to remove leading and trailing whitespace.</p>
 *
 * <p>Non-string values are returned unchanged.</p>
 */
public final class TrimStringsPlugin implements MappingPlugin {

    /**
     * Intercept the current value produced by the mapping pipeline and normalize it when it is a string.
     *
     * @param value mapping value wrapper
     * @return trimmed string value, or original value for non-strings
     */
    @Override
    public Object onValue(MappingValue value) {
        Object current = value.current();

        if (current instanceof String string) {
            current = string.trim();
        }

        return current;
    }

}
