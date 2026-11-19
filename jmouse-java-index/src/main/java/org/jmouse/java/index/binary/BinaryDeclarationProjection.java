package org.jmouse.java.index.binary;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.java.bytecode.node.*;
import org.jmouse.java.bytecode.node.attribute.*;
import org.jmouse.java.bytecode.node.constant.JavaModuleConstantNode;
import org.jmouse.java.bytecode.signature.*;
import org.jmouse.java.bytecode.vocabulary.JvmClassAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmFieldAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmMethodAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmModuleNames;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.Modifier;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeReferenceRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** One invocation; name ranges are captured while the shared writer emits each declaration. */
final class BinaryDeclarationProjection {
    private final BinaryProjectionContext context;
    private final JavaClassFileNode root;
    private final SourceWriter writer;
    private final JvmDescriptorParser descriptors = new JvmDescriptorParser();
    private final SymbolId identity;
    private final String qualifiedName;
    private final String simpleName;
    private final Map<String, Long> methodIdentities = new HashMap<>();
    private final Map<String, Long> fieldIdentities = new HashMap<>();
    private final Map<String, JvmMethodDescriptorNode> methodDescriptors = new HashMap<>();
    private final Map<String, JvmDescriptorTypeNode> fieldDescriptors = new HashMap<>();

    BinaryDeclarationProjection(BinaryProjectionContext context) {
        this.context = context;
        root = context.classFile();
        writer = context.writer();
        qualifiedName = declarationName();
        identity = BinarySymbolIds.type(qualifiedName);
        simpleName = simpleName();
        root.methods().forEach(method -> methodIdentities.merge(methodBase(method), 1L, Long::sum));
        root.fields().forEach(field -> fieldIdentities.merge(field.name(), 1L, Long::sum));
    }

    ClassFileIndexer.IndexedClass project() {
        String internalName = root.internalName();
        int separator = internalName.lastIndexOf('/');
        String packageName =
                separator < 0 ? "" : internalName.substring(0, separator).replace('/', '.');
        writer.word(BinaryProjectionNames.API_NOTICE + internalName).endLine();
        if (!packageName.isEmpty()) {
            writer.word(JavaKeyword.PACKAGE.spelling())
                    .word(packageName)
                    .join(JavaToken.SEMICOLON.spelling())
                    .blankLine();
        }
        int start = writer.length();
        DeclarationKind kind = kind();
        Set<Modifier> modifiers = JvmModifiers.type(root.accessFlags());
        modifiers(modifiers);
        if (kind == DeclarationKind.ANNOTATION_TYPE) {
            writer.openingWord(JavaToken.AT.spelling()).word(JavaKeyword.INTERFACE.spelling());
        } else {
            writer.word(
                    switch (kind) {
                        case INTERFACE -> JavaKeyword.INTERFACE.spelling();
                        case ENUM -> JavaKeyword.ENUM.spelling();
                        case RECORD -> JavaKeyword.RECORD.spelling();
                        case MODULE -> JavaKeyword.MODULE.spelling();
                        default -> JavaKeyword.CLASS.spelling();
                    });
        }
        BinaryTextRange name = context.name(simpleName);
        JvmClassSignatureNode generic = signature(root.attributes(), JvmClassSignatureNode.class);
        List<JvmTypeParameterNode> variables =
                generic == null ? List.of() : generic.typeParameters();
        variables(variables, identity, container());
        if (kind == DeclarationKind.RECORD) {
            recordComponents();
        }
        if (kind != DeclarationKind.INTERFACE
                && kind != DeclarationKind.ANNOTATION_TYPE
                && kind != DeclarationKind.MODULE
                && root.superclassIndex() != 0) {
            writer.word(JavaKeyword.EXTENDS.spelling());
            JvmTextNode superclass =
                    generic == null
                            ? classDescriptor(root.pool().className(root.superclassIndex()))
                            : generic.superclass();
            type(superclass, identity, container(), TypeReferenceRole.EXTENDS);
        }
        List<? extends JvmTextNode> interfaces =
                generic == null
                        ? root.interfaceIndices().stream()
                                .map(index -> classDescriptor(root.pool().className(index)))
                                .toList()
                        : generic.interfaces();
        if (!interfaces.isEmpty()) {
            writer.word(
                    kind == DeclarationKind.INTERFACE || kind == DeclarationKind.ANNOTATION_TYPE
                            ? JavaKeyword.EXTENDS.spelling()
                            : JavaKeyword.IMPLEMENTS.spelling());
            types(
                    interfaces,
                    identity,
                    container(),
                    kind == DeclarationKind.INTERFACE
                            ? TypeReferenceRole.EXTENDS
                            : TypeReferenceRole.IMPLEMENTS);
        }
        int headerEnd = writer.length();
        int[] bodyStart = new int[1];
        writer.block(
                context.configuration().layout().blockOpening(),
                context.configuration().layout().blockClosing(),
                output -> {
                    bodyStart[0] =
                            writer.length()
                                    - 1
                                    - context.configuration().layout().blockOpening().length();
                    for (JavaFieldNode field : root.fields()) {
                        if (context.configuration().members().matches(field)) {
                            field(field);
                        }
                    }
                    for (JavaMethodNode method : root.methods()) {
                        if (context.configuration().members().matches(method)) {
                            method(method);
                        }
                    }
                });
        int end = writer.length();
        writer.endLine();
        context.declaration(
                new BinaryProjectionContext.PendingDeclaration(
                        identity,
                        kind,
                        simpleName,
                        qualifiedName,
                        container(),
                        variableNames(variables),
                        modifiers,
                        name,
                        new BinaryTextRange(start, headerEnd),
                        new BinaryTextRange(bodyStart[0], end),
                        new BinaryTextRange(start, end)));
        return context.finish(packageName);
    }

    private String simpleName() {
        if ((root.accessFlags() & JvmClassAccessFlag.MODULE.code()) != 0) {
            return qualifiedName;
        }
        for (JavaInnerClassesAttributeNode table :
                root.attributes().stream()
                        .filter(JavaInnerClassesAttributeNode.class::isInstance)
                        .map(JavaInnerClassesAttributeNode.class::cast)
                        .toList()) {
            for (JavaInnerClassNode entry : table.entries()) {
                if (entry.innerClassIndex() == root.thisClassIndex()
                        && entry.innerNameIndex() != 0) {
                    return root.pool().utf8(entry.innerNameIndex());
                }
            }
        }
        return root.internalName().substring(root.internalName().lastIndexOf('/') + 1);
    }

    private SymbolId container() {
        for (JavaInnerClassesAttributeNode table :
                root.attributes().stream()
                        .filter(JavaInnerClassesAttributeNode.class::isInstance)
                        .map(JavaInnerClassesAttributeNode.class::cast)
                        .toList()) {
            for (JavaInnerClassNode entry : table.entries()) {
                if (entry.innerClassIndex() == root.thisClassIndex()
                        && entry.outerClassIndex() != 0) {
                    return BinarySymbolIds.type(
                            context.qualifiedName(root.pool().className(entry.outerClassIndex())));
                }
            }
        }
        return null;
    }

    private DeclarationKind kind() {
        int flags = root.accessFlags();
        if ((flags & JvmClassAccessFlag.MODULE.code()) != 0) {
            return DeclarationKind.MODULE;
        }
        if ((flags & JvmClassAccessFlag.ANNOTATION.code()) != 0) {
            return DeclarationKind.ANNOTATION_TYPE;
        }
        if ((flags & JvmClassAccessFlag.ENUM.code()) != 0) {
            return DeclarationKind.ENUM;
        }
        if (root.attributes().stream().anyMatch(JavaRecordAttributeNode.class::isInstance)) {
            return DeclarationKind.RECORD;
        }
        return (flags & JvmClassAccessFlag.INTERFACE.code()) != 0
                ? DeclarationKind.INTERFACE
                : DeclarationKind.CLASS;
    }

    private String declarationName() {
        if ((root.accessFlags() & JvmClassAccessFlag.MODULE.code()) != 0) {
            JavaModuleAttributeNode module =
                    root.attributes().stream()
                            .filter(JavaModuleAttributeNode.class::isInstance)
                            .map(JavaModuleAttributeNode.class::cast)
                            .findFirst()
                            .orElseThrow();
            JavaModuleConstantNode name =
                    root.pool().require(module.moduleNameIndex(), JavaModuleConstantNode.class);
            return JvmModuleNames.decode(root.pool().utf8(name.nameIndex()));
        }
        return context.qualifiedName(root.internalName());
    }

    private void recordComponents() {
        writer.opening(JavaToken.LEFT_PARENTHESIS.spelling());
        List<JavaRecordComponentNode> components =
                root.attributes().stream()
                        .filter(JavaRecordAttributeNode.class::isInstance)
                        .map(JavaRecordAttributeNode.class::cast)
                        .findFirst()
                        .orElseThrow()
                        .entries();
        for (int index = 0; index < components.size(); index++) {
            if (index > 0) {
                writer.join(JavaToken.COMMA.spelling());
            }
            JavaRecordComponentNode component = components.get(index);
            String name = root.pool().utf8(component.nameIndex());
            SymbolId symbol =
                    new SymbolId(
                            identity
                                    + BinaryProjectionNames.MEMBER_SEPARATOR
                                    + BinarySymbolIds.segment(name));
            int start = writer.length();
            JvmTextNode generic =
                    signature(component.attributes(), JvmReferenceTypeSignatureNode.class);
            BinaryTextRange type =
                    type(
                            generic == null
                                    ? fieldDescriptor(root.pool().utf8(component.descriptorIndex()))
                                    : generic,
                            symbol,
                            identity,
                            TypeReferenceRole.RECORD_COMPONENT_TYPE);
            BinaryTextRange nameRange = context.name(name);
            int end = writer.length();
            BinaryProjectionContext.PendingDeclaration declaration =
                    declaration(
                            symbol,
                            DeclarationKind.RECORD_COMPONENT,
                            name,
                            List.of(),
                            Set.of(),
                            nameRange,
                            start,
                            end);
            context.declaration(declaration);
            context.member(
                    new BinaryProjectionContext.PendingMember(declaration, type, null, List.of()));
        }
        writer.join(JavaToken.RIGHT_PARENTHESIS.spelling());
    }

    private void field(JavaFieldNode field) {
        int start = writer.length();
        Set<Modifier> modifiers = JvmModifiers.field(field.accessFlags());
        modifiers(modifiers);
        String suffix =
                fieldIdentities.get(field.name()) > 1 || kind() == DeclarationKind.RECORD
                        ? BinaryProjectionNames.DESCRIPTOR_SUFFIX + field.descriptor()
                        : "";
        SymbolId symbol =
                new SymbolId(
                        identity
                                + BinaryProjectionNames.MEMBER_SEPARATOR
                                + BinarySymbolIds.segment(field.name())
                                + suffix);
        JvmTextNode generic = signature(field.attributes(), JvmReferenceTypeSignatureNode.class);
        BinaryTextRange type =
                type(
                        generic == null ? fieldDescriptor(field.descriptor()) : generic,
                        symbol,
                        identity,
                        TypeReferenceRole.FIELD_TYPE);
        BinaryTextRange name = context.name(field.name());
        writer.join(JavaToken.SEMICOLON.spelling());
        int end = writer.length();
        writer.endLine();
        DeclarationKind kind =
                (field.accessFlags() & JvmFieldAccessFlag.ENUM.code()) != 0
                        ? DeclarationKind.ENUM_CONSTANT
                        : DeclarationKind.FIELD;
        BinaryProjectionContext.PendingDeclaration declaration =
                declaration(symbol, kind, field.name(), List.of(), modifiers, name, start, end);
        context.declaration(declaration);
        context.member(
                new BinaryProjectionContext.PendingMember(declaration, type, null, List.of()));
    }

    private void method(JavaMethodNode method) {
        JvmMethodDescriptorNode descriptor = methodDescriptor(method.descriptor());
        JvmMethodSignatureNode generic =
                signature(method.attributes(), JvmMethodSignatureNode.class);
        boolean constructor = method.name().equals(JavaJvmNames.CONSTRUCTOR);
        boolean initializer = method.name().equals(JavaJvmNames.CLASS_INITIALIZER);
        String name = constructor ? simpleName : method.name();
        String base = methodBase(method);
        String suffix =
                methodIdentities.get(base) > 1 || constructor
                        ? BinaryProjectionNames.DESCRIPTOR_SUFFIX
                                + BinarySymbolIds.segment(method.name())
                                + method.descriptor()
                        : "";
        SymbolId symbol = new SymbolId(base + suffix);
        int start = writer.length();
        Set<Modifier> modifiers = JvmModifiers.method(method, root.version());
        modifiers(modifiers);
        List<JvmTypeParameterNode> variables =
                generic == null ? List.of() : generic.typeParameters();
        variables(variables, symbol, identity);
        BinaryTextRange result =
                constructor || initializer
                        ? null
                        : type(
                                generic == null ? descriptor.result() : generic.result(),
                                symbol,
                                identity,
                                TypeReferenceRole.RETURN_TYPE);
        if (initializer) {
            writer.openingWord(BinaryProjectionNames.INITIALIZER_OPEN);
        }
        BinaryTextRange nameRange = context.name(name);
        List<BinaryProjectionContext.PendingParameter> parameters = new ArrayList<>();
        if (initializer) {
            writer.word(BinaryProjectionNames.INITIALIZER_CLOSE);
        } else {
            writer.opening(JavaToken.LEFT_PARENTHESIS.spelling());
            List<? extends JvmTypeSyntaxNode> types =
                    generic == null ? descriptor.parameters() : generic.parameters();
            for (int index = 0; index < types.size(); index++) {
                if (index > 0) {
                    writer.join(JavaToken.COMMA.spelling());
                }
                int parameterStart = writer.length();
                boolean variableArity =
                        (method.accessFlags() & JvmMethodAccessFlag.VARARGS.code()) != 0
                                && index == types.size() - 1
                                && (types.get(index) instanceof JvmArrayDescriptorNode
                                        || types.get(index) instanceof JvmArraySignatureNode);
                BinaryTextRange parameterType =
                        parameterType(types.get(index), variableArity, symbol);
                String parameterName = parameterName(method, index, types.size());
                context.name(parameterName);
                parameters.add(
                        new BinaryProjectionContext.PendingParameter(
                                parameterName,
                                parameterType,
                                new BinaryTextRange(parameterStart, writer.length()),
                                variableArity));
            }
            writer.join(JavaToken.RIGHT_PARENTHESIS.spelling());
            List<? extends JvmTextNode> thrown;
            if (generic != null && !generic.thrownTypes().isEmpty()) {
                thrown = generic.thrownTypes().stream().map(JvmThrowsSignatureNode::type).toList();
            } else {
                thrown =
                        method.attributes().stream()
                                .filter(JavaExceptionsAttributeNode.class::isInstance)
                                .map(JavaExceptionsAttributeNode.class::cast)
                                .flatMap(exceptions -> exceptions.exceptionIndices().stream())
                                .map(index -> classDescriptor(root.pool().className(index)))
                                .toList();
            }
            if (!thrown.isEmpty()) {
                writer.word(JavaKeyword.THROWS.spelling());
                types(thrown, symbol, identity, TypeReferenceRole.THROWS);
            }
            writer.join(JavaToken.SEMICOLON.spelling());
        }
        int end = writer.length();
        writer.endLine();
        DeclarationKind kind =
                initializer
                        ? DeclarationKind.STATIC_INITIALIZER
                        : constructor ? DeclarationKind.CONSTRUCTOR : DeclarationKind.METHOD;
        BinaryProjectionContext.PendingDeclaration declaration =
                declaration(
                        symbol,
                        kind,
                        name,
                        variableNames(variables),
                        modifiers,
                        nameRange,
                        start,
                        end);
        context.declaration(declaration);
        context.member(
                new BinaryProjectionContext.PendingMember(
                        declaration,
                        result,
                        constructor ? qualifiedName : JavaKeyword.VOID.spelling(),
                        parameters));
    }

    private BinaryTextRange parameterType(
            JvmTypeSyntaxNode node, boolean variableArity, SymbolId owner) {
        int start = writer.length();
        if (variableArity) {
            if (node instanceof JvmArrayDescriptorNode array) {
                type(array.element(), owner, identity, TypeReferenceRole.PARAMETER_TYPE);
                writer.join(BinaryProjectionNames.ARRAY_SUFFIX.repeat(array.dimensions() - 1));
            } else if (node instanceof JvmArraySignatureNode array) {
                type(array.element(), owner, identity, TypeReferenceRole.PARAMETER_TYPE);
                writer.join(BinaryProjectionNames.ARRAY_SUFFIX.repeat(array.dimensions() - 1));
            }
            int end = writer.length();
            writer.join(JavaToken.ELLIPSIS.spelling());
            return new BinaryTextRange(start, end);
        }
        return type(node, owner, identity, TypeReferenceRole.PARAMETER_TYPE);
    }

    private String parameterName(JavaMethodNode method, int index, int count) {
        JavaMethodParametersAttributeNode names =
                method.attributes().stream()
                        .filter(JavaMethodParametersAttributeNode.class::isInstance)
                        .map(JavaMethodParametersAttributeNode.class::cast)
                        .findFirst()
                        .orElse(null);
        if (names != null
                && names.entries().size() == count
                && names.entries().get(index).nameIndex() != 0) {
            return root.pool().utf8(names.entries().get(index).nameIndex());
        }
        return BinaryProjectionNames.PARAMETER_PREFIX + (index + 1);
    }

    private String methodBase(JavaMethodNode method) {
        String name = method.name().equals(JavaJvmNames.CONSTRUCTOR) ? simpleName : method.name();
        String parameters =
                methodDescriptor(method.descriptor()).descriptor().parameters().stream()
                        .map(parameter -> BinarySymbolIds.descriptorName(parameter, context))
                        .collect(Collectors.joining(JavaToken.COMMA.spelling()));
        return identity
                + BinaryProjectionNames.MEMBER_SEPARATOR
                + BinarySymbolIds.segment(name)
                + JavaToken.LEFT_PARENTHESIS.spelling()
                + parameters
                + JavaToken.RIGHT_PARENTHESIS.spelling();
    }

    private BinaryProjectionContext.PendingDeclaration declaration(
            SymbolId symbol,
            DeclarationKind kind,
            String name,
            List<String> variables,
            Set<Modifier> modifiers,
            BinaryTextRange nameRange,
            int start,
            int end) {
        BinaryTextRange span = new BinaryTextRange(start, end);
        return new BinaryProjectionContext.PendingDeclaration(
                symbol,
                kind,
                name,
                qualifiedName + BinaryProjectionNames.NESTED_SEPARATOR + name,
                identity,
                variables,
                modifiers,
                nameRange,
                span,
                null,
                span);
    }

    private void variables(List<JvmTypeParameterNode> variables, SymbolId owner, SymbolId scope) {
        if (variables.isEmpty()) {
            return;
        }
        writer.opening(JavaToken.LESS.spelling());
        for (int index = 0; index < variables.size(); index++) {
            if (index > 0) {
                writer.join(JavaToken.COMMA.spelling());
            }
            type(variables.get(index), owner, scope, TypeReferenceRole.TYPE_PARAMETER_BOUND);
        }
        writer.join(JavaToken.GREATER.spelling());
    }

    private List<String> variableNames(List<JvmTypeParameterNode> variables) {
        return variables.stream().map(JvmTypeParameterNode::name).toList();
    }

    private BinaryTextRange type(
            JvmTextNode node, SymbolId owner, SymbolId scope, TypeReferenceRole role) {
        int start = writer.length();
        new BinaryTypeProjectionContext(context, owner, scope, role).write(node);
        return new BinaryTextRange(start, writer.length());
    }

    private void types(
            List<? extends JvmTextNode> nodes,
            SymbolId owner,
            SymbolId scope,
            TypeReferenceRole role) {
        for (int index = 0; index < nodes.size(); index++) {
            if (index > 0) {
                writer.join(JavaToken.COMMA.spelling());
            }
            type(nodes.get(index), owner, scope, role);
        }
    }

    private JvmClassDescriptorNode classDescriptor(String name) {
        return (JvmClassDescriptorNode) fieldDescriptor(new JavaClassDescriptor(name).encoding());
    }

    private JvmMethodDescriptorNode methodDescriptor(String encoding) {
        context.step();
        return methodDescriptors.computeIfAbsent(
                encoding, text -> descriptors.parseMethod(root.source().getName(), text));
    }

    private JvmDescriptorTypeNode fieldDescriptor(String encoding) {
        context.step();
        return fieldDescriptors.computeIfAbsent(
                encoding, text -> descriptors.parseField(root.source().getName(), text));
    }

    private static <N extends JvmTextNode> N signature(
            List<JavaAttributeNode> attributes, Class<N> type) {
        return attributes.stream()
                .filter(JavaSignatureAttributeNode.class::isInstance)
                .map(JavaSignatureAttributeNode.class::cast)
                .map(JavaSignatureAttributeNode::syntax)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }

    private void modifiers(Set<Modifier> modifiers) {
        for (Modifier modifier : Modifier.values()) {
            if (modifiers.contains(modifier)) {
                writer.word(modifier.spelling());
            }
        }
    }
}
