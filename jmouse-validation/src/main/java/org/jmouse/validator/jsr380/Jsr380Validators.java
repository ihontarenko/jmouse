package org.jmouse.validator.jsr380;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.jmouse.core.Verify;

/**
 * Factory utilities for creating {@link Jsr380ValidatorAdapter} instances. 🧩
 *
 * <p>This class bridges the Jakarta Bean Validation (JSR-380) API to the jMouse validation layer
 * by wrapping a {@link Validator} into {@link Jsr380ValidatorAdapter}.</p>
 *
 * <p>Two creation paths are supported:</p>
 * <ul>
 *   <li><b>Default</b> - builds a {@link ValidatorFactory} via {@link Validation#buildDefaultValidatorFactory()}
 *       and obtains a {@link Validator} from it.</li>
 *   <li><b>Custom</b> - accepts an externally managed {@link Validator} and optional
 *       {@link Jsr380ViolationMapper}.</li>
 * </ul>
 *
 * <p><b>Resource note:</b> {@link ValidatorFactory} is created per call here. If you call this frequently,
 * consider building and reusing a single factory/validator at application startup.</p>
 */
public final class Jsr380Validators {

    private Jsr380Validators() {
    }

    /**
     * Create an adapter using the default Jakarta Bean Validation provider configuration.
     *
     * @return a new {@link Jsr380ValidatorAdapter}
     */
    public static Jsr380ValidatorAdapter createDefaultAdapter() {
        return createAdapter(defaultValidator());
    }

    /**
     * Create an adapter using the default Jakarta Bean Validation provider configuration and
     * a custom violation mapper.
     *
     * @param mapper violation mapper used to convert constraint violations into jMouse errors
     * @return a new {@link Jsr380ValidatorAdapter}
     */
    public static Jsr380ValidatorAdapter createDefaultAdapter(Jsr380ViolationMapper mapper) {
        return createAdapter(defaultValidator(), mapper);
    }

    /**
     * Create an adapter for the provided {@link Validator}.
     *
     * @param validator Jakarta validator (never {@code null})
     * @return a new {@link Jsr380ValidatorAdapter}
     */
    public static Jsr380ValidatorAdapter createAdapter(Validator validator) {
        return new Jsr380ValidatorAdapter(Verify.nonNull(validator, "validator"));
    }

    /**
     * Create an adapter for the provided {@link Validator} using a custom violation mapper.
     *
     * @param validator Jakarta validator (never {@code null})
     * @param mapper violation mapper (never {@code null})
     * @return a new {@link Jsr380ValidatorAdapter}
     */
    public static Jsr380ValidatorAdapter createAdapter(Validator validator, Jsr380ViolationMapper mapper) {
        return new Jsr380ValidatorAdapter(
                Verify.nonNull(validator, "validator"),
                Verify.nonNull(mapper, "mapper")
        );
    }

    private static Validator defaultValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }
}
