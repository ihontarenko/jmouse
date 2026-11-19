package org.jmouse.java.analysis.jdk;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import org.jmouse.java.analysis.JavaOverrideRelation;
import org.jmouse.java.model.symbol.JavaConstantValue;
import org.jmouse.java.model.symbol.JavaExecutableAbi;
import org.jmouse.java.model.symbol.JavaFieldKey;
import org.jmouse.java.model.symbol.JavaInferenceContextKey;
import org.jmouse.java.model.symbol.JavaInitializerKey;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.symbol.JavaMethodKey;
import org.jmouse.java.model.symbol.JavaModifier;
import org.jmouse.java.model.symbol.JavaModuleKey;
import org.jmouse.java.model.symbol.JavaPackageKey;
import org.jmouse.java.model.symbol.JavaSourceLocation;
import org.jmouse.java.model.symbol.JavaStandardModifier;
import org.jmouse.java.model.symbol.JavaSymbolDescription;
import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.symbol.JavaSymbolOriginKind;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.symbol.JavaTypeVariableKey;
import org.jmouse.java.model.symbol.JavaVariableKey;
import org.jmouse.java.model.symbol.JavaVariableKind;
import org.jmouse.java.model.type.JavaType;
import org.jmouse.java.parser.lexer.JavaKeyword;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

final class JdkSymbolMapping {
    final Map<JavaSymbolKey, JavaSymbolDescription> symbols = new LinkedHashMap<>();
    final Map<JavaTypeVariableKey, JavaType> upperBounds = new LinkedHashMap<>();
    final Map<JavaMethodKey, JavaExecutableAbi> executableAbis = new LinkedHashMap<>();
    final Map<JavaSymbolKey, JavaConstantValue> constants = new LinkedHashMap<>();
    final Set<JavaOverrideRelation> overrides = new LinkedHashSet<>();
    final JdkTypeMapping typeMapping;
    private final Elements elements;
    private final Trees trees;
    private final JdkSourceIndex source;
    private final JdkAbiMapping abis;
    private final JavaInferenceContextKey inferenceContext;
    private final Map<Element, JavaSymbolKey> keys = new IdentityHashMap<>();
    private final Set<JavaTypeVariableKey> readingBounds = new HashSet<>();
    private int captureOrdinal;

    JdkSymbolMapping(Elements elements, Types types, Trees trees, JdkSourceIndex source) {
        this.elements = elements;
        this.trees = trees;
        this.source = source;
        typeMapping = new JdkTypeMapping(types, this);
        abis = new JdkAbiMapping(elements, typeMapping);
        inferenceContext = new JavaInferenceContextKey(source.snapshotIdentity());
    }

    JavaTypeKey typeKey(TypeElement element) {
        ModuleElement module = elements.getModuleOf(element);
        return new JavaTypeKey(
                new JavaModuleKey(module == null ? "" : module.getQualifiedName().toString()),
                elements.getBinaryName(element).toString());
    }

    JavaTypeVariableKey variableKey(TypeVariable variable) {
        JavaTypeVariableKey key = (JavaTypeVariableKey) key(variable.asElement());
        if (!upperBounds.containsKey(key) && readingBounds.add(key)) {
            try {
                upperBounds.put(key, typeMapping.type(variable.getUpperBound()));
            } finally {
                readingBounds.remove(key);
            }
        }
        return key;
    }

    JavaSymbolKey key(Element element) {
        if (element == null) {
            return null;
        }
        JavaSymbolKey existing = keys.get(element);
        if (existing != null) {
            return existing;
        }
        String name = element.getSimpleName().toString();
        JavaSymbolKey key =
                switch (element.getKind()) {
                    case MODULE ->
                            new JavaModuleKey(
                                    ((ModuleElement) element).getQualifiedName().toString());
                    case PACKAGE -> {
                        ModuleElement module = elements.getModuleOf(element);
                        yield new JavaPackageKey(
                                new JavaModuleKey(
                                        module == null ? "" : module.getQualifiedName().toString()),
                                ((PackageElement) element).getQualifiedName().toString());
                    }
                    case CLASS, INTERFACE, ENUM, ANNOTATION_TYPE, RECORD ->
                            typeKey((TypeElement) element);
                    case METHOD, CONSTRUCTOR -> {
                        ExecutableElement executable = (ExecutableElement) element;
                        yield new JavaMethodKey(
                                typeKey((TypeElement) executable.getEnclosingElement()),
                                element.getKind() == ElementKind.CONSTRUCTOR
                                        ? JavaJvmNames.CONSTRUCTOR
                                        : name,
                                typeMapping.methodDescriptor((ExecutableType) executable.asType()));
                    }
                    case STATIC_INIT, INSTANCE_INIT ->
                            new JavaInitializerKey(
                                    typeKey((TypeElement) element.getEnclosingElement()),
                                    ordinal(element),
                                    element.getKind() == ElementKind.STATIC_INIT
                                            ? JavaInitializerKey.Kind.STATIC
                                            : JavaInitializerKey.Kind.INSTANCE);
                    case FIELD, ENUM_CONSTANT -> {
                        JavaTypeKey owner = typeKey((TypeElement) element.getEnclosingElement());
                        yield name.equals(JavaKeyword.THIS.spelling())
                                        || name.equals(JavaKeyword.SUPER.spelling())
                                ? new JavaVariableKey(owner, name, 0, JavaVariableKind.RECEIVER)
                                : new JavaFieldKey(
                                        owner, name, typeMapping.descriptor(element.asType()));
                    }
                    case PARAMETER,
                            LOCAL_VARIABLE,
                            EXCEPTION_PARAMETER,
                            RESOURCE_VARIABLE,
                            BINDING_VARIABLE,
                            RECORD_COMPONENT -> {
                        JavaVariableKind kind =
                                switch (element.getKind()) {
                                    case PARAMETER -> JavaVariableKind.PARAMETER;
                                    case RESOURCE_VARIABLE -> JavaVariableKind.RESOURCE;
                                    case EXCEPTION_PARAMETER ->
                                            JavaVariableKind.EXCEPTION_PARAMETER;
                                    case RECORD_COMPONENT -> JavaVariableKind.RECORD_COMPONENT;
                                    default -> JavaVariableKind.LOCAL;
                                };
                        yield new JavaVariableKey(
                                key(element.getEnclosingElement()), name, ordinal(element), kind);
                    }
                    case TYPE_PARAMETER -> {
                        TypeParameterElement parameter = (TypeParameterElement) element;
                        Element owner = parameter.getGenericElement();
                        int index =
                                owner instanceof TypeElement type
                                        ? memberOrdinal(type.getTypeParameters(), parameter)
                                        : owner instanceof ExecutableElement executable
                                                ? memberOrdinal(
                                                        executable.getTypeParameters(), parameter)
                                                : -1;
                        yield new JavaTypeVariableKey(
                                index < 0 ? inferenceContext : key(owner),
                                index < 0 ? JdkSemanticNames.CAPTURE : name,
                                index < 0 ? captureOrdinal++ : index);
                    }
                    default -> null;
                };
        if (key != null) {
            keys.put(element, key);
        }
        return key;
    }

    JavaSymbolKey describe(Element element) {
        JavaSymbolKey key = key(element);
        if (key == null || symbols.containsKey(key)) {
            return key;
        }
        List<JavaSymbolKey> members = new ArrayList<>();
        for (Element member : element.getEnclosedElements()) {
            JavaSymbolKey memberKey = key(member);
            if (memberKey != null) {
                members.add(memberKey);
            }
        }
        List<JavaType> supertypes = new ArrayList<>();
        if (element instanceof TypeElement type) {
            if (type.getSuperclass().getKind() != TypeKind.NONE) {
                supertypes.add(typeMapping.type(type.getSuperclass()));
            }
            type.getInterfaces().forEach(parent -> supertypes.add(typeMapping.type(parent)));
            type.getTypeParameters().forEach(parameter -> members.add(key(parameter)));
            type.getRecordComponents().forEach(component -> members.add(key(component)));
        } else if (element instanceof ExecutableElement method) {
            method.getParameters().forEach(parameter -> members.add(key(parameter)));
            method.getTypeParameters().forEach(parameter -> members.add(key(parameter)));
        }
        TreePath path = trees.getPath(element);
        Optional<JavaSourceLocation> location =
                path == null
                        ? Optional.empty()
                        : source.location(
                                path.getCompilationUnit(),
                                trees.getSourcePositions()
                                        .getStartPosition(
                                                path.getCompilationUnit(), path.getLeaf()),
                                trees.getSourcePositions()
                                        .getEndPosition(path.getCompilationUnit(), path.getLeaf()));
        JavaSymbolOriginKind origin =
                path == null
                        ? sourceOwner(element)
                                ? JavaSymbolOriginKind.SOURCE_IMPLICIT
                                : JavaSymbolOriginKind.CLASSPATH
                        : elements.getOrigin(element) == Elements.Origin.EXPLICIT
                                ? JavaSymbolOriginKind.SOURCE_EXPLICIT
                                : JavaSymbolOriginKind.SOURCE_IMPLICIT;
        Set<JavaModifier> modifiers =
                element.getModifiers().stream()
                        .<JavaModifier>map(
                                modifier -> JavaStandardModifier.valueOf(modifier.name()))
                        .collect(Collectors.toUnmodifiableSet());
        symbols.put(
                key,
                new JavaSymbolDescription(
                        key,
                        typeMapping.type(element.asType()),
                        modifiers,
                        members.stream().distinct().toList(),
                        supertypes,
                        origin,
                        location));
        if (key instanceof JavaMethodKey method
                && element instanceof ExecutableElement executable) {
            executableAbis.put(method, abis.abi(executable, method.descriptor()));
        }
        if (element instanceof VariableElement variable && variable.getConstantValue() != null) {
            constants.put(key, new JavaConstantValue(variable.getConstantValue()));
        }
        return key;
    }

    private boolean sourceOwner(Element element) {
        for (Element owner = element.getEnclosingElement();
                owner != null;
                owner = owner.getEnclosingElement()) {
            if (owner instanceof TypeElement) {
                return trees.getPath(owner) != null;
            }
        }
        return false;
    }

    private int ordinal(Element element) {
        if (element instanceof VariableElement variable
                && element.getKind() == ElementKind.PARAMETER
                && element.getEnclosingElement() instanceof ExecutableElement owner) {
            int parameter = memberOrdinal(owner.getParameters(), variable);
            if (parameter >= 0) {
                return parameter;
            }
            // Lambda parameters are owned by the enclosing method in the JDK element model,
            // but are not members of that method's declared parameter list.
        }
        if (element instanceof RecordComponentElement component
                && element.getEnclosingElement() instanceof TypeElement owner) {
            return memberOrdinal(owner.getRecordComponents(), component);
        }
        TreePath path = trees.getPath(element);
        if (path == null) {
            return 0;
        }
        long start =
                trees.getSourcePositions()
                        .getStartPosition(path.getCompilationUnit(), path.getLeaf());
        return start < 0 ? 0 : (int) start;
    }

    private static int memberOrdinal(List<? extends Element> members, Element element) {
        int ordinal = members.indexOf(element);
        if (ordinal >= 0) {
            return ordinal;
        }
        // Trees.getScope may return attributed copies rather than the declaration's element.
        for (int index = 0; index < members.size(); index++) {
            if (members.get(index).getSimpleName().contentEquals(element.getSimpleName())) {
                return index;
            }
        }
        return -1;
    }
}
