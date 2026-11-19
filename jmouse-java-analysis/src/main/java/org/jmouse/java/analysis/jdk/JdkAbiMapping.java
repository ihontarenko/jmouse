package org.jmouse.java.analysis.jdk;

import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaAbiStatus;
import org.jmouse.java.model.symbol.JavaExecutableAbi;
import org.jmouse.java.model.symbol.JavaImplicitParameter;
import org.jmouse.java.model.symbol.JavaImplicitParameterKind;
import org.jmouse.java.model.type.JavaPlatformTypeNames;
import org.jmouse.java.model.type.JavaPrimitiveType;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

final class JdkAbiMapping {
    private final Elements elements;
    private final JdkTypeMapping types;

    JdkAbiMapping(Elements elements, JdkTypeMapping types) {
        this.elements = elements;
        this.types = types;
    }

    JavaExecutableAbi abi(ExecutableElement executable, JavaMethodDescriptor declared) {
        List<JavaImplicitParameter> prefix = new ArrayList<>();
        JavaAbiStatus status = JavaAbiStatus.RESOLVED;
        if (executable.getKind() == ElementKind.CONSTRUCTOR) {
            TypeElement owner = (TypeElement) executable.getEnclosingElement();
            if (owner.getKind() == ElementKind.ENUM) {
                prefix.add(
                        new JavaImplicitParameter(
                                JavaImplicitParameterKind.ENUM_NAME,
                                types.descriptor(
                                        elements.getTypeElement(JavaPlatformTypeNames.STRING)
                                                .asType())));
                prefix.add(
                        new JavaImplicitParameter(
                                JavaImplicitParameterKind.ENUM_ORDINAL, JavaPrimitiveType.INT));
            } else if (owner.getNestingKind() == NestingKind.MEMBER
                    && !owner.getModifiers().contains(Modifier.STATIC)) {
                prefix.add(
                        new JavaImplicitParameter(
                                JavaImplicitParameterKind.ENCLOSING_INSTANCE,
                                types.descriptor(owner.getEnclosingElement().asType())));
            } else if ((owner.getNestingKind() == NestingKind.LOCAL
                            || owner.getNestingKind() == NestingKind.ANONYMOUS)
                    && !owner.getModifiers().contains(Modifier.STATIC)) {
                status = JavaAbiStatus.CAPTURE_DEPENDENT;
            }
        }
        return new JavaExecutableAbi(declared, prefix, status);
    }
}
