package org.jmouse.web.binding.validation;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.mapping.MappingDestination;
import org.jmouse.validator.ValidationHints;
import org.jmouse.web.binding.*;
import org.jmouse.validator.ValidationProcessor;
import org.jmouse.validator.ValidationResult;

public final class AnnotationValidationProcessor implements BindingProcessor {

    private final ValidationProcessor        validationProcessor;
    private final ValidationAnnotationReader reader;
    private final ValidationHintsResolver    hintsResolver;

    public AnnotationValidationProcessor(
            ValidationProcessor validationProcessor,
            ValidationAnnotationReader reader,
            ValidationHintsResolver hintsResolver
    ) {
        this.validationProcessor = validationProcessor;
        this.reader = reader;
        this.hintsResolver = hintsResolver;
    }

    @Override
    public Object onValue(BindingValue value, BindingSession session) {
        Object   descriptor = extractDescriptor(value);
        Object   current    = value.current();
        Validate validate   = reader.findOnDestination(descriptor, Validate.class);

        if (validate == null) {
            return value.current();
        }

        if (current == null) {
            return null;
        }

        ValidationHints     validationHints = hintsResolver.resolve(validate.groups());
        ValidationResult<?> result          = validationProcessor.validate(
                current, session.objectName(), validationHints);

        ErrorsMerging.merge(session.errors(), result.errors());

        return current;
    }

    @Override
    public void onFinish(BindingFinish finish, BindingSession session) {
        Object target = finish.target();

        if (target == null) {
            return;
        }

        Validate validate = reader.findOnTargetClass(target.getClass(), Validate.class);

        if (validate == null) {
            return;
        }

        ValidationHints     validationHints = hintsResolver.resolve(validate.groups());
        ValidationResult<?> result          = validationProcessor.validate(
                target, session.objectName(), validationHints);

        ErrorsMerging.merge(session.errors(), result.errors());
    }

    private static Object extractDescriptor(BindingValue value) {
        if (value.destination() == null) {
            return null;
        }

        MappingDestination destination = value.destination();

        if (destination instanceof MappingDestination.BeanProperty(Object target, PropertyPath path, Object descriptor)) {
            return descriptor;
        }

        if (destination instanceof MappingDestination.RecordComponent(Object target, PropertyPath path, Object descriptor)) {
            return descriptor;
        }

        return null;
    }

    public static AnnotationValidationProcessor defaults(ValidationProcessor processor) {
        return new AnnotationValidationProcessor(
                processor,
                new ReflectionValidationAnnotationReader(),
                ValidationHintsResolver.defaults()
        );
    }
}
