package org.jmouse.java.index.binary;

import org.jmouse.java.parser.model.DeclarationKind;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stateless declaration reader over class bytes. It neither executes nor loads the represented
 * class.
 */
final class ClassApiReader extends ClassVisitor {

    private static final String CLASS_INITIALIZER = "<clinit>";
    private final List<BinaryClass.Field> fields = new ArrayList<>();
    private final List<BinaryClass.Method> methods = new ArrayList<>();
    private final List<BinaryClass.Component> components = new ArrayList<>();
    private String binaryName;
    private int access;
    private SignatureTypes.ClassSignature signature;

    private ClassApiReader() {
        super(Opcodes.ASM9);
    }

    static BinaryClass read(byte[] bytes) {
        ClassApiReader reader = new ClassApiReader();
        new ClassReader(bytes)
                .accept(
                        reader,
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return new BinaryClass(
                reader.binaryName,
                kind(reader.access),
                JvmModifiers.type(reader.access),
                reader.signature.parameters(),
                reader.signature.superclass(),
                reader.signature.interfaces(),
                reader.fields,
                reader.methods,
                reader.components);
    }

    @Override
    public void visit(
            int version,
            int access,
            String name,
            String signature,
            String superclass,
            String[] interfaces) {
        this.binaryName = name;
        this.access = access;
        this.signature = SignatureTypes.type(signature, superclass, interfaces);
    }

    @Override
    public FieldVisitor visitField(
            int access, String name, String descriptor, String signature, Object value) {
        if ((access & Opcodes.ACC_SYNTHETIC) == 0) {
            fields.add(
                    new BinaryClass.Field(
                            name,
                            SignatureTypes.field(signature, descriptor),
                            JvmModifiers.field(access),
                            (access & Opcodes.ACC_ENUM) != 0));
        }
        return null;
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        if ((access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) == 0
                && !name.equals(CLASS_INITIALIZER)) {
            SignatureTypes.MethodSignature method = SignatureTypes.method(signature, descriptor);
            List<String> thrown =
                    exceptions == null
                            ? List.of()
                            : Arrays.stream(exceptions).map(SignatureTypes::name).toList();
            methods.add(
                    new BinaryClass.Method(
                            name,
                            method.returnType(),
                            method.parameters(),
                            method.variables(),
                            thrown,
                            JvmModifiers.method(access),
                            (access & Opcodes.ACC_VARARGS) != 0));
        }
        return null;
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(
            String name, String descriptor, String signature) {
        components.add(
                new BinaryClass.Component(name, SignatureTypes.field(signature, descriptor)));
        return null;
    }

    private static DeclarationKind kind(int access) {
        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            return DeclarationKind.ANNOTATION_TYPE;
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            return DeclarationKind.ENUM;
        }
        if ((access & Opcodes.ACC_RECORD) != 0) {
            return DeclarationKind.RECORD;
        }
        return (access & Opcodes.ACC_INTERFACE) != 0
                ? DeclarationKind.INTERFACE
                : DeclarationKind.CLASS;
    }
}
