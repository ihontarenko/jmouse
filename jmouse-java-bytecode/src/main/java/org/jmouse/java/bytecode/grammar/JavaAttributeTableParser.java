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
import org.jmouse.java.model.version.ClassFileVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        Map<String, Integer> occurrences = new HashMap<>();
        for (int index = 0; index < count; index++) {
            long start = bytes.position();
            int nameIndex = bytes.unsigned16();
            String name = pool.utf8(nameIndex);
            BytesCursor payload = bytes.region(bytes.unsigned32());
            BytecodeSpan span = new BytecodeSpan(start, payload.end());
            JavaAttributeInput input =
                    new JavaAttributeInput(
                            payload, span, pool, nameIndex, context, version, mode, language);
            language.attributePolicy().validate(input, occurrences.merge(name, 1, Integer::sum));
            Optional<ParseRole<JavaAttributeInput, JavaAttributeNode>> decoder =
                    language.attribute(name);
            if (decoder.isPresent()) {
                attributes.add(session.parse(decoder.get(), input));
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
