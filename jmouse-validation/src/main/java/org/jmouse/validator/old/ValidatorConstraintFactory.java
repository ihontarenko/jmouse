package org.jmouse.validator.old;

import org.jmouse.common.mapping.mapper.ObjectFieldMapper;
import org.jmouse.core.proxy.AnnotationProxyFactory;
import org.jmouse.validator.old.constraint.NonEmptyValidator;
import org.jmouse.validator.old.constraint.NotNullValidator;
import org.jmouse.validator.old.constraint.NumberRangeValidator;
import org.jmouse.core.proxy.ProxyFactory;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class ValidatorConstraintFactory {

    public static final ValidatorConstraintFactory BASIC_FACTORY = new ValidatorConstraintFactory();
    public static final ProxyFactory               PROXY_FACTORY = new AnnotationProxyFactory();

    static {
        // add validator instances
        BASIC_FACTORY.addValidator(BasicValidators.NOT_NULL, new NotNullValidator());

        // add validator classes for dynamic creation
        BASIC_FACTORY.addValidator(BasicValidators.NOT_NULL, NotNullValidator.class);
        BASIC_FACTORY.addValidator(BasicValidators.EMPTY_STRING, NonEmptyValidator.class);
        BASIC_FACTORY.addValidator(BasicValidators.RANGE, NumberRangeValidator.class);
    }

    private final Map<Enum<?>, Validator>                  validators       = new HashMap<>();
    private final Map<Enum<?>, Class<? extends Validator>> validatorClasses = new HashMap<>();

    public void addValidator(Enum<?> key, Class<? extends Validator> validatorClass) {
        validatorClasses.put(key, validatorClass);
    }

    public void addValidator(Enum<?> key, Validator validator) {
        validators.put(key, validator);
    }

    public Validator getValidator(Enum<?> key) {
        Validator validator = validators.get(key);

        if (validator == null) {
            throw new UnregisteredValidatorException(
                    "Validator bean with the enum key '%s' is not found in the ValidatorConstraintFactory. Please ensure it is registered."
                            .formatted(key));
        }

        return validator;
    }

    public Class<? extends Validator> getValidatorClass(Enum<?> key) {
        Class<? extends Validator> validatorClass = validatorClasses.get(key);

        if (validatorClass == null) {
            throw new UnregisteredValidatorException(
                    "Validator class with the enum key '%s' is not found in the ValidatorConstraintFactory. Please ensure it is registered."
                            .formatted(key));
        }

        return validatorClass;
    }

    public Validator createNewValidator(Enum<?> key) {
        return createNewValidator(key, null);
    }

    public Validator createNewValidator(Enum<?> key, Map<String, Object> parameters) {
        return createNewValidator(getValidatorClass(key), parameters);
    }

    public Validator createNewValidator(Class<? extends Validator> validatorClass, Map<String, Object> parameters) {
        Constructor<?> constructor    = Reflections.findFirstConstructor(validatorClass);
        Validator      validator      = (Validator) Reflections.instantiate(constructor);
        Validator      proxyValidator = PROXY_FACTORY.createProxy(validator);

        if (parameters != null) {
            new ObjectFieldMapper().reverse(parameters, validator);
        }

        return proxyValidator;
    }

}
