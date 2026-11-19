package org.jmouse.java.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.java.model.symbol.JavaFieldKey;
import org.jmouse.java.model.symbol.JavaJvmNames;
import org.jmouse.java.model.symbol.JavaVariableKey;
import org.jmouse.java.model.type.JavaDeclaredType;
import org.jmouse.java.parser.JavaSyntaxParser;
import org.jmouse.java.parser.JavaSyntaxResult;
import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaLambdaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodInvocationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodReferenceExpressionNode;
import org.jmouse.java.parser.node.expression.JavaNameExpressionNode;
import org.jmouse.java.parser.node.statement.JavaBreakStatementNode;
import org.jmouse.java.parser.node.statement.JavaContinueStatementNode;
import org.jmouse.java.parser.node.statement.JavaWhileStatementNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

class JavaSemanticAnalyzerTest {
    @Test
    void sourceUnitsShareCanonicalSymbolsWithoutUsingTheHostApplicationClasspath() {
        JavaSyntaxParser parser = new JavaSyntaxParser();
        JavaSyntaxResult<JavaCompilationUnitNode> api =
                parser.parseSource(
                        JavaUnitRoles.COMPILATION_UNIT,
                        "sample/Api.java",
                        "package sample; public class Api { public static String text() { return"
                                + " \"text\"; } }");
        JavaSyntaxResult<JavaCompilationUnitNode> consumer =
                parser.parseSource(
                        JavaUnitRoles.COMPILATION_UNIT,
                        "sample/Consumer.java",
                        "package sample; class Consumer { String run() { return Api.text(); } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(api, consumer)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        assertEquals(
                "sample.Api",
                result.program()
                        .invocation(invocation(consumer, "text"))
                        .orElseThrow()
                        .target()
                        .owner()
                        .binaryName());
        JavaAnalysisResult isolated =
                new JavaSemanticAnalyzer()
                        .analyze(
                                JavaAnalysisRequest.of(
                                        List.of(
                                                syntax(
                                                        "class Example {"
                                                            + " org.jmouse.java.analysis.JavaSemanticAnalyzer"
                                                            + " analyzer; }"))));
        assertFalse(isolated.isSuccessful());
    }

    @Test
    void arrayOperationsDoNotInventAClasspathClassForTheCompilerArrayPseudoType() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                syntax(
                        "class Example { int[] copy(int[] input) { int size = input.length; return"
                                + " input.clone(); } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        JavaInvocation clone =
                result.program().invocation(invocation(syntax, "clone")).orElseThrow();
        assertEquals("java.lang.Object", clone.target().owner().binaryName());
        assertTrue(
                result.program()
                        .intrinsics()
                        .containsValue(JavaStandardIntrinsicOperation.ARRAY_CLONE));
        assertTrue(
                result.program()
                        .intrinsics()
                        .containsValue(JavaStandardIntrinsicOperation.ARRAY_LENGTH));
    }

    @Test
    void nestedArrayInitializersAndTheirElementsHaveAttributedTypes() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                syntax("class Example { int[][] values = new int[][] {{1}, {2}}; }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        syntax.root()
                .accept(
                        new Visitor() {
                            @Override
                            public void visit(Node node) {
                                if (node instanceof JavaExpressionNode expression) {
                                    assertTrue(
                                            result.program().facts(expression).isPresent(),
                                            expression.schema().kind().name());
                                }
                            }
                        });
    }

    @Test
    void aBoundMethodReferenceCapturesItsValueQualifierOnce() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                syntax("class Example { Runnable bound(String value) { return value::trim; } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        List<JavaMethodReferenceExpressionNode> references = new ArrayList<>();
        syntax.root()
                .accept(
                        new Visitor() {
                            @Override
                            public void visit(Node node) {
                                if (node instanceof JavaMethodReferenceExpressionNode reference) {
                                    references.add(reference);
                                }
                            }
                        });
        JavaMethodReferenceExpressionNode reference = references.getFirst();
        assertInstanceOf(JavaNameExpressionNode.class, reference.qualifier());
        assertEquals(1, result.program().captures(reference).size());
        assertInstanceOf(
                JavaCapturedExpression.class, result.program().captures(reference).getFirst());
        JavaFunctionalTarget functional =
                result.program()
                        .functionalTargets()
                        .get(result.program().identities().get(reference));
        assertEquals("run", functional.declaration().name());
        assertEquals("void", functional.signature().result().displayName());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "class Example { int[] copy(int[] input) { return input.clone(); } }",
                "record Example(int x, int y) { int sum(Object value) { return switch(value) { case"
                    + " Example(int x, int y) -> x + y; default -> 0; }; } }",
                "class Example { Number read(java.util.List<? extends Number> values) { return"
                        + " values.get(0); } }",
                "class Example<T extends Comparable<T>> { T compare(T value) {"
                        + " value.compareTo(value); return value; } }",
                "class Example { void run() throws Exception { try(java.io.StringReader reader ="
                        + " new java.io.StringReader(\"text\")) { reader.read(); }"
                        + " catch(java.io.IOException | IllegalArgumentException failure) { throw"
                        + " failure; } } }",
                "class Example { static { if(true) throw new AssertionError(); } }",
                "class Example { java.util.function.Function<Integer, String> run() { return value"
                        + " -> value.toString(); } }",
                "class Example { java.util.function.Function<String, Integer> run() { return"
                        + " String::length; } }",
                "class Example { java.util.function.IntFunction<int[]> run() { return int[]::new; }"
                        + " }",
                "class Outer { class Inner {} } class Example extends Outer.Inner { Example() { new"
                        + " Outer().super(); } }",
                "class Example { int read(Object input) { return (int) input; } }",
                "class Example { Byte small = 1; java.util.List<String> values = new"
                        + " java.util.ArrayList(); }",
                "class Example { int[] values = new int[] {1, 2}; int[][] matrix = {{1}, {2}}; }",
                "class Example { Runnable bound(String value) { return value::trim; } }",
                "class Example { Runnable run(int seed) { class Local implements Runnable { public"
                        + " void run() { System.out.println(seed); } } return new Local(); } }",
                "record Example(int value) { Example { if(value < 0) throw new"
                        + " IllegalArgumentException(); } }",
                "class Example { int run(int value) { return switch(value) { case 1 -> 2; default"
                        + " -> { yield 3; } }; } }"
            })
    void validJavaBodiesProduceAnAnalyzedProgram(String text) {
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax(text))));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
    }

    @Test
    void genericOverridesRetainTheErasedInheritedSignatureForBridgeGeneration() {
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer()
                        .analyze(
                                JavaAnalysisRequest.of(
                                        List.of(
                                                syntax(
                                                        "interface Parent<T> { T value(); } class"
                                                            + " Example implements Parent<String> {"
                                                            + " public String value() { return"
                                                            + " \"value\"; } }"))));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        JavaOverrideRelation relation =
                result.program().overrides().stream()
                        .filter(
                                override ->
                                        override.implementation()
                                                .owner()
                                                .binaryName()
                                                .equals("Example"))
                        .findFirst()
                        .orElseThrow();
        assertEquals("()Ljava/lang/String;", relation.implementation().descriptor().encoding());
        assertEquals("()Ljava/lang/Object;", relation.declaration().descriptor().encoding());
        assertEquals(JavaBridgeRequirement.REQUIRED, relation.bridge());
    }

    @Test
    void constantValuesAndAssignmentConversionsAreAvailableWithoutEvaluatingSource() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                syntax(
                        "class Example { static final int LIMIT = 6 * 7; byte small = LIMIT;"
                            + " Integer boxed = LIMIT; long run(int value) { long widened = value;"
                            + " return value; } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        assertEquals(
                42,
                result.program().constants().entrySet().stream()
                        .filter(
                                entry ->
                                        entry.getKey() instanceof JavaFieldKey field
                                                && field.name().equals("LIMIT"))
                        .findFirst()
                        .orElseThrow()
                        .getValue()
                        .value());
        List<JavaConversionKind> conversions =
                result.program().facts().values().stream()
                        .flatMap(facts -> facts.conversions().stream())
                        .map(JavaConversion::kind)
                        .toList();
        assertTrue(conversions.contains(JavaConversionKind.CONSTANT_NARROWING));
        assertTrue(conversions.contains(JavaConversionKind.BOXING));
        assertTrue(conversions.contains(JavaConversionKind.WIDENING_PRIMITIVE));
    }

    @Test
    void lambdaCapturesKeepTheCanonicalOuterVariableIdentity() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                syntax(
                        "class Example { Runnable run(int seed) {"
                                + " return () -> System.out.println(seed); } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        List<JavaLambdaExpressionNode> lambdas = new ArrayList<>();
        syntax.root()
                .accept(
                        new Visitor() {
                            @Override
                            public void visit(Node node) {
                                if (node instanceof JavaLambdaExpressionNode lambda) {
                                    lambdas.add(lambda);
                                }
                            }
                        });
        JavaCapturedSymbol capture =
                assertInstanceOf(
                        JavaCapturedSymbol.class,
                        result.program().captures(lambdas.getFirst()).getFirst());
        assertEquals("seed", ((JavaVariableKey) capture.symbol()).name());
        assertEquals("int", capture.type().displayName());
    }

    @Test
    void enumConstructorsExposeTheirMandatedJvmParameters() {
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer()
                        .analyze(
                                JavaAnalysisRequest.of(
                                        List.of(
                                                syntax(
                                                        "enum Example { ONE(1); Example(int value)"
                                                                + " {} }"))));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        assertEquals(
                "(Ljava/lang/String;II)V",
                result.program().executableAbis().entrySet().stream()
                        .filter(
                                entry ->
                                        entry.getKey().owner().binaryName().equals("Example")
                                                && entry.getKey()
                                                        .name()
                                                        .equals(JavaJvmNames.CONSTRUCTOR))
                        .findFirst()
                        .orElseThrow()
                        .getValue()
                        .effectiveDescriptor()
                        .orElseThrow()
                        .encoding());
    }

    @Test
    void flowScopesDistinguishAPatternBindingFromTheFieldItShadows() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                syntax(
                        "class Example { Object value; void run(Object input) { if(input instanceof"
                                + " String value) { value.trim(); } value.toString(); } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        List<JavaNameExpressionNode> names = new ArrayList<>();
        syntax.root()
                .accept(
                        new Visitor() {
                            @Override
                            public void visit(Node node) {
                                if (node instanceof JavaNameExpressionNode name
                                        && name.name()
                                                .segments()
                                                .getFirst()
                                                .name()
                                                .equals("value")) {
                                    names.add(name);
                                }
                            }
                        });
        assertInstanceOf(
                JavaVariableKey.class,
                result.program().facts(names.getFirst()).orElseThrow().symbol().orElseThrow());
        assertInstanceOf(
                JavaFieldKey.class,
                result.program().facts(names.getLast()).orElseThrow().symbol().orElseThrow());
        assertEquals(
                "java.lang.String",
                result.program()
                        .scopeOf(names.getFirst())
                        .orElseThrow()
                        .lookup("value")
                        .getFirst()
                        .type()
                        .displayName());
    }

    @Test
    void labelledControlStatementsBindToTheirActualLoopTargets() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                syntax(
                        "class Example { boolean test() { return true; } void run() { outer:"
                                + " while(true) { if(test()) continue outer; break outer; } } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        List<JavaSyntaxNode> targets = new ArrayList<>();
        List<JavaSyntaxNode> transfers = new ArrayList<>();
        syntax.root()
                .accept(
                        new Visitor() {
                            @Override
                            public void visit(Node node) {
                                if (node instanceof JavaWhileStatementNode loop) {
                                    targets.add(loop);
                                } else if (node instanceof JavaContinueStatementNode transfer) {
                                    transfers.add(transfer);
                                } else if (node instanceof JavaBreakStatementNode transfer) {
                                    transfers.add(transfer);
                                }
                            }
                        });
        for (JavaSyntaxNode transfer : transfers) {
            assertTrue(result.program().facts(transfer).orElseThrow().controlTarget().isPresent());
        }
        assertEquals(
                result.program().identities().get(targets.getFirst()),
                result.program()
                        .facts(transfers.getFirst())
                        .orElseThrow()
                        .controlTarget()
                        .orElseThrow());
    }

    @Test
    void variableArityAndElementBoxingAreExplicitCallFacts() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                syntax(
                        "class Example { static void collect(String first, Integer... rest) {} void"
                                + " run() { collect(\"x\", 1, 2); } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        JavaInvocation call =
                result.program().invocation(invocation(syntax, "collect")).orElseThrow();
        assertEquals(JavaInvocationArity.VARIABLE, call.arity());
        assertEquals(JavaDispatchKind.STATIC, call.dispatch());
        assertEquals(
                "(Ljava/lang/String;[Ljava/lang/Integer;)V", call.target().descriptor().encoding());
        assertEquals(JavaConversionKind.BOXING, call.conversions().get(1).kind());
        assertEquals(JavaConversionKind.BOXING, call.conversions().get(2).kind());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "class Example { void unused() { int value; System.out.println(value); } }",
                "class Hidden { private void secret() {} } class Example { void run(Hidden value) {"
                        + " value.secret(); } }",
                "class Example { void use(String value) {} void use(Integer value) {} void run() {"
                        + " use(null); } }",
                "class Example { byte value = 128; }"
            })
    void semanticErrorsInSelectedUnitsAreNeverReportedAsSuccessfulAnalysis(String text) {
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax(text))));
        assertFalse(result.isSuccessful());
        assertTrue(
                result.diagnostics().stream()
                        .anyMatch(diagnostic -> diagnostic.code().startsWith("compiler.err.")));
        assertThrows(IllegalStateException.class, result::requireSuccessful);
    }

    private static JavaSyntaxResult<JavaCompilationUnitNode> syntax(String text) {
        return new JavaSyntaxParser()
                .parseSource(JavaUnitRoles.COMPILATION_UNIT, "Example.java", text);
    }

    private static JavaMethodInvocationExpressionNode invocation(
            JavaSyntaxResult<JavaCompilationUnitNode> syntax, String name) {
        List<JavaMethodInvocationExpressionNode> matches = new ArrayList<>();
        syntax.root()
                .accept(
                        new Visitor() {
                            @Override
                            public void visit(Node node) {
                                if (node instanceof JavaMethodInvocationExpressionNode call
                                        && call.name().name().equals(name)) {
                                    matches.add(call);
                                }
                            }
                        });
        return matches.getFirst();
    }

    @Test
    void genericInferenceAndBoxingAreRecordedWithoutErasingTheExpressionType() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                syntax(
                        "import java.util.List; import java.util.Collections; class Example {"
                            + " List<Integer> run() { return Collections.singletonList(1); } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        JavaInvocation call =
                result.program().invocation(invocation(syntax, "singletonList")).orElseThrow();
        assertEquals("(Ljava/lang/Object;)Ljava/util/List;", call.target().descriptor().encoding());
        assertEquals(
                "java.lang.Integer",
                ((JavaDeclaredType) call.resultType()).arguments().getFirst().displayName());
        assertEquals(JavaConversionKind.BOXING, call.conversions().getFirst().kind());
    }

    @Test
    void overloadSelectionProducesTheCanonicalDeclarationDescriptor() {
        JavaSyntaxResult<JavaCompilationUnitNode> syntax =
                new JavaSyntaxParser()
                        .parseSource(
                                JavaUnitRoles.COMPILATION_UNIT,
                                "Example.java",
                                "class Example { Object choose(Object value) { return value; }"
                                    + " String choose(String value) { return value; } String run()"
                                    + " { return choose(\"text\"); } }");
        JavaAnalysisResult result =
                new JavaSemanticAnalyzer().analyze(JavaAnalysisRequest.of(List.of(syntax)));
        assertTrue(result.isSuccessful(), result.diagnostics().toString());
        syntax.root()
                .accept(
                        new Visitor() {
                            @Override
                            public void visit(Node node) {
                                if (node instanceof JavaMethodInvocationExpressionNode invocation) {
                                    assertEquals(
                                            "(Ljava/lang/String;)Ljava/lang/String;",
                                            result.program()
                                                    .invocation(invocation)
                                                    .orElseThrow()
                                                    .target()
                                                    .descriptor()
                                                    .encoding());
                                    assertEquals(
                                            "Example",
                                            result.program()
                                                    .invocation(invocation)
                                                    .orElseThrow()
                                                    .target()
                                                    .owner()
                                                    .binaryName());
                                }
                            }
                        });
    }
}
