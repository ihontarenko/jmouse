package org.jmouse.java.index.binary;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** Converts JVM descriptors and generic Signature attributes into written Java types. */
final class SignatureTypes {

    private SignatureTypes() {}

    static String name(String internalName) {
        return internalName == null ? null : internalName.replace('/', '.').replace('$', '.');
    }

    static String descriptor(Type type) {
        return type.getClassName().replace('$', '.');
    }

    static String field(String signature, String descriptor) {
        if (signature == null) {
            return descriptor(Type.getType(descriptor));
        }

        List<String> result = new ArrayList<>();
        new SignatureReader(signature).acceptType(new TypeWriter(result::add));
        return result.getFirst();
    }

    static ClassSignature type(String signature, String superclass, String[] interfaces) {
        List<String> parameters = new ArrayList<>();
        List<String> parents = new ArrayList<>();
        List<String> implemented = new ArrayList<>();

        if (signature == null) {
            return new ClassSignature(
                    parameters,
                    name(superclass),
                    Arrays.stream(interfaces).map(SignatureTypes::name).toList());
        }

        new SignatureReader(signature)
                .accept(
                        new SignatureVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitFormalTypeParameter(String name) {
                                parameters.add(name);
                            }

                            @Override
                            public SignatureVisitor visitClassBound() {
                                return new TypeWriter(ignored -> {});
                            }

                            @Override
                            public SignatureVisitor visitInterfaceBound() {
                                return new TypeWriter(ignored -> {});
                            }

                            @Override
                            public SignatureVisitor visitSuperclass() {
                                return new TypeWriter(parents::add);
                            }

                            @Override
                            public SignatureVisitor visitInterface() {
                                return new TypeWriter(implemented::add);
                            }
                        });

        return new ClassSignature(
                parameters, parents.isEmpty() ? null : parents.getFirst(), implemented);
    }

    static MethodSignature method(String signature, String descriptor) {
        List<String> variables = new ArrayList<>();
        List<String> parameters = new ArrayList<>();
        List<String> returns = new ArrayList<>();

        if (signature == null) {
            return new MethodSignature(
                    variables,
                    Arrays.stream(Type.getArgumentTypes(descriptor))
                            .map(SignatureTypes::descriptor)
                            .toList(),
                    descriptor(Type.getReturnType(descriptor)));
        }

        new SignatureReader(signature)
                .accept(
                        new SignatureVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitFormalTypeParameter(String name) {
                                variables.add(name);
                            }

                            @Override
                            public SignatureVisitor visitClassBound() {
                                return new TypeWriter(ignored -> {});
                            }

                            @Override
                            public SignatureVisitor visitInterfaceBound() {
                                return new TypeWriter(ignored -> {});
                            }

                            @Override
                            public SignatureVisitor visitParameterType() {
                                return new TypeWriter(parameters::add);
                            }

                            @Override
                            public SignatureVisitor visitReturnType() {
                                return new TypeWriter(returns::add);
                            }

                            @Override
                            public SignatureVisitor visitExceptionType() {
                                return new TypeWriter(ignored -> {});
                            }
                        });

        return new MethodSignature(variables, parameters, returns.getFirst());
    }

    record ClassSignature(List<String> parameters, String superclass, List<String> interfaces) {}

    record MethodSignature(List<String> variables, List<String> parameters, String returnType) {}

    private static final class TypeWriter extends SignatureVisitor {

        private final Consumer<String> completed;
        private final StringBuilder name = new StringBuilder();
        private final List<String> arguments = new ArrayList<>();

        TypeWriter(Consumer<String> completed) {
            super(Opcodes.ASM9);
            this.completed = completed;
        }

        @Override
        public void visitBaseType(char descriptor) {
            completed.accept(SignatureTypes.descriptor(Type.getType(String.valueOf(descriptor))));
        }

        @Override
        public void visitTypeVariable(String variable) {
            completed.accept(variable);
        }

        @Override
        public SignatureVisitor visitArrayType() {
            return new TypeWriter(type -> completed.accept(type + "[]"));
        }

        @Override
        public void visitClassType(String internalName) {
            name.append(SignatureTypes.name(internalName));
        }

        @Override
        public void visitInnerClassType(String innerName) {
            appendArguments();
            name.append('.').append(innerName);
        }

        @Override
        public void visitTypeArgument() {
            arguments.add("?");
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            String prefix =
                    switch (wildcard) {
                        case EXTENDS -> "? extends ";
                        case SUPER -> "? super ";
                        case INSTANCEOF -> "";
                        default ->
                                throw new IllegalArgumentException(
                                        "Unknown JVM wildcard: " + wildcard);
                    };
            return new TypeWriter(type -> arguments.add(prefix + type));
        }

        @Override
        public void visitEnd() {
            appendArguments();
            completed.accept(name.toString());
        }

        private void appendArguments() {
            if (!arguments.isEmpty()) {
                name.append('<').append(String.join(", ", arguments)).append('>');
                arguments.clear();
            }
        }
    }
}
