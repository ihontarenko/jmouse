package org.jmouse.java.parser.extension.fixture;

import java.math.BigInteger;
import org.jmouse.el.node.NodeLifecycle;
import org.jmouse.java.model.version.JavaRelease;
import org.jmouse.java.parser.JavaSyntaxParser;
import org.jmouse.java.parser.extension.JavaSourceLanguage;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.node.expression.JavaIntegerLiteralNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaSourceExtensionTest {
    @Test
    void anotherPackageAddsAReleaseKeywordsLexingRulesAndNewAstFormsThroughPublicContracts() {
        JavaSourceLanguage baseline = JavaSourceLanguage.standard();
        JavaSourceLanguage extended = baseline.toBuilder().install(new FixtureSourceExtension()).build();
        JavaSyntaxParser parser = new JavaSyntaxParser(extended, JavaSyntaxParser.defaultBudget());
        JavaCompilationUnitNode unit = parser.parseCompilationUnit("Example.java",
                "import bundle sample; class Example { void run() { trace 7; !> 8; } }");
        assertInstanceOf(FixtureBundleImportNode.class, unit.imports().getFirst());
        FixtureTraceNode trace = assertInstanceOf(FixtureTraceNode.class, parser.parseStatement("Trace.java", "trace 7;"));
        assertEquals(BigInteger.valueOf(7), assertInstanceOf(JavaIntegerLiteralNode.class, trace.expression()).value());
        assertEquals(NodeLifecycle.FROZEN, trace.lifecycle());
        assertEquals(new JavaRelease(FixtureNames.SOURCE_RELEASE), extended.profile().release());
        assertEquals(new JavaRelease(21), baseline.profile().release());
        assertThrows(JavaSyntaxException.class,
                () -> new JavaSyntaxParser(baseline, JavaSyntaxParser.defaultBudget()).parseStatement("Trace.java", "trace 7;"));
        assertThrows(IllegalStateException.class,
                () -> extended.rules().bind(JavaUnitRoles.COMPILATION_UNIT, FixtureNames.TRACE_BINDING, (input, session) -> null));
    }
}
