package org.jmouse.java.analysis;

import org.jmouse.java.model.symbol.JavaConstantValue;
import org.jmouse.java.model.symbol.JavaExecutableAbi;
import org.jmouse.java.model.symbol.JavaMethodKey;
import org.jmouse.java.model.symbol.JavaSymbolDescription;
import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.symbol.JavaTypeVariableKey;
import org.jmouse.java.model.type.JavaType;
import org.jmouse.java.parser.node.JavaSyntaxNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Immutable side tables. Syntax ownership and source spans remain unchanged by analysis. */
public record JavaAnalyzedProgram(
        Map<JavaSyntaxNode, JavaNodeIdentity> identities,
        Map<JavaNodeIdentity, JavaNodeFacts> facts,
        Map<JavaSymbolKey, JavaSymbolDescription> symbols,
        Map<JavaTypeVariableKey, JavaType> upperBounds,
        Map<JavaNodeIdentity, JavaScopeView> scopes,
        Map<JavaMethodKey, JavaExecutableAbi> executableAbis,
        Map<JavaNodeIdentity, List<JavaCapturedValue>> capturedValues,
        Map<JavaSymbolKey, JavaConstantValue> constants,
        List<JavaOverrideRelation> overrides,
        Map<JavaNodeIdentity, JavaFunctionalTarget> functionalTargets,
        Map<JavaNodeIdentity, JavaIntrinsicOperation> intrinsics,
        JavaSemanticCoverage coverage) {
    public JavaAnalyzedProgram {
        identities = Map.copyOf(identities);
        facts = Map.copyOf(facts);
        symbols = Map.copyOf(symbols);
        upperBounds = Map.copyOf(upperBounds);
        scopes = Map.copyOf(scopes);
        executableAbis = Map.copyOf(executableAbis);
        constants = Map.copyOf(constants);
        overrides = List.copyOf(overrides);
        functionalTargets = Map.copyOf(functionalTargets);
        intrinsics = Map.copyOf(intrinsics);
        capturedValues =
                capturedValues.entrySet().stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        Objects.requireNonNull(coverage, "coverage");
    }

    public Optional<JavaNodeFacts> facts(JavaSyntaxNode node) {
        return Optional.ofNullable(identities.get(node)).map(facts::get);
    }

    public Optional<JavaInvocation> invocation(JavaSyntaxNode node) {
        return facts(node).flatMap(JavaNodeFacts::invocation);
    }

    public Optional<JavaScopeView> scopeOf(JavaSyntaxNode node) {
        return Optional.ofNullable(identities.get(node)).map(scopes::get);
    }

    public List<JavaCapturedValue> captures(JavaSyntaxNode node) {
        JavaNodeIdentity identity = identities.get(node);
        return identity == null ? List.of() : capturedValues.getOrDefault(identity, List.of());
    }
}
