package org.jmouse.common.support.provider.error;

import org.jmouse.core.scope.ErrorDetails;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ErrorDetailsProvider implements ErrorProvider<String> {

    private final Map<String, ErrorDetails> errors;

    public ErrorDetailsProvider(Map<String, ErrorDetails> errors) {
        this.errors = errors;
    }

    @Override
    public ErrorDetails getError(String key) {
        return errors.get(key);
    }

    @Override
    public boolean hasError(String key) {
        return errors.containsKey(key);
    }

    @Override
    public Map<String, ErrorDetails> getErrorsMap() {
        return Collections.unmodifiableMap(errors);
    }

    @Override
    public Set<ErrorDetails> getErrorsSet() {
        return Set.copyOf(errors.values());
    }

}
