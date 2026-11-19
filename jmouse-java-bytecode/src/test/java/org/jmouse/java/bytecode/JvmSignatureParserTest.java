package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.java.bytecode.signature.JvmClassTypeSignatureNode;
import org.jmouse.java.bytecode.signature.JvmMethodSignatureNode;
import org.jmouse.java.bytecode.signature.JvmSignatureParser;
import org.jmouse.java.bytecode.signature.JvmTypeArgumentVariance;
import org.junit.jupiter.api.Test;

class JvmSignatureParserTest {
    @Test
    void identifiersFollowJvmGrammarAndAnOmittedClassBoundDoesNotConsumeTheNextFormal() {
        JvmSignatureParser parser = new JvmSignatureParser();
        assertEquals(
                3, parser.classSignature("<A:T:U:>Ljava/lang/Object;").typeParameters().size());
        assertEquals(
                "sample/odd(name)",
                ((JvmClassTypeSignatureNode) parser.field("Lsample/odd(name);")).internalName());
        assertThrows(IllegalArgumentException.class, () -> parser.field("Lsample//Type;"));
        assertThrows(IllegalArgumentException.class, () -> parser.field("[V"));
        assertThrows(IllegalArgumentException.class, () -> parser.method("<>()V"));
    }

    @Test
    void genericSignaturesRetainBoundsOwnerSegmentsVarianceArraysAndThrows() {
        JvmSignatureParser parser = new JvmSignatureParser();
        String text =
                "<T:Ljava/lang/Object;:Ljava/io/Serializable;E:Ljava/lang/Exception;>"
                    + "([TT;Ljava/util/List<+TT;>;)Ljava/util/Map<TT;-Ljava/lang/String;>;^TE;^Ljava/io/IOException;";
        JvmMethodSignatureNode method = parser.method(text);
        assertEquals(text, method.text());
        assertEquals(2, method.typeParameters().size());
        assertEquals(1, method.typeParameters().getFirst().interfaceBounds().size());
        assertEquals(2, method.thrownTypes().size());
        JvmClassTypeSignatureNode nested =
                (JvmClassTypeSignatureNode) parser.field("Lsample/Owner<TT;>.Inner<*>;");
        assertEquals("sample/Owner$Inner", nested.internalName());
        assertEquals(2, nested.segments().size());
        assertEquals(
                JvmTypeArgumentVariance.UNBOUNDED,
                nested.segments().get(1).arguments().getFirst().variance());
        assertEquals(
                1,
                parser.classSignature(
                                "<T::Ljava/io/Serializable;>Ljava/lang/Object;Ljava/lang/Runnable;")
                        .interfaces()
                        .size());
        assertThrows(IllegalArgumentException.class, () -> parser.field("Ljava/util/List<I>;"));
        assertThrows(IllegalArgumentException.class, () -> parser.method("()V^[I"));
        assertThrows(IllegalArgumentException.class, () -> parser.field("Ljava/util/List<>;"));
    }
}
