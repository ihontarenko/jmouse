package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.java.bytecode.signature.JvmDescriptorParser;
import org.jmouse.java.model.descriptor.JavaArrayDescriptor;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.type.JavaPrimitiveType;
import org.junit.jupiter.api.Test;

class JvmDescriptorParserTest {
    @Test
    void descriptorLimitsAdmitTheBoundaryAndRejectOverflow() {
        JvmDescriptorParser parser = new JvmDescriptorParser();
        String maximumArray = "[".repeat(255) + "I";
        assertEquals(maximumArray, parser.field(maximumArray).encoding());
        assertEquals(255, parser.method("(" + "I".repeat(255) + ")V").parameters().size());
        assertThrows(IllegalArgumentException.class, () -> parser.field("[" + maximumArray));
        assertThrows(
                IllegalArgumentException.class, () -> parser.method("(" + "J".repeat(128) + ")V"));
        assertThrows(IllegalArgumentException.class, () -> parser.field("V"));
        assertThrows(IllegalArgumentException.class, () -> parser.field("Lbroken.name;"));
    }

    @Test
    void descriptorsPreserveArrayRankErasedNamesAndParameterSlots() {
        JvmDescriptorParser parser = new JvmDescriptorParser();
        String text = "(J[[ILjava/util/List;)Ljava/lang/String;";
        JavaMethodDescriptor method = parser.method(text);
        assertEquals(text, method.encoding());
        assertEquals(
                4,
                method.parameters().stream()
                        .mapToInt(parameter -> parameter.parameterSlots())
                        .sum());
        JavaArrayDescriptor array =
                assertInstanceOf(JavaArrayDescriptor.class, method.parameters().get(1));
        assertEquals(2, array.dimensions());
        assertEquals(JavaPrimitiveType.INT, array.element());
        assertThrows(IllegalArgumentException.class, () -> parser.method("([V)V"));
        assertThrows(IllegalArgumentException.class, () -> parser.method("(I)Vtail"));
        assertThrows(IllegalArgumentException.class, () -> parser.field("Ljava//lang/String;"));
    }
}
