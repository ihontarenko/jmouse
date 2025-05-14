package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;

/**
 * Test implementation that checks if a value is an {@link Iterable}.
 * <p>
 * Returns true if the provided value implements the Iterable interface.
 * </p>
 */
public class TypeTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        boolean result = false;

        if (arguments.getFirst() instanceof String string) {
            DataType dataType = DataType.valueOf(string.toUpperCase());

            result = switch (dataType) {
                case STRING -> type.isString();
                case NUMERIC -> type.isNumber();
                case ITERABLE -> type.isIterable();
                case COLLECTION -> type.isCollection();
                case ARRAY -> type.isArray();
                case ENUM -> type.isEnum();
                case BYTE -> value instanceof Byte;
                case SHORT -> value instanceof Short;
                case CHAR -> type.isCharacter();
                case INT -> value instanceof Integer;
                case LONG -> value instanceof Long;
                case FLOAT -> value instanceof Float;
                case DOUBLE -> type.is(Double.class);
                case BOOLEAN -> type.isBoolean();
                case LIST -> type.isList();
                case SET -> type.isSet();
                case MAP -> type.isMap();
            };
        }

        return result;
    }

    /**
     * Returns the name of this test.
     *
     * @return the string "iterable"
     */
    @Override
    public String getName() {
        return "type";
    }

    enum DataType {
        // global
        STRING, NUMERIC, ITERABLE, COLLECTION, ARRAY, ENUM,
        // specific
        BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE, BOOLEAN,
        LIST, SET, MAP
    }

}
