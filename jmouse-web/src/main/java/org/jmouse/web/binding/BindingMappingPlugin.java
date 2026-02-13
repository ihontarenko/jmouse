package org.jmouse.web.binding;

import org.jmouse.core.mapping.plugin.MappingFailure;
import org.jmouse.core.mapping.plugin.MappingPlugin;
import org.jmouse.validator.Errors;

import java.util.function.Supplier;

public final class BindingMappingPlugin implements MappingPlugin {

    private final MappingFailureTranslator translator;
    private final Supplier<Errors> errorsSupplier;

    public BindingMappingPlugin(Supplier<Errors> supplier, MappingFailureTranslator translator) {
        this.errorsSupplier = supplier;
        this.translator = translator;
    }

    @Override
    public void onError(MappingFailure failure) {
        Errors errors = errorsSupplier.get();

        if (errors == null) {
            return;
        }

        translator.translate(failure, errors);
    }
}
