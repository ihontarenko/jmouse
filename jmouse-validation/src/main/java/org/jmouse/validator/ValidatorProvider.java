package org.jmouse.validator;

import java.util.List;

public interface ValidatorProvider {
    List<Validator> getValidators();
}