package org.jmouse.java.bytecode.grammar;

import org.jmouse.el.lexer.BytesCursor;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeMode;
import org.jmouse.java.bytecode.JavaCodeCoverage;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationDefaultAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaAnnotationNode;
import org.jmouse.java.bytecode.node.attribute.JavaAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaBootstrapMethodNode;
import org.jmouse.java.bytecode.node.attribute.JavaBootstrapMethodsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaConstantValueAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaDeprecatedAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaEnclosingMethodAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaExceptionHandlerNode;
import org.jmouse.java.bytecode.node.attribute.JavaExceptionsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaInnerClassNode;
import org.jmouse.java.bytecode.node.attribute.JavaInnerClassesAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaLineNumberNode;
import org.jmouse.java.bytecode.node.attribute.JavaLineNumberTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaLocalVariableTypeTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaMethodParameterNode;
import org.jmouse.java.bytecode.node.attribute.JavaMethodParametersAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaModuleMainClassAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaModulePackagesAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaNestHostAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaNestMembersAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaParameterAnnotationsNode;
import org.jmouse.java.bytecode.node.attribute.JavaPermittedSubclassesAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaRecordAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaRecordComponentNode;
import org.jmouse.java.bytecode.node.attribute.JavaRuntimeInvisibleAnnotationsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaRuntimeInvisibleParameterAnnotationsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaRuntimeInvisibleTypeAnnotationsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaRuntimeVisibleAnnotationsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaRuntimeVisibleParameterAnnotationsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaRuntimeVisibleTypeAnnotationsAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaSignatureAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaSourceDebugExtensionAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaSourceFileAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaStackMapFrameNode;
import org.jmouse.java.bytecode.node.attribute.JavaStackMapTableAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaSyntheticAttributeNode;
import org.jmouse.java.bytecode.node.attribute.JavaTypeAnnotationNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;
import org.jmouse.java.bytecode.signature.JvmSignatureRoles;
import org.jmouse.java.bytecode.signature.JvmTextGrammar;
import org.jmouse.java.bytecode.signature.JvmTextInput;
import org.jmouse.java.bytecode.signature.JvmTextNode;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeContext;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeKind;
import org.jmouse.java.bytecode.vocabulary.JvmFormatLimits;

import java.util.ArrayList;
import java.util.List;

public final class JavaAttributeParser implements ParseRule<JavaAttributeInput, JavaAttributeNode> {
    private final JvmAttributeKind kind;

    public JavaAttributeParser(JvmAttributeKind kind) {
        this.kind = kind;
    }

    @Override
    public JavaAttributeNode parse(JavaAttributeInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        if (input.version().major() < kind.minimumMajor()) {
            throw bytes.failure(
                    "Attribute is not available in this classfile version: " + kind.encodedName());
        }
        return switch (kind) {
            case CONSTANT_VALUE -> {
                int constantIndex = bytes.unsigned16();
                yield new JavaConstantValueAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        constantIndex);
            }
            case EXCEPTIONS -> {
                List<Integer> exceptionIndices = indices(bytes);
                yield new JavaExceptionsAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        exceptionIndices);
            }
            case ENCLOSING_METHOD -> {
                int classIndex = bytes.unsigned16();
                int methodIndex = bytes.unsigned16();
                yield new JavaEnclosingMethodAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        classIndex,
                        methodIndex);
            }
            case SYNTHETIC -> {
                yield new JavaSyntheticAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex());
            }
            case SIGNATURE -> {
                int signatureIndex = bytes.unsigned16();
                ParseRole<JvmTextInput, ? extends JvmTextNode> role =
                        switch (input.context()) {
                            case CLASS -> JvmSignatureRoles.CLASS;
                            case METHOD -> JvmSignatureRoles.METHOD;
                            case FIELD, RECORD_COMPONENT -> JvmSignatureRoles.REFERENCE;
                            case CODE -> throw bytes.failure("Signature is not a Code attribute");
                        };
                JvmTextNode syntax =
                        JvmTextGrammar.parseEmbedded(
                                input.language().textLexer(),
                                bytes.source().getName(),
                                input.pool().utf8(signatureIndex),
                                role,
                                session);
                yield new JavaSignatureAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        signatureIndex,
                        syntax);
            }
            case SOURCE_FILE -> {
                int sourceFileIndex = bytes.unsigned16();
                yield new JavaSourceFileAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        sourceFileIndex);
            }
            case SOURCE_DEBUG_EXTENSION -> {
                BytecodeSpan payload = remaining(bytes);
                yield new JavaSourceDebugExtensionAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex(), payload);
            }
            case DEPRECATED -> {
                yield new JavaDeprecatedAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex());
            }
            case MODULE_PACKAGES -> {
                List<Integer> packageIndices = indices(bytes);
                yield new JavaModulePackagesAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        packageIndices);
            }
            case MODULE_MAIN_CLASS -> {
                int mainClassIndex = bytes.unsigned16();
                yield new JavaModuleMainClassAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        mainClassIndex);
            }
            case NEST_HOST -> {
                int hostClassIndex = bytes.unsigned16();
                yield new JavaNestHostAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        hostClassIndex);
            }
            case NEST_MEMBERS -> {
                List<Integer> classIndices = indices(bytes);
                yield new JavaNestMembersAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        classIndices);
            }
            case PERMITTED_SUBCLASSES -> {
                List<Integer> classIndices = indices(bytes);
                yield new JavaPermittedSubclassesAttributeNode(
                        bytes.source(),
                        input.span(),
                        input.pool(),
                        input.nameIndex(),
                        classIndices);
            }
            case INNER_CLASSES -> {
                int count = bytes.unsigned16();
                List<JavaInnerClassNode> entries = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    long start = bytes.position();
                    int value0 = bytes.unsigned16();
                    int value1 = bytes.unsigned16();
                    int value2 = bytes.unsigned16();
                    int value3 = bytes.unsigned16();
                    entries.add(
                            new JavaInnerClassNode(
                                    bytes.source(),
                                    new BytecodeSpan(start, bytes.position()),
                                    value0,
                                    value1,
                                    value2,
                                    value3));
                }
                yield new JavaInnerClassesAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex(), entries);
            }
            case LINE_NUMBER_TABLE -> {
                int count = bytes.unsigned16();
                List<JavaLineNumberNode> entries = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    long start = bytes.position();
                    int value0 = bytes.unsigned16();
                    int value1 = bytes.unsigned16();
                    entries.add(
                            new JavaLineNumberNode(
                                    bytes.source(),
                                    new BytecodeSpan(start, bytes.position()),
                                    value0,
                                    value1));
                }
                yield new JavaLineNumberTableAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex(), entries);
            }
            case LOCAL_VARIABLE_TABLE -> {
                int count = bytes.unsigned16();
                List<JavaLocalVariableNode> entries = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    long start = bytes.position();
                    int value0 = bytes.unsigned16();
                    int value1 = bytes.unsigned16();
                    int value2 = bytes.unsigned16();
                    int value3 = bytes.unsigned16();
                    int value4 = bytes.unsigned16();
                    entries.add(
                            new JavaLocalVariableNode(
                                    bytes.source(),
                                    new BytecodeSpan(start, bytes.position()),
                                    value0,
                                    value1,
                                    value2,
                                    value3,
                                    value4));
                }
                yield new JavaLocalVariableTableAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex(), entries);
            }
            case LOCAL_VARIABLE_TYPE_TABLE -> {
                int count = bytes.unsigned16();
                List<JavaLocalVariableNode> entries = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    long start = bytes.position();
                    int value0 = bytes.unsigned16();
                    int value1 = bytes.unsigned16();
                    int value2 = bytes.unsigned16();
                    int value3 = bytes.unsigned16();
                    int value4 = bytes.unsigned16();
                    entries.add(
                            new JavaLocalVariableNode(
                                    bytes.source(),
                                    new BytecodeSpan(start, bytes.position()),
                                    value0,
                                    value1,
                                    value2,
                                    value3,
                                    value4));
                }
                yield new JavaLocalVariableTypeTableAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex(), entries);
            }
            case METHOD_PARAMETERS -> {
                int count = bytes.unsigned8();
                List<JavaMethodParameterNode> entries = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    long start = bytes.position();
                    int value0 = bytes.unsigned16();
                    int value1 = bytes.unsigned16();
                    entries.add(
                            new JavaMethodParameterNode(
                                    bytes.source(),
                                    new BytecodeSpan(start, bytes.position()),
                                    value0,
                                    value1));
                }
                yield new JavaMethodParametersAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex(), entries);
            }
            case BOOTSTRAP_METHODS -> {
                int count = bytes.unsigned16();
                List<JavaBootstrapMethodNode> entries = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    long start = bytes.position();
                    int handle = bytes.unsigned16();
                    List<Integer> arguments = indices(bytes);
                    entries.add(
                            new JavaBootstrapMethodNode(
                                    bytes.source(),
                                    new BytecodeSpan(start, bytes.position()),
                                    handle,
                                    arguments));
                }
                yield new JavaBootstrapMethodsAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex(), entries);
            }
            case CODE -> code(input, session);
            case STACK_MAP_TABLE -> {
                int count = bytes.unsigned16();
                List<JavaStackMapFrameNode> frames = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    frames.add(
                            session.parse(
                                    JavaBytecodeRoles.STACK_MAP_FRAME,
                                    new JavaStackMapInput(bytes, input.pool())));
                }
                yield new JavaStackMapTableAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex(), frames);
            }
            case RECORD -> {
                int count = bytes.unsigned16();
                List<JavaRecordComponentNode> components = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    long start = bytes.position();
                    int name = bytes.unsigned16();
                    int descriptor = bytes.unsigned16();
                    input.pool().utf8(name);
                    input.pool().utf8(descriptor);
                    List<JavaAttributeNode> attributes =
                            JavaAttributeTableParser.read(
                                    bytes,
                                    input.pool(),
                                    JvmAttributeContext.RECORD_COMPONENT,
                                    input.version(),
                                    input.mode(),
                                    input.language(),
                                    session);
                    components.add(
                            new JavaRecordComponentNode(
                                    bytes.source(),
                                    new BytecodeSpan(start, bytes.position()),
                                    name,
                                    descriptor,
                                    attributes));
                }
                yield new JavaRecordAttributeNode(
                        bytes.source(), input.span(), input.pool(), input.nameIndex(), components);
            }
            case RUNTIME_VISIBLE_ANNOTATIONS ->
                    new JavaRuntimeVisibleAnnotationsAttributeNode(
                            bytes.source(),
                            input.span(),
                            input.pool(),
                            input.nameIndex(),
                            annotations(input, session));
            case RUNTIME_INVISIBLE_ANNOTATIONS ->
                    new JavaRuntimeInvisibleAnnotationsAttributeNode(
                            bytes.source(),
                            input.span(),
                            input.pool(),
                            input.nameIndex(),
                            annotations(input, session));
            case RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS ->
                    new JavaRuntimeVisibleParameterAnnotationsAttributeNode(
                            bytes.source(),
                            input.span(),
                            input.pool(),
                            input.nameIndex(),
                            parameterAnnotations(input, session));
            case RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS ->
                    new JavaRuntimeInvisibleParameterAnnotationsAttributeNode(
                            bytes.source(),
                            input.span(),
                            input.pool(),
                            input.nameIndex(),
                            parameterAnnotations(input, session));
            case RUNTIME_VISIBLE_TYPE_ANNOTATIONS ->
                    new JavaRuntimeVisibleTypeAnnotationsAttributeNode(
                            bytes.source(),
                            input.span(),
                            input.pool(),
                            input.nameIndex(),
                            typeAnnotations(input, session));
            case RUNTIME_INVISIBLE_TYPE_ANNOTATIONS ->
                    new JavaRuntimeInvisibleTypeAnnotationsAttributeNode(
                            bytes.source(),
                            input.span(),
                            input.pool(),
                            input.nameIndex(),
                            typeAnnotations(input, session));
            case ANNOTATION_DEFAULT ->
                    new JavaAnnotationDefaultAttributeNode(
                            bytes.source(),
                            input.span(),
                            input.pool(),
                            input.nameIndex(),
                            session.parse(
                                    JavaBytecodeRoles.ANNOTATION_VALUE,
                                    new JavaAnnotationInput(bytes, input.pool())));
            case MODULE -> new JavaModuleAttributeParser().parse(input, session);
        };
    }

    private static JavaCodeAttributeNode code(JavaAttributeInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        int stack = bytes.unsigned16();
        int locals = bytes.unsigned16();
        long length = bytes.unsigned32();
        if (length == 0 || length > JvmFormatLimits.MAXIMUM_CODE_BYTES) {
            throw bytes.failure("Invalid Code byte length: " + length);
        }
        BytesCursor code = bytes.region(length);
        List<JavaInstructionNode> instructions = new ArrayList<>();
        if (input.mode() != JavaBytecodeMode.DECLARATIONS_ONLY) {
            while (code.remaining() > 0) {
                long start = code.position();
                instructions.add(
                        session.parse(
                                JavaBytecodeRoles.INSTRUCTION,
                                new JavaInstructionInput(code, code.start())));
                if (code.position() <= start) {
                    throw code.failure(
                            "An instruction decoder must consume its encoded instruction");
                }
            }
        }
        int count = bytes.unsigned16();
        List<JavaExceptionHandlerNode> handlers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long start = bytes.position();
            int startPc = bytes.unsigned16();
            int endPc = bytes.unsigned16();
            int handlerPc = bytes.unsigned16();
            int catchType = bytes.unsigned16();
            handlers.add(
                    new JavaExceptionHandlerNode(
                            bytes.source(),
                            new BytecodeSpan(start, bytes.position()),
                            startPc,
                            endPc,
                            handlerPc,
                            catchType));
        }
        List<JavaAttributeNode> attributes =
                JavaAttributeTableParser.read(
                        bytes,
                        input.pool(),
                        JvmAttributeContext.CODE,
                        input.version(),
                        input.mode(),
                        input.language(),
                        session);
        return new JavaCodeAttributeNode(
                bytes.source(),
                input.span(),
                input.pool(),
                input.nameIndex(),
                stack,
                locals,
                new BytecodeSpan(code.start(), code.end()),
                input.mode() == JavaBytecodeMode.DECLARATIONS_ONLY
                        ? JavaCodeCoverage.OMITTED
                        : JavaCodeCoverage.COMPLETE,
                instructions,
                handlers,
                attributes);
    }

    private static List<JavaAnnotationNode> annotations(
            JavaAttributeInput input, ParseSession session) {
        int count = input.bytes().unsigned16();
        List<JavaAnnotationNode> annotations = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            annotations.add(
                    session.parse(
                            JavaBytecodeRoles.ANNOTATION,
                            new JavaAnnotationInput(input.bytes(), input.pool())));
        }
        return List.copyOf(annotations);
    }

    private static List<JavaParameterAnnotationsNode> parameterAnnotations(
            JavaAttributeInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        int count = bytes.unsigned8();
        List<JavaParameterAnnotationsNode> parameters = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long start = bytes.position();
            List<JavaAnnotationNode> annotations = annotations(input, session);
            parameters.add(
                    new JavaParameterAnnotationsNode(
                            bytes.source(),
                            new BytecodeSpan(start, bytes.position()),
                            index,
                            annotations));
        }
        return List.copyOf(parameters);
    }

    private static List<JavaTypeAnnotationNode> typeAnnotations(
            JavaAttributeInput input, ParseSession session) {
        BytesCursor bytes = input.bytes();
        int count = bytes.unsigned16();
        List<JavaTypeAnnotationNode> annotations = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            annotations.add(
                    session.parse(
                            JavaBytecodeRoles.TYPE_ANNOTATION,
                            new JavaAnnotationInput(bytes, input.pool())));
        }
        return List.copyOf(annotations);
    }

    static List<Integer> indices(BytesCursor bytes) {
        int count = bytes.unsigned16();
        List<Integer> indices = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            indices.add(bytes.unsigned16());
        }
        return List.copyOf(indices);
    }

    private static BytecodeSpan remaining(BytesCursor bytes) {
        BytesCursor payload = bytes.region(bytes.remaining());
        return new BytecodeSpan(payload.start(), payload.end());
    }
}
