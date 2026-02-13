package org.jmouse.web.binding;

public final class FailureToErrorsProcessor implements BindingProcessor {

    private final MappingFailureTranslator translator;

    public FailureToErrorsProcessor(MappingFailureTranslator translator) {
        this.translator = translator;
    }

    @Override
    public void onError(BindingError error, BindingSession session) {
        translator.translate(error.error(), error.path(), session.errors());
    }
}
