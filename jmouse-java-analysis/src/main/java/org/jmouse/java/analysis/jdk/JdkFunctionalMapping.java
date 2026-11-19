package org.jmouse.java.analysis.jdk;

import org.jmouse.java.analysis.JavaFunctionalTarget;
import org.jmouse.java.model.symbol.JavaMethodKey;
import org.jmouse.java.model.type.JavaExecutableType;
import org.jmouse.java.model.type.JavaPlatformTypeNames;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

final class JdkFunctionalMapping {
    private record Candidate(ExecutableElement method, ExecutableType signature) {}

    private final Elements elements;
    private final Types types;
    private final JdkSymbolMapping symbols;

    JdkFunctionalMapping(Elements elements, Types types, JdkSymbolMapping symbols) {
        this.elements = elements;
        this.types = types;
        this.symbols = symbols;
    }

    JavaFunctionalTarget target(TypeMirror target) {
        List<Candidate> candidates = new ArrayList<>();
        collect(target, candidates);
        for (Candidate candidate : candidates) {
            if (candidates.stream()
                    .allMatch(
                            other ->
                                    candidate
                                                    .method()
                                                    .getSimpleName()
                                                    .contentEquals(other.method().getSimpleName())
                                            && types.isSubsignature(
                                                    candidate.signature(), other.signature())
                                            && (types.isSameType(
                                                            candidate.signature().getReturnType(),
                                                            other.signature().getReturnType())
                                                    || types.isSubtype(
                                                            candidate.signature().getReturnType(),
                                                            other.signature().getReturnType())))) {
                return new JavaFunctionalTarget(
                        (JavaMethodKey) symbols.describe(candidate.method()),
                        (JavaExecutableType) symbols.typeMapping.type(candidate.signature()));
            }
        }
        return null;
    }

    private void collect(TypeMirror target, List<Candidate> candidates) {
        if (target instanceof IntersectionType intersection) {
            intersection.getBounds().forEach(bound -> collect(bound, candidates));
        } else if (target instanceof DeclaredType declared
                && declared.asElement() instanceof TypeElement type) {
            for (Element member : elements.getAllMembers(type)) {
                if (member.getKind() == ElementKind.METHOD
                        && member.getModifiers().contains(Modifier.ABSTRACT)
                        && !member.getModifiers().contains(Modifier.STATIC)) {
                    ExecutableElement method = (ExecutableElement) member;
                    ExecutableType signature = (ExecutableType) types.asMemberOf(declared, method);
                    if (!objectMethod(method, signature)) {
                        candidates.add(new Candidate(method, signature));
                    }
                }
            }
        }
    }

    private boolean objectMethod(ExecutableElement candidate, ExecutableType signature) {
        for (Element member :
                elements.getTypeElement(JavaPlatformTypeNames.OBJECT).getEnclosedElements()) {
            if (member instanceof ExecutableElement method
                    && member.getModifiers().contains(Modifier.PUBLIC)
                    && method.getSimpleName().contentEquals(candidate.getSimpleName())
                    && types.isSubsignature(signature, (ExecutableType) method.asType())) {
                return true;
            }
        }
        return false;
    }
}
