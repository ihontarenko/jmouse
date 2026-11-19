package org.jmouse.java.analysis.jdk;

import com.sun.source.tree.Tree;

import org.jmouse.java.analysis.JavaConversion;
import org.jmouse.java.analysis.JavaConversionKind;

import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

final class JdkConversions {
    private final Types types;
    private final JdkTypeMapping mapping;

    JdkConversions(Types types, JdkTypeMapping mapping) {
        this.types = types;
        this.mapping = mapping;
    }

    JavaConversion conversion(
            TypeMirror source, TypeMirror target, Tree expression, boolean explicit) {
        JavaConversionKind kind;
        if (expression != null
                && (expression.getKind() == Tree.Kind.LAMBDA_EXPRESSION
                        || expression.getKind() == Tree.Kind.MEMBER_REFERENCE)) {
            kind = JavaConversionKind.FUNCTIONAL_TARGET;
        } else if (types.isSameType(source, target)) {
            kind = JavaConversionKind.IDENTITY;
        } else if (source.getKind().isPrimitive() && target.getKind().isPrimitive()) {
            kind =
                    types.isSubtype(source, target)
                            ? JavaConversionKind.WIDENING_PRIMITIVE
                            : explicit
                                    ? JavaConversionKind.NARROWING_PRIMITIVE
                                    : JavaConversionKind.CONSTANT_NARROWING;
        } else if (source.getKind().isPrimitive()) {
            TypeMirror boxed = types.boxedClass((PrimitiveType) source).asType();
            kind =
                    types.isSameType(boxed, target)
                            ? JavaConversionKind.BOXING
                            : narrowingBoxing(source, target)
                                    ? JavaConversionKind.CONSTANT_NARROWING_BOXING
                                    : JavaConversionKind.BOXING_WIDENING_REFERENCE;
        } else if (target.getKind().isPrimitive()) {
            try {
                TypeMirror unboxed = types.unboxedType(source);
                kind =
                        types.isSameType(unboxed, target)
                                ? JavaConversionKind.UNBOXING
                                : types.isSubtype(unboxed, target)
                                        ? JavaConversionKind.UNBOXING_WIDENING_PRIMITIVE
                                        : JavaConversionKind.UNBOXING_NARROWING_PRIMITIVE;
            } catch (IllegalArgumentException exception) {
                if (!explicit) {
                    throw exception;
                }
                kind = JavaConversionKind.NARROWING_REFERENCE_UNBOXING;
            }
        } else {
            kind =
                    explicit && !types.isAssignable(source, target)
                            ? JavaConversionKind.NARROWING_REFERENCE
                            : !types.isSubtype(source, target) && types.isAssignable(source, target)
                                    ? JavaConversionKind.UNCHECKED
                                    : JavaConversionKind.WIDENING_REFERENCE;
        }
        return new JavaConversion(mapping.type(source), mapping.type(target), kind);
    }

    private boolean narrowingBoxing(TypeMirror source, TypeMirror target) {
        try {
            return !types.isSubtype(source, types.unboxedType(target));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
