package org.jmouse.core.mapping.values;

import org.jmouse.core.reflection.TypeClassifier;

/**
 * High-level classification of a value for mapping purposes.
 */
public enum ValueKind {

    NULL,
    SCALAR,
    JAVA_BEAN,
    VALUE_OBJECT,
    COLLECTION,
    MAP;

    public static ValueKind ofClassifier(TypeClassifier classifier) {
        ValueKind kind = ValueKind.NULL;

        if (classifier.isScalar()) {
            kind = ValueKind.SCALAR;
        } else if (classifier.isCollection()) {
            kind = ValueKind.COLLECTION;
        } else if (classifier.isMap()) {
            kind = ValueKind.MAP;
        } else if (classifier.isBean()) {
            kind = ValueKind.JAVA_BEAN;
        } else if (classifier.isRecord()) {
            kind = ValueKind.VALUE_OBJECT;
        }

        return kind;
    }

}
