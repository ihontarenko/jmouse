package org.jmouse.core.environment;

import org.jmouse.core.environment.converter.HierarchicalMapConverter;
import org.jmouse.core.convert.StandardConversion;
import org.jmouse.core.convert.converter.NumberToStringConverter;
import org.jmouse.core.convert.converter.StringToNumberConverter;

/**
 * Provides a set of standard converters for property value transformation.
 * <p>This class extends {@link StandardConversion} and registers commonly used
 * converters for handling property values, ensuring smooth type conversion
 * within the environment.</p>
 */
public class PropertyValueConversion extends StandardConversion {

    /**
     * Constructs a new {@code PropertyValueConversion} instance with pre-registered converters.
     * <br>
     * The following converters are registered:
     * <ul>
     *     <li>{@link HierarchicalMapConverter} - Converts flat property maps into hierarchical structures.</li>
     *     <li>{@link NumberToStringConverter} - Converts numeric values to their string representation.</li>
     *     <li>{@link StringToNumberConverter} - Converts string values into numeric types.</li>
     * </ul>
     */
    public PropertyValueConversion() {
        registerConverter(new HierarchicalMapConverter());
        registerConverter(new NumberToStringConverter());
        registerConverter(new StringToNumberConverter());
    }
}
