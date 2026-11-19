package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeLanguage;
import org.jmouse.java.bytecode.JavaBytecodeMode;
import org.jmouse.java.bytecode.node.attribute.JavaAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaUnknownAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeContext;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeDefinition;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeKind;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeMultiplicity;
import org.jmouse.java.bytecode.vocabulary.JvmVocabulary;
import org.jmouse.java.model.version.ClassFileVersion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class JavaAttributeTableParser {
    private JavaAttributeTableParser() {}

    public static List<JavaAttributeNode> read(
            BytesCursor bytes,
            JavaConstantPoolNode pool,
            JvmAttributeContext context,
            ClassFileVersion version,
            JavaBytecodeMode mode,
            JavaBytecodeLanguage language,
            ParseSession session) {
        int count = bytes.unsigned16();
        List<JavaAttributeNode> attributes = new ArrayList<>(count);
        Set<JvmAttributeKind> unique = new HashSet<>();
        for (int index = 0; index < count; index++) {
            long start = bytes.position();
            int nameIndex = bytes.unsigned16();
            String name = pool.utf8(nameIndex);
            Optional<JvmAttributeKind> standard = JvmAttributeKind.named(name);
            if (standard.isPresent()) {
                JvmAttributeKind kind = standard.get();
                JvmAttributeDefinition definition = JvmVocabulary.attribute(kind);
                if (!definition.contexts().contains(context)) {
                    throw bytes.failure("Attribute " + name + " is forbidden in " + context);
                }
                if (definition.multiplicity() == JvmAttributeMultiplicity.SINGLE
                        && !unique.add(kind)) {
                    throw bytes.failure("Duplicate singleton attribute: " + name);
                }
            }
            BytesCursor payload = bytes.region(bytes.unsigned32());
            BytecodeSpan span = new BytecodeSpan(start, payload.end());
            Optional<ParseRole<JavaAttributeInput, JavaAttributeNode>> decoder =
                    language.attribute(name);
            if (decoder.isPresent()) {
                attributes.add(
                        session.parse(
                                decoder.get(),
                                new JavaAttributeInput(
                                        payload, span, pool, nameIndex, context, version, mode,
                                        language)));
                payload.requireEnd();
            } else {
                attributes.add(
                        new JavaUnknownAttributeNode(
                                bytes.source(),
                                span,
                                pool,
                                nameIndex,
                                new BytecodeSpan(payload.start(), payload.end())));
            }
        }
        return List.copyOf(attributes);
    }
}
