package org.jmouse.java.analysis.jdk;

import org.jmouse.java.analysis.JavaBridgeRequirement;
import org.jmouse.java.analysis.JavaOverrideRelation;
import org.jmouse.java.model.symbol.JavaMethodKey;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

final class JdkOverrideMapping {
    private final Elements elements;
    private final Types types;
    private final JdkSymbolMapping symbols;

    JdkOverrideMapping(Elements elements, Types types, JdkSymbolMapping symbols) {
        this.elements = elements;
        this.types = types;
        this.symbols = symbols;
    }

    void collect(TypeElement owner) {
        ArrayDeque<TypeMirror> pending = new ArrayDeque<>(types.directSupertypes(owner.asType()));
        Set<Element> visited = new HashSet<>();
        Set<ExecutableElement> inherited = new LinkedHashSet<>();
        while (!pending.isEmpty()) {
            TypeMirror parent = pending.removeFirst();
            Element element = types.asElement(parent);
            if (element == null || !visited.add(element)) {
                continue;
            }
            for (Element member : element.getEnclosedElements()) {
                if (member.getKind() == ElementKind.METHOD) {
                    inherited.add((ExecutableElement) member);
                }
            }
            pending.addAll(types.directSupertypes(parent));
        }
        for (Element member : owner.getEnclosedElements()) {
            if (member.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement implementation = (ExecutableElement) member;
            for (ExecutableElement declaration : inherited) {
                if (elements.overrides(implementation, declaration, owner)) {
                    JavaMethodKey implementingKey =
                            (JavaMethodKey) symbols.describe(implementation);
                    JavaMethodKey inheritedKey = (JavaMethodKey) symbols.describe(declaration);
                    JavaBridgeRequirement bridge =
                            implementingKey.descriptor().equals(inheritedKey.descriptor())
                                    ? JavaBridgeRequirement.NONE
                                    : JavaBridgeRequirement.REQUIRED;
                    symbols.overrides.add(
                            new JavaOverrideRelation(implementingKey, inheritedKey, bridge));
                }
            }
        }
    }
}
