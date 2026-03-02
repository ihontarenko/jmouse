package org.jmouse.validator.jsr380;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.jmouse.validator.Errors;
import org.jmouse.validator.SmartValidator;
import org.jmouse.validator.Hints;

import java.util.Set;

/**
 * Adapter that bridges Jakarta Bean Validation (JSR 380 / Jakarta Validation) into jMouse
 * {@link SmartValidator} API. 🔌
 *
 * <p>
 * This validator delegates to a provided {@link Validator} and maps
 * {@link ConstraintViolation violations} into {@link Errors} using a
 * {@link Jsr380ViolationMapper}.
 * </p>
 *
 * <h3>Groups support via {@link Hints}</h3>
 * <p>
 * Validation groups are derived from {@link Hints} using the following strategy:
 * </p>
 * <ol>
 *     <li>If {@link ValidationGroups} hint is present, use {@link ValidationGroups#groups()}.</li>
 *     <li>Otherwise, treat any {@code Class<?>} values inside hints as group classes.</li>
 *     <li>If hints are {@code null} or no groups provided, validate with default group.</li>
 * </ol>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * jakarta.validation.Validator jakartaValidator = Validation.buildDefaultValidatorFactory().getValidator();
 *
 * SmartValidator validator = new Jsr380ValidatorAdapter(jakartaValidator);
 *
 * // default group
 * validator.validate(user, errors, Hints.empty());
 *
 * // explicit groups
 * validator.validate(user, errors, Hints.of(AdminChecks.class, ExtendedChecks.class));
 *
 * // via wrapper hint
 * validator.validate(user, errors, Hints.of(new ValidationGroups(AdminChecks.class)));
 * }</pre>
 *
 * <p>
 * This adapter supports all target classes ({@link #supports(Class)} returns {@code true}).
 * </p>
 */
public final class Jsr380ValidatorAdapter implements SmartValidator {

    private final Validator             validator;
    private final Jsr380ViolationMapper violationMapper;

    /**
     * Creates an adapter with default violation mapper.
     *
     * @param validator Jakarta validator (must not be {@code null})
     */
    public Jsr380ValidatorAdapter(Validator validator) {
        this(validator, new DefaultJsr380ViolationMapper());
    }

    /**
     * Creates an adapter with a custom violation mapper.
     *
     * @param validator Jakarta validator (must not be {@code null})
     * @param violationMapper mapper used to translate violations to {@link Errors}
     */
    public Jsr380ValidatorAdapter(Validator validator, Jsr380ViolationMapper violationMapper) {
        this.validator = validator;
        this.violationMapper = violationMapper;
    }

    /**
     * Supports all classes.
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

    /**
     * Validates the target object using optional groups derived from {@link Hints}.
     *
     * @param target object to validate (nullable)
     * @param errors error collector
     * @param hints  contextual hints (may contain group classes)
     */
    @Override
    public void validate(Object target, Errors errors, Hints hints) {
        if (target == null) {
            return;
        }

        Class<?>[]                       groups     = toGroups(hints);
        Set<ConstraintViolation<Object>> violations = validator.validate(target, groups);

        for (ConstraintViolation<?> violation : violations) {
            violationMapper.apply(violation, errors);
        }
    }

    /**
     * Resolves Bean Validation groups from hints.
     *
     * @param hints hints container (nullable)
     * @return groups array (never {@code null})
     */
    private static Class<?>[] toGroups(Hints hints) {
        if (hints == null) {
            return new Class<?>[0];
        }

        ValidationGroups groups = hints.find(ValidationGroups.class);

        if (groups != null && groups.groups() != null) {
            return groups.groups();
        }

        return hints.values().stream()
                .filter(value -> value instanceof Class<?>)
                .map(type -> (Class<?>) type)
                .toArray(Class[]::new);
    }
}