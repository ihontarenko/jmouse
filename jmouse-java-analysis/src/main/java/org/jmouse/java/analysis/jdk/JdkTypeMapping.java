package org.jmouse.java.analysis.jdk;

import org.jmouse.java.model.descriptor.JavaArrayDescriptor;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;
import org.jmouse.java.model.type.JavaArrayType;
import org.jmouse.java.model.type.JavaCompoundType;
import org.jmouse.java.model.type.JavaDeclaredType;
import org.jmouse.java.model.type.JavaErrorType;
import org.jmouse.java.model.type.JavaExecutableType;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.jmouse.java.model.type.JavaSpecialType;
import org.jmouse.java.model.type.JavaType;
import org.jmouse.java.model.type.JavaTypeVariable;
import org.jmouse.java.model.type.JavaVoidType;
import org.jmouse.java.model.type.JavaWildcardType;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

final class JdkTypeMapping {
    private final Types types;
    private final JdkSymbolMapping symbols;

    JdkTypeMapping(Types types, JdkSymbolMapping symbols) {
        this.types = types;
        this.symbols = symbols;
    }

    JavaType type(TypeMirror mirror) {
        if (mirror == null) {
            return JavaSpecialType.NONE;
        }
        return switch (mirror.getKind()) {
            case BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE ->
                    JavaPrimitiveType.valueOf(mirror.getKind().name());
            case VOID -> JavaVoidType.VOID;
            case NULL -> JavaSpecialType.NULL;
            case NONE, PACKAGE, MODULE -> JavaSpecialType.NONE;
            case ERROR -> new JavaErrorType(mirror.toString());
            case ARRAY -> new JavaArrayType(type(((ArrayType) mirror).getComponentType()));
            case DECLARED -> {
                DeclaredType declared = (DeclaredType) mirror;
                yield new JavaDeclaredType(
                        symbols.typeKey((TypeElement) declared.asElement()),
                        declared.getTypeArguments().stream().map(this::type).toList(),
                        type(declared.getEnclosingType()));
            }
            case TYPEVAR -> new JavaTypeVariable(symbols.variableKey((TypeVariable) mirror));
            case WILDCARD -> {
                WildcardType wildcard = (WildcardType) mirror;
                yield wildcard.getExtendsBound() != null
                        ? new JavaWildcardType(
                                JavaWildcardType.Bound.EXTENDS, type(wildcard.getExtendsBound()))
                        : wildcard.getSuperBound() != null
                                ? new JavaWildcardType(
                                        JavaWildcardType.Bound.SUPER,
                                        type(wildcard.getSuperBound()))
                                : new JavaWildcardType(
                                        JavaWildcardType.Bound.UNBOUNDED, JavaSpecialType.NONE);
            }
            case INTERSECTION ->
                    new JavaCompoundType(
                            JavaCompoundType.Kind.INTERSECTION,
                            ((IntersectionType) mirror)
                                    .getBounds().stream().map(this::type).toList());
            case UNION ->
                    new JavaCompoundType(
                            JavaCompoundType.Kind.UNION,
                            ((UnionType) mirror)
                                    .getAlternatives().stream().map(this::type).toList());
            case EXECUTABLE -> {
                ExecutableType executable = (ExecutableType) mirror;
                yield new JavaExecutableType(
                        executable.getParameterTypes().stream().map(this::type).toList(),
                        type(executable.getReturnType()),
                        executable.getThrownTypes().stream().map(this::type).toList(),
                        executable.getTypeVariables().stream()
                                .map(
                                        variable ->
                                                new JavaTypeVariable(symbols.variableKey(variable)))
                                .toList(),
                        type(executable.getReceiverType()));
            }
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported compiler type: " + mirror.getKind());
        };
    }

    JavaTypeDescriptor descriptor(TypeMirror mirror) {
        TypeMirror erased = types.erasure(mirror);
        if (erased.getKind().isPrimitive()) {
            return JavaPrimitiveType.valueOf(erased.getKind().name());
        }
        if (erased.getKind() == TypeKind.VOID) {
            return JavaVoidType.VOID;
        }
        if (erased instanceof ArrayType array) {
            int dimensions = 1;
            TypeMirror element = array.getComponentType();
            while (element instanceof ArrayType nested) {
                dimensions++;
                element = nested.getComponentType();
            }
            return new JavaArrayDescriptor(descriptor(element), dimensions);
        }
        if (erased instanceof DeclaredType declared) {
            return new JavaClassDescriptor(
                    symbols.typeKey((TypeElement) declared.asElement())
                            .binaryName()
                            .replace('.', '/'));
        }
        throw new IllegalArgumentException("This compiler type has no JVM descriptor: " + erased);
    }

    JavaMethodDescriptor methodDescriptor(ExecutableType mirror) {
        List<JavaTypeDescriptor> parameters = new ArrayList<>();
        mirror.getParameterTypes().forEach(parameter -> parameters.add(descriptor(parameter)));
        return new JavaMethodDescriptor(parameters, descriptor(mirror.getReturnType()));
    }
}
