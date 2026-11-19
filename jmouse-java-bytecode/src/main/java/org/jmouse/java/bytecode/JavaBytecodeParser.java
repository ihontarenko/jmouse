package org.jmouse.java.bytecode;

import org.jmouse.el.BytesSource;
import org.jmouse.el.BytesSourceOptions;
import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.lexer.BytesCursorException;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.grammar.JavaAttributeTableParser;
import org.jmouse.java.bytecode.grammar.JavaConstantInput;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.JavaFieldNode;
import org.jmouse.java.bytecode.node.JavaMethodNode;
import org.jmouse.java.bytecode.node.attribute.JavaAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.validation.JavaBytecodeValidation;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeContext;
import org.jmouse.java.bytecode.vocabulary.JvmFormatLimits;
import org.jmouse.java.model.version.ClassFileVersion;
import org.jmouse.java.model.version.VersionAdmission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Independent classfile parser. Decoding does not load classes or execute bootstrap methods. */
public final class JavaBytecodeParser {
    private final JavaBytecodeLanguage language;
    private final JavaBytecodeLimits limits;

    public JavaBytecodeParser() {
        this(JavaBytecodeLanguage.standard(), JavaBytecodeLimits.standard());
    }

    public JavaBytecodeParser(JavaBytecodeLanguage language, JavaBytecodeLimits limits) {
        this.language = Objects.requireNonNull(language, "language");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public JavaBytecodeResult parse(String name, byte[] input) {
        return parse(name, input, JavaBytecodeMode.COMPLETE);
    }

    public JavaBytecodeResult parse(String name, byte[] input, JavaBytecodeMode mode) {
        Objects.requireNonNull(mode, "mode");
        if (input.length > limits.maximumInputBytes()) {
            throw new BytesCursorException(
                    name, 0, "Input exceeds the configured binary resource limit");
        }
        BytesSource source = BytesSource.copyOf(name, input);
        return parseSnapshot(source, mode);
    }

    /** File/resource inputs become owned snapshots before the AST retains their provenance. */
    public JavaBytecodeResult parse(BytesSource source, JavaBytecodeMode mode) throws IOException {
        Objects.requireNonNull(mode, "mode");
        return parseSnapshot(
                source.snapshot(
                        new BytesSourceOptions(
                                BytesSourceOptions.standard().bufferSize(),
                                limits.maximumInputBytes())),
                mode);
    }

    private JavaBytecodeResult parseSnapshot(BytesSource source, JavaBytecodeMode mode) {
        ParseSession session = new ParseSession(language.rules(), limits.parsing());
        BytesCursor bytes = BytesCursor.of(source, session::step);
        try {
            return read(bytes, mode, session);
        } catch (IllegalArgumentException exception) {
            if (exception instanceof BytesCursorException binary) {
                throw binary;
            }
            BytesCursorException failure = bytes.failure(exception.getMessage());
            failure.initCause(exception);
            throw failure;
        }
    }

    private JavaBytecodeResult read(
            BytesCursor bytes, JavaBytecodeMode mode, ParseSession session) {
        if (bytes.unsigned32() != JvmFormatLimits.MAGIC) {
            throw bytes.failure("Invalid class-file magic");
        }
        int minor = bytes.unsigned16();
        int major = bytes.unsigned16();
        ClassFileVersion version = new ClassFileVersion(major, minor);
        VersionAdmission admission = language.profile().admission(version);
        if (admission != VersionAdmission.ACCEPTED && mode != JavaBytecodeMode.INSPECTION) {
            throw bytes.failure("Class-file version admission failed: " + admission);
        }
        long poolStart = bytes.position();
        int slotCount = bytes.unsigned16();
        if (slotCount == 0) {
            throw bytes.failure("constant_pool_count must be positive");
        }
        List<JavaConstantNode> entries = new ArrayList<>();
        for (int index = 1; index < slotCount; ) {
            long start = bytes.position();
            int tag = bytes.unsigned8();
            JavaConstantNode entry =
                    session.parse(
                            language.constant(tag),
                            new JavaConstantInput(bytes, start, index, tag));
            if (entry.index() != index
                    || entry.tag().code() != tag
                    || entry.tag().slots() <= 0
                    || index + entry.tag().slots() > slotCount) {
                throw bytes.failure("Constant decoder returned an inconsistent slot");
            }
            entries.add(entry);
            index += entry.tag().slots();
        }
        JavaConstantPoolNode pool =
                new JavaConstantPoolNode(
                        bytes.source(),
                        new BytecodeSpan(poolStart, bytes.position()),
                        slotCount,
                        entries);
        int flags = bytes.unsigned16();
        int identity = bytes.unsigned16();
        int superclass = bytes.unsigned16();
        pool.className(identity);
        if (superclass != JvmFormatLimits.NO_INDEX) {
            pool.className(superclass);
        }
        List<Integer> interfaces = indices(bytes);
        interfaces.forEach(pool::className);
        int fieldCount = bytes.unsigned16();
        List<JavaFieldNode> fields = new ArrayList<>(fieldCount);
        for (int index = 0; index < fieldCount; index++) {
            long start = bytes.position();
            int access = bytes.unsigned16();
            int name = bytes.unsigned16();
            int descriptor = bytes.unsigned16();
            pool.utf8(name);
            pool.utf8(descriptor);
            List<JavaAttributeNode> attributes =
                    JavaAttributeTableParser.read(
                            bytes,
                            pool,
                            JvmAttributeContext.FIELD,
                            version,
                            mode,
                            language,
                            session);
            fields.add(
                    new JavaFieldNode(
                            bytes.source(),
                            new BytecodeSpan(start, bytes.position()),
                            pool,
                            access,
                            name,
                            descriptor,
                            attributes));
        }
        int methodCount = bytes.unsigned16();
        List<JavaMethodNode> methods = new ArrayList<>(methodCount);
        for (int index = 0; index < methodCount; index++) {
            long start = bytes.position();
            int access = bytes.unsigned16();
            int name = bytes.unsigned16();
            int descriptor = bytes.unsigned16();
            pool.utf8(name);
            pool.utf8(descriptor);
            List<JavaAttributeNode> attributes =
                    JavaAttributeTableParser.read(
                            bytes,
                            pool,
                            JvmAttributeContext.METHOD,
                            version,
                            mode,
                            language,
                            session);
            methods.add(
                    new JavaMethodNode(
                            bytes.source(),
                            new BytecodeSpan(start, bytes.position()),
                            pool,
                            access,
                            name,
                            descriptor,
                            attributes));
        }
        List<JavaAttributeNode> attributes =
                JavaAttributeTableParser.read(
                        bytes, pool, JvmAttributeContext.CLASS, version, mode, language, session);
        bytes.requireEnd();
        JavaClassFileNode root =
                new JavaClassFileNode(
                        bytes.source(),
                        new BytecodeSpan(bytes.start(), bytes.end()),
                        version,
                        pool,
                        flags,
                        identity,
                        superclass,
                        interfaces,
                        fields,
                        methods,
                        attributes);
        JavaBytecodeValidation.validate(root, language, mode, session);
        root.freeze();
        return new JavaBytecodeResult(root, mode, admission);
    }

    private static List<Integer> indices(BytesCursor bytes) {
        int count = bytes.unsigned16();
        List<Integer> indices = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            indices.add(bytes.unsigned16());
        }
        return List.copyOf(indices);
    }
}
