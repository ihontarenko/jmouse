package org.jmouse.el.evaluation;

import org.jmouse.core.convert.PredefinedConversion;
import org.jmouse.core.convert.converter.NumberToNumberConverter;
import org.jmouse.el.evaluation.converter.BooleanToStringConverter;

/**
 * An extension of {@link PredefinedConversion} tailored for the expression language.
 * <p>
 * This class provides conversion services specifically for expression evaluation,
 * inheriting predefined conversion rules and mechanisms from {@link PredefinedConversion}.
 * It can be extended to include additional conversions as needed by the expression language.
 * </p>
 */
public class ExpressionLanguageConversion extends PredefinedConversion {

    /**
     * Constructs a new {@code ExpressionLanguageConversion} instance.
     * <p>
     * Initializes the conversion service with predefined conversion rules.
     * </p>
     */
    public ExpressionLanguageConversion() {
        super();

        registerConverter(new NumberToNumberConverter());
        registerConverter(new BooleanToStringConverter());
    }
}
