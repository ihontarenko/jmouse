package svit.provider.error;

import svit.context.ErrorDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BindingResultErrorProvider implements ErrorProvider<String> {

    private final BindingResult bindingResult;

    public BindingResultErrorProvider(BindingResult bindingResult) {
        this.bindingResult = bindingResult;
    }

    @Override
    public boolean hasError(String key) {
        return bindingResult.hasFieldErrors(key);
    }

    @Override
    public ErrorDetails getError(String key) {
        FieldError fieldError = bindingResult.getFieldError(key);

        assert fieldError != null;

        return new ErrorDetails(fieldError.getCode(), fieldError.getDefaultMessage());
    }

    @Override
    public Map<String, ErrorDetails> getErrorsMap() {
        Map<String, ErrorDetails> errors = new HashMap<>();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), new ErrorDetails(fieldError.getCode(), fieldError.getDefaultMessage()));
        }

        return errors;
    }

    @Override
    public Set<ErrorDetails> getErrorsSet() {
        return Set.copyOf(getErrorsMap().values());
    }

}
