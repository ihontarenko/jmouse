package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.attribute.JavaModuleAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleExportNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleOpenNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleProvideNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleRequireNode;

import java.util.ArrayList;
import java.util.List;

public final class JavaModuleAttributeParser
        implements ParseRule<JavaAttributeInput, JavaModuleAttributeNode> {
    @Override
    public JavaModuleAttributeNode parse(JavaAttributeInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        int name = bytes.unsigned16();
        int flags = bytes.unsigned16();
        int version = bytes.unsigned16();
        List<JavaModuleRequireNode> requires = requires(bytes);
        List<JavaModuleExportNode> exports = exports(bytes);
        List<JavaModuleOpenNode> opens = opens(bytes);
        List<Integer> uses = JavaAttributeParser.indices(bytes);
        List<JavaModuleProvideNode> provides = provides(bytes);
        return new JavaModuleAttributeNode(
                bytes.source(),
                input.span(),
                input.pool(),
                input.nameIndex(),
                name,
                flags,
                version,
                uses,
                requires,
                exports,
                opens,
                provides);
    }

    private static List<JavaModuleRequireNode> requires(BytesCursor bytes) {
        int count = bytes.unsigned16();
        List<JavaModuleRequireNode> requires = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long start = bytes.position();
            int module = bytes.unsigned16();
            int flags = bytes.unsigned16();
            int version = bytes.unsigned16();
            requires.add(
                    new JavaModuleRequireNode(
                            bytes.source(),
                            new BytecodeSpan(start, bytes.position()),
                            module,
                            flags,
                            version));
        }
        return List.copyOf(requires);
    }

    private static List<JavaModuleExportNode> exports(BytesCursor bytes) {
        int count = bytes.unsigned16();
        List<JavaModuleExportNode> exports = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long start = bytes.position();
            int packageIndex = bytes.unsigned16();
            int flags = bytes.unsigned16();
            List<Integer> targets = JavaAttributeParser.indices(bytes);
            exports.add(
                    new JavaModuleExportNode(
                            bytes.source(),
                            new BytecodeSpan(start, bytes.position()),
                            packageIndex,
                            flags,
                            targets));
        }
        return List.copyOf(exports);
    }

    private static List<JavaModuleOpenNode> opens(BytesCursor bytes) {
        int count = bytes.unsigned16();
        List<JavaModuleOpenNode> opens = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long start = bytes.position();
            int packageIndex = bytes.unsigned16();
            int flags = bytes.unsigned16();
            List<Integer> targets = JavaAttributeParser.indices(bytes);
            opens.add(
                    new JavaModuleOpenNode(
                            bytes.source(),
                            new BytecodeSpan(start, bytes.position()),
                            packageIndex,
                            flags,
                            targets));
        }
        return List.copyOf(opens);
    }

    private static List<JavaModuleProvideNode> provides(BytesCursor bytes) {
        int count = bytes.unsigned16();
        List<JavaModuleProvideNode> provides = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long start = bytes.position();
            int service = bytes.unsigned16();
            List<Integer> implementations = JavaAttributeParser.indices(bytes);
            provides.add(
                    new JavaModuleProvideNode(
                            bytes.source(),
                            new BytecodeSpan(start, bytes.position()),
                            service,
                            implementations));
        }
        return List.copyOf(provides);
    }
}
