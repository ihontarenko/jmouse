package org.jmouse.validator.manual;

import org.jmouse.common.mapping.Mapper;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public final class BindingResultMapper implements Mapper<Iterable<? extends ErrorMessage>, BindingResult> {

    @Override
    public void map(Iterable<? extends ErrorMessage> source, BindingResult destination) {
        for (ErrorMessage error : source) {
            destination.addError(new FieldError(destination.getObjectName(), error.pointer(), error.message()));
        }
    }

    @Override
    public BindingResult map(Iterable<? extends ErrorMessage> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<? extends ErrorMessage> reverse(BindingResult source) {
        throw new UnsupportedOperationException();
    }

}
