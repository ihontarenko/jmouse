package org.jmouse.validator;

import org.jmouse.core.bind.descriptor.structured.bean.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.structured.bean.JavaBeanIntrospector;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract base class for managing validation errors.
 * <p>
 * This class provides a structured way to collect and retrieve validation errors
 * at both the object and field levels. It utilizes a {@link JavaBeanDescriptor}
 * for accessing the target object's metadata and properties.
 * </p>
 *
 * @see Errors
 * @see FieldError
 * @see ObjectError
 */
abstract public class AbstractErrors implements Errors {

    public static final String ERROR_CODE_PATH_SEPARATOR = ".";

    private final Object                     target;
    private final JavaBeanDescriptor<Object> descriptor;
    private final String                     objectName;

    private final List<ObjectError> errors      = new ArrayList<>();
    private final List<FieldError>  fieldErrors = new ArrayList<>();

    /**
     * Constructs an {@code AbstractErrors} instance for a given target object.
     * <p>
     * The constructor initializes the object descriptor and extracts the object name
     * for error reporting purposes.
     * </p>
     *
     * @param target the object being validated (must not be null)
     * @throws NullPointerException if {@code target} is null
     */
    @SuppressWarnings("unchecked")
    public AbstractErrors(Object target) {
        this.target = target;
        this.descriptor = (JavaBeanDescriptor<Object>) new JavaBeanIntrospector<>(target.getClass()).introspect().toDescriptor();
        this.objectName = descriptor.getName();
    }

    /**
     * Registers a parser error with the given error code, default message, and optional arguments.
     * <p>
     * The error is associated with the entire object rather than a specific field.
     * </p>
     *
     * @param errorCode      the error code identifying the validation issue
     * @param defaultMessage the default message if no localized message is found
     * @param arguments      optional arguments for message formatting
     */
    @Override
    public void reject(String errorCode, String defaultMessage, Object... arguments) {
        errors.add(new ObjectError(this.objectName, getErrorCodes(errorCode), defaultMessage, (Object) arguments));
    }

    /**
     * Registers a validation error for a specific field.
     * <p>
     * If the field exists in the target object, the error is recorded along with
     * the field's current value. If the field is not found, an exception is thrown.
     * </p>
     *
     * @param field          the name of the field that has the validation error
     * @param errorCode      the error code identifying the validation issue
     * @param defaultMessage the default message if no localized message is found
     * @param arguments      optional arguments for message formatting
     * @throws IllegalArgumentException if the field does not exist in the validated object
     */
    @Override
    public void rejectValue(String field, String errorCode, String defaultMessage, Object... arguments) {
        Object fieldValue = null;

/*        if (descriptor.hasProperty(field)) {
            fieldValue = getFieldValue(field);
        } else {
            throw new IllegalArgumentException(
                    "Validated object '%s' does not contain field: '%s'".formatted(objectName, field));
        }*/

        String[] errorCodes = getErrorCodes(errorCode);
        fieldErrors.add(new FieldError(objectName, field, fieldValue, errorCodes, defaultMessage, arguments));
    }

    /**
     * Retrieves the current value of a specific field in the validated object.
     * <p>
     * This method allows access to the actual value being validated, which can be useful
     * for debugging or advanced validation logic.
     * </p>
     *
     * @param field the name of the field
     * @return the value of the field, or {@code null} if unavailable
     */
    @Override
    public Object getFieldValue(String field) {
        Object value = null;

//        if (descriptor.hasProperty(field)) {
//            value = descriptor.getPropertyAccessor(field).obtainValue(target);
//        }

        return value;
    }

    /**
     * Retrieves the type of a specific field in the validated object.
     * <p>
     * This method provides insight into the expected data type of a field, which can be useful
     * for dynamic validation rules.
     * </p>
     *
     * @param field the name of the field
     * @return the {@link Class} representing the field type, or {@code null} if unknown
     */
    @Override
    public Class<?> getFieldType(String field) {
        Class<?> type = null;

//        if (descriptor.hasProperty(field)) {
//            type = descriptor.getProperty(field).getClassType();
//        }

        return type;
    }

    /**
     * Resolves error codes based on a given error code.
     * <p>
     * This method allows for error code expansion, which can be used in hierarchical
     * validation error handling. Implementations may append additional codes
     * to provide more contextual validation messages.
     * </p>
     *
     * @param code the base error code
     * @return an array of error codes, where the first one is typically the most specific
     */
    @Override
    public String[] getErrorCodes(String code) {
        return new String[]{
                objectName + ERROR_CODE_PATH_SEPARATOR + code,
                code,
        };
    }

    /**
     * Returns a list of all field-specific validation errors.
     *
     * @return a list of {@link FieldError} instances
     */
    @Override
    public List<FieldError> getErrors() {
        return fieldErrors;
    }

    /**
     * Returns a list of all parser (non-field-specific) validation errors.
     *
     * @return a list of {@link ObjectError} instances
     */
    @Override
    public List<ObjectError> getGlobalErrors() {
        return errors;
    }
}
