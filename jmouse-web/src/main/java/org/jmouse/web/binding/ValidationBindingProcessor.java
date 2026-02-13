package org.jmouse.web.binding;

import org.jmouse.validator.ValidationProcessor;
import org.jmouse.validator.ValidationResult;

public final class ValidationBindingProcessor implements BindingProcessor {

    private final ValidationProcessor processor;

    public ValidationBindingProcessor(ValidationProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void onFinish(BindingFinish finish, BindingSession session) {
        Object target = finish.target();

        if (target == null) {
            return;
        }

        ValidationResult<?> result = processor.validate(target, session.objectName(), session.hints());
        ErrorsMerging.merge(session.errors(), result.errors());
    }
}
