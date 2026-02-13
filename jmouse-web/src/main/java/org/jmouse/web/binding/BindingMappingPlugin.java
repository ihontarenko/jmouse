package org.jmouse.web.binding;

import org.jmouse.core.mapping.plugin.*;
import org.jmouse.validator.Errors;

import java.util.List;

public final class BindingMappingPlugin implements MappingPlugin {

    private final BindingContextScope scope;
    private final List<BindingProcessor> processors;

    public BindingMappingPlugin(BindingContextScope scope, List<BindingProcessor> processors) {
        this.scope = scope;
        this.processors = processors;
    }

    private BindingSession currentSession() {
        BindingContext context = scope.current();

        if (context == null) {
            return null;
        }

        Errors errors = context.errors();

        if (errors == null) {
            return null;
        }

        return new BindingSession(errors, context.objectName(), context::hints);
    }

    @Override
    public void onStart(MappingCall call) {
        BindingSession session = currentSession();

        if (session == null) {
            return;
        }

        BindingStart start = BindingStart.empty();

        for (BindingProcessor processor : processors) {
            processor.onStart(start, session);
        }
    }

    @Override
    public Object onValue(MappingValue value) {
        BindingSession session = currentSession();

        if (session == null) {
            return value.current();
        }

        BindingValue bindingValue = new BindingValue(
                value.current(),
                value.destination(),
                value.destination() == null ? null : value.destination().path()
        );

        Object current = bindingValue.current();

        for (BindingProcessor processor : processors) {
            current = processor.onValue(new BindingValue(
                    current,
                    bindingValue.destination(),
                    bindingValue.path()
            ), session);
        }

        return current;
    }

    @Override
    public void onFinish(MappingResult result) {
        BindingSession session = currentSession();

        if (session == null) {
            return;
        }

        BindingFinish finish = new BindingFinish(result.target());

        for (BindingProcessor processor : processors) {
            processor.onFinish(finish, session);
        }
    }

    @Override
    public void onError(MappingFailure failure) {
        BindingSession session = currentSession();

        if (session == null) {
            return;
        }

        BindingError error = new BindingError(failure.error(), failure.path());

        for (BindingProcessor processor : processors) {
            processor.onError(error, session);
        }
    }
}
