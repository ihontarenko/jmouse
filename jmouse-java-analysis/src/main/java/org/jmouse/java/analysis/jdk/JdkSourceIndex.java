package org.jmouse.java.analysis.jdk;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.java.analysis.JavaCapturedValue;
import org.jmouse.java.analysis.JavaConversion;
import org.jmouse.java.analysis.JavaFunctionalTarget;
import org.jmouse.java.analysis.JavaIntrinsicOperation;
import org.jmouse.java.analysis.JavaInvocation;
import org.jmouse.java.analysis.JavaNodeFacts;
import org.jmouse.java.analysis.JavaNodeIdentity;
import org.jmouse.java.analysis.JavaScopeView;
import org.jmouse.java.model.symbol.JavaSourceLocation;
import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.type.JavaType;
import org.jmouse.java.parser.JavaSyntaxResult;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.declaration.JavaRecordComponentNode;
import org.jmouse.java.parser.node.declaration.JavaVariableDeclaratorNode;
import org.jmouse.java.parser.node.expression.JavaClassInstanceCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodInvocationExpressionNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.parameter.JavaFormalParameterNode;
import org.jmouse.java.parser.node.parameter.JavaInferredLambdaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaVariableArityParameterNode;
import org.jmouse.java.parser.node.pattern.JavaTypePatternNode;
import org.jmouse.java.parser.node.resource.JavaResourceVariableDeclarationNode;
import org.jmouse.java.parser.node.statement.JavaBreakStatementNode;
import org.jmouse.java.parser.node.statement.JavaContinueStatementNode;
import org.jmouse.java.parser.node.statement.JavaYieldStatementNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

final class JdkSourceIndex {
    private record Range(int start, int end) {}

    record VariableAnchor(JavaSyntaxNode declaration, JavaIdentifierNode name) {}

    static final class Unit {
        final JavaSyntaxResult<JavaCompilationUnitNode> syntax;
        final Map<Range, List<JavaSyntaxNode>> ranges = new LinkedHashMap<>();
        final Map<String, NavigableMap<Integer, VariableAnchor>> variables = new LinkedHashMap<>();

        Unit(JavaSyntaxResult<JavaCompilationUnitNode> syntax) {
            this.syntax = syntax;
        }
    }

    final Map<JavaSyntaxNode, JavaNodeIdentity> identities = new LinkedHashMap<>();
    final Map<JavaNodeIdentity, JavaNodeFacts> facts = new LinkedHashMap<>();
    final Map<JavaNodeIdentity, JavaScopeView> scopes = new LinkedHashMap<>();
    final Map<JavaNodeIdentity, List<JavaCapturedValue>> capturedValues = new LinkedHashMap<>();
    final Map<JavaNodeIdentity, JavaFunctionalTarget> functionalTargets = new LinkedHashMap<>();
    final Map<JavaNodeIdentity, JavaIntrinsicOperation> intrinsics = new LinkedHashMap<>();
    final List<JavaSyntaxNode> requiredInvocations = new ArrayList<>();
    final List<JavaSyntaxNode> requiredControlTargets = new ArrayList<>();
    final List<JavaExpressionNode> requiredExpressions = new ArrayList<>();
    private final Map<URI, Unit> units = new LinkedHashMap<>();

    String snapshotIdentity() {
        StringBuilder identity = new StringBuilder();
        units.values().stream()
                .sorted(Comparator.comparing(unit -> unit.syntax.source().name()))
                .forEach(
                        unit ->
                                identity.append(unit.syntax.source().name())
                                        .append('\n')
                                        .append(digest(unit.syntax.source().text()))
                                        .append('\n')
                                        .append(unit.syntax.profile().release().number())
                                        .append('\n')
                                        .append(unit.syntax.profile().preview())
                                        .append('\n')
                                        .append(
                                                unit.syntax
                                                        .profile()
                                                        .specification()
                                                        .specificationFingerprint())
                                        .append('\n'));
        return digest(identity.toString());
    }

    void add(JdkSourceFile file, JavaSyntaxResult<JavaCompilationUnitNode> syntax) {
        Unit unit = new Unit(syntax);
        if (units.putIfAbsent(file.toUri(), unit) != null) {
            throw new IllegalArgumentException("Duplicate normalized source identity");
        }
        String snapshot = digest(syntax.source().text());
        syntax.root()
                .accept(
                        new Visitor() {
                            private int ordinal;

                            @Override
                            public void visit(Node node) {
                                JavaSyntaxNode source = (JavaSyntaxNode) node;
                                identities.put(
                                        source,
                                        new JavaNodeIdentity(
                                                syntax.source().name(), snapshot, ordinal++));
                                Range range =
                                        new Range(
                                                source.span().start().offset(),
                                                source.span().end().offset());
                                unit.ranges
                                        .computeIfAbsent(range, ignored -> new ArrayList<>())
                                        .add(source);
                                VariableAnchor anchor = anchor(source);
                                if (source instanceof JavaExpressionNode expression) {
                                    requiredExpressions.add(expression);
                                }
                                if (anchor != null) {
                                    unit.variables
                                            .computeIfAbsent(
                                                    anchor.name().name(),
                                                    ignored -> new TreeMap<>())
                                            .put(anchor.name().span().start().offset(), anchor);
                                }
                                if (source instanceof JavaMethodInvocationExpressionNode
                                        || source instanceof JavaClassInstanceCreationExpressionNode
                                        || source instanceof JavaConstructorInvocationNode) {
                                    requiredInvocations.add(source);
                                }
                                if (source instanceof JavaBreakStatementNode
                                        || source instanceof JavaContinueStatementNode
                                        || source instanceof JavaYieldStatementNode) {
                                    requiredControlTargets.add(source);
                                }
                            }
                        });
    }

    Unit unit(CompilationUnitTree tree) {
        return units.get(tree.getSourceFile().toUri());
    }

    Optional<JavaSourceLocation> location(CompilationUnitTree tree, long start, long end) {
        Unit unit = unit(tree);
        if (unit == null
                || start < 0
                || end < start
                || end > unit.syntax.source().text().length()) {
            return Optional.empty();
        }
        return Optional.of(
                new JavaSourceLocation(unit.syntax.source().name(), (int) start, (int) end));
    }

    List<JavaSyntaxNode> nodes(CompilationUnitTree tree, long start, long end) {
        Unit unit = unit(tree);
        if (unit == null || start < 0 || end < start || end > Integer.MAX_VALUE) {
            return List.of();
        }
        return unit.ranges.getOrDefault(new Range((int) start, (int) end), List.of());
    }

    VariableAnchor variable(
            CompilationUnitTree unitTree, VariableTree tree, SourcePositions positions) {
        Unit unit = unit(unitTree);
        long start = positions.getStartPosition(unitTree, tree);
        long end =
                tree.getInitializer() == null
                        ? positions.getEndPosition(unitTree, tree)
                        : positions.getStartPosition(unitTree, tree.getInitializer());
        if (unit == null || start < 0 || end < start || end > Integer.MAX_VALUE) {
            return null;
        }
        NavigableMap<Integer, VariableAnchor> names = unit.variables.get(tree.getName().toString());
        if (names == null) {
            return null;
        }
        Map.Entry<Integer, VariableAnchor> candidate = names.ceilingEntry((int) start);
        return candidate != null && candidate.getKey() < end ? candidate.getValue() : null;
    }

    void bind(JavaSyntaxNode node, JavaType type, JavaSymbolKey symbol) {
        JavaNodeIdentity identity = identities.get(node);
        JavaNodeFacts previous = facts.get(identity);
        facts.put(
                identity,
                new JavaNodeFacts(
                        type,
                        Optional.ofNullable(symbol),
                        previous == null ? Optional.empty() : previous.invocation(),
                        previous == null ? Optional.empty() : previous.controlTarget(),
                        previous == null ? List.of() : previous.conversions()));
    }

    void invocation(JavaSyntaxNode node, JavaInvocation invocation) {
        JavaNodeIdentity identity = identities.get(node);
        JavaNodeFacts previous = facts.get(identity);
        facts.put(
                identity,
                new JavaNodeFacts(
                        invocation.resultType(),
                        Optional.of(invocation.target()),
                        Optional.of(invocation),
                        previous == null ? Optional.empty() : previous.controlTarget(),
                        invocation.conversions()));
        if (node instanceof JavaMethodInvocationExpressionNode call) {
            bind(call.name(), invocation.selectedType(), invocation.target());
        }
    }

    void control(JavaSyntaxNode node, JavaSyntaxNode target) {
        JavaNodeIdentity identity = identities.get(node);
        JavaNodeFacts previous = facts.get(identity);
        facts.put(
                identity,
                new JavaNodeFacts(
                        previous.type(),
                        previous.symbol(),
                        previous.invocation(),
                        Optional.of(identities.get(target)),
                        previous.conversions()));
    }

    void capture(JavaSyntaxNode site, JavaCapturedValue value) {
        List<JavaCapturedValue> values =
                capturedValues.computeIfAbsent(identities.get(site), ignored -> new ArrayList<>());
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    void conversion(JavaSyntaxNode expression, JavaConversion conversion) {
        JavaNodeIdentity identity = identities.get(expression);
        JavaNodeFacts previous = facts.get(identity);
        if (previous != null) {
            List<JavaConversion> values = new ArrayList<>(previous.conversions());
            values.add(conversion);
            facts.put(
                    identity,
                    new JavaNodeFacts(
                            previous.type(),
                            previous.symbol(),
                            previous.invocation(),
                            previous.controlTarget(),
                            values));
        }
    }

    private static VariableAnchor anchor(JavaSyntaxNode node) {
        return switch (node) {
            case JavaVariableDeclaratorNode variable ->
                    new VariableAnchor(variable, variable.name());
            case JavaFormalParameterNode parameter ->
                    new VariableAnchor(parameter, parameter.name());
            case JavaVariableArityParameterNode parameter ->
                    new VariableAnchor(parameter, parameter.name());
            case JavaInferredLambdaParameterNode parameter ->
                    new VariableAnchor(parameter, parameter.name());
            case JavaRecordComponentNode component ->
                    new VariableAnchor(component, component.name());
            case JavaResourceVariableDeclarationNode resource ->
                    new VariableAnchor(resource, resource.name());
            case JavaTypePatternNode pattern -> new VariableAnchor(pattern, pattern.binding());
            default -> null;
        };
    }

    private static String digest(String text) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }
}
