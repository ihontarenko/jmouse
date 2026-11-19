package org.jmouse.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.java.model.descriptor.JavaClassDescriptor;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.symbol.JavaAbiStatus;
import org.jmouse.java.model.symbol.JavaExecutableAbi;
import org.jmouse.java.model.symbol.JavaImplicitParameter;
import org.jmouse.java.model.symbol.JavaImplicitParameterKind;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.symbol.JavaMethodKey;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.type.JavaVoidType;
import org.junit.jupiter.api.Test;

import java.util.List;

class ExecutableMethodKeyTest {

    @Test
    void usesTheEffectiveConstructorDescriptorAndRefusesUnresolvedCaptureLayouts() {
        JavaMethodDescriptor declared = new JavaMethodDescriptor(List.of(), JavaVoidType.VOID);
        JavaMethodKey source =
                new JavaMethodKey(
                        new JavaTypeKey(new JavaModuleKey(""), "Outer$Inner"),
                        JavaJvmNames.CONSTRUCTOR,
                        declared);
        JavaExecutableAbi abi =
                new JavaExecutableAbi(
                        declared,
                        List.of(
                                new JavaImplicitParameter(
                                        JavaImplicitParameterKind.ENCLOSING_INSTANCE,
                                        new JavaClassDescriptor("Outer"))),
                        JavaAbiStatus.RESOLVED);

        ExecutableMethodKey physical = ExecutableMethodKey.fromDeclaration(source, abi);

        assertEquals("(LOuter;)V", physical.descriptor().encoding());
        assertThrows(
                IllegalStateException.class,
                () ->
                        ExecutableMethodKey.fromDeclaration(
                                source,
                                new JavaExecutableAbi(
                                        declared, List.of(), JavaAbiStatus.CAPTURE_DEPENDENT)));
    }
}
