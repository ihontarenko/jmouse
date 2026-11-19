package org.jmouse.java.parser;

import org.jmouse.java.parser.model.BodyReference;
import org.jmouse.java.parser.model.ExecutableBody;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.LocalBinding;
import org.jmouse.java.parser.projection.JavaExecutableProjection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaExecutableProjectionTest {
    @Test
    void executableReferencesRetainTheirOwnersRolesParametersAndSourceSpans() {
        String text = "class Service { int count; void use(int value) { this.count += value; work(value); }"
                + " void work(int value) {} }";
        JavaSourceFile file = JavaExecutableProjection.project(new JavaSyntaxParser()
                .recoverCompilationUnit("Service.java", text));
        ExecutableBody body = file.bodies().getFirst();

        assertTrue(file.isComplete(), file.problems().toString());
        assertEquals("Service#use(int)", body.owner().value());
        assertEquals("Service", body.scopeType().value());
        assertEquals("value", body.variables().getFirst().name());
        assertEquals("int", body.variables().getFirst().type());
        assertTrue(body.references().stream().anyMatch(reference -> reference.role() == BodyReference.Role.READS_WRITES
                && reference.name().equals("count") && reference.span().slice(text).equals("count")));
        assertTrue(body.references().stream().anyMatch(reference -> reference.role() == BodyReference.Role.CALLS
                && reference.name().equals("work") && reference.owner().equals(body.owner())));
    }

    @Test
    void aLocalVariableRetainsTheNestedBlockScopeAndItsInitializer() {
        String text = "class Example { void use(First target) { { Second target = new Second(); target.run(1); }"
                + " target.run(2); } }";
        JavaSourceFile file = JavaExecutableProjection.project(new JavaSyntaxParser()
                .recoverCompilationUnit("Example.java", text));
        ExecutableBody body = file.bodies().getFirst();
        LocalBinding parameter = body.variables().getFirst();
        LocalBinding local = body.variables().getLast();

        assertEquals(2, body.variables().size());
        assertEquals("First", parameter.type());
        assertEquals("Second", local.type());
        assertEquals("{ Second target = new Second(); target.run(1); }", local.scope().slice(text));
        assertEquals("Second", local.initializer().name());
        assertEquals(1, body.references().stream().filter(reference -> reference.role() == BodyReference.Role.INSTANTIATES).count());
        assertEquals(2, body.references().stream().filter(reference -> reference.role() == BodyReference.Role.CALLS).count());
    }

    @Test
    void constructorsFieldsAndInitializerBlocksHaveSeparateExecutableOwners() {
        String text = "class Example { Object value = new Object(); Example() { ready(); }"
                + " { ready(); } static { ready(); } void ready() {} }";
        JavaSourceFile file = JavaExecutableProjection.project(new JavaSyntaxParser()
                .recoverCompilationUnit("Example.java", text));

        assertEquals(5, file.bodies().size());
        assertEquals("Example#value", file.bodies().getFirst().owner().value());
        assertEquals("Example#Example()", file.bodies().get(1).owner().value());
        assertEquals(3, file.bodies().stream().flatMap(body -> body.references().stream())
                .filter(reference -> reference.role() == BodyReference.Role.CALLS).count());
        assertTrue(file.bodies().stream().allMatch(body -> body.scopeType().value().equals("Example")));
    }

    @Test
    void anExplicitConstructorInvocationKeepsItsKeywordOrigin() {
        String text = "class Child extends Parent { Child() { super(); } }";
        JavaSourceFile file = JavaExecutableProjection.project(new JavaSyntaxParser()
                .recoverCompilationUnit("Child.java", text));
        BodyReference reference = file.bodies().getFirst().references().getFirst();

        assertEquals(BodyReference.Role.CALLS, reference.role());
        assertEquals("super", reference.name());
        assertEquals("super", reference.span().slice(text));
        assertEquals("Child#Child()", reference.owner().value());
    }

    @Test
    void arrayCreationAndElementUpdatesRetainTheirNavigationRoles() {
        String text = "class Arrays { int[] values = new int[] {1, 2}; int next() { return ++values[0]; } }";
        JavaSourceFile file = JavaExecutableProjection.project(new JavaSyntaxParser()
                .recoverCompilationUnit("Arrays.java", text));

        assertTrue(file.isComplete(), file.problems().toString());
        assertEquals(2, file.bodies().size());
        assertEquals("int[]", file.bodies().getFirst().expressions().getFirst().name());
        assertTrue(file.bodies().getLast().references().stream().anyMatch(reference -> reference.name().equals("values")));
    }

    @Test
    void lambdaBindingsAreConfinedToTheLambdaAndCallsAreNotDuplicated() {
        String text = "class Example { void use(String value) { Object action = (String value) -> value.trim();"
                + " value.trim(); } }";
        JavaSourceFile file = JavaExecutableProjection.project(new JavaSyntaxParser()
                .recoverCompilationUnit("Example.java", text));
        ExecutableBody body = file.bodies().getFirst();

        assertEquals(2, body.references().stream().filter(reference -> reference.role() == BodyReference.Role.CALLS).count());
        assertEquals(2, body.variables().stream().filter(variable -> variable.name().equals("value")).count());
        LocalBinding lambda = body.variables().getLast();
        assertEquals("value.trim()", lambda.scope().slice(text));
    }

    @Test
    void conditionalCastsAndClassLiteralsAreProjectedFromTheirTypedNodes() {
        String text = "class Example { Object choose(Object value) { return value instanceof String"
                + " ? (String) value : void.class; } }";
        JavaSourceFile file = JavaExecutableProjection.project(new JavaSyntaxParser()
                .recoverCompilationUnit("Example.java", text));

        assertTrue(file.isComplete(), file.problems().toString());
        assertEquals("?:", file.bodies().getFirst().expressions().getFirst().name());
        assertEquals(3, file.bodies().getFirst().expressions().getFirst().arguments().size());
    }

    @Test
    void switchExpressionCallsAreRetainedAcrossExpressionAndBlockOutcomes() {
        String text = "class Example { int choose(int value) { return switch(value) { case 0 -> first();"
                + " default -> { yield second(); } }; } }";
        JavaSourceFile file = JavaExecutableProjection.project(new JavaSyntaxParser()
                .recoverCompilationUnit("Example.java", text));

        assertTrue(file.isComplete(), file.problems().toString());
        assertEquals(2, file.bodies().getFirst().references().stream()
                .filter(reference -> reference.role() == BodyReference.Role.CALLS).count());
    }

    @Test
    void localAndAnonymousMethodsHaveTheirOwnOwners() {
        String text = "class Example { void use() { class Local { void inside() { localCall(); } }"
                + " Object value = new Object() { void inside() { anonymousCall(); } }; outerCall(); } }";
        JavaSourceFile file = JavaExecutableProjection.project(new JavaSyntaxParser()
                .recoverCompilationUnit("Example.java", text));

        assertEquals(3, file.bodies().size());
        ExecutableBody outer = file.bodies().getFirst();
        assertEquals(1, outer.references().stream().filter(reference -> reference.role() == BodyReference.Role.CALLS).count());
        assertEquals(3, file.bodies().stream().map(ExecutableBody::owner).distinct().count());
        assertTrue(file.bodies().stream().flatMap(body -> body.references().stream())
                .anyMatch(reference -> reference.name().equals("anonymousCall")
                        && !reference.owner().equals(outer.owner())));
    }
}
