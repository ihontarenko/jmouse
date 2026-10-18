package org.jmouse.java.index;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.java.parser.JavaParser;
import org.jmouse.java.parser.model.BodyReference;
import org.jmouse.java.parser.source.ParseProblem;
import org.junit.jupiter.api.Test;

import java.util.List;

class JavaIndexContractTest {
    private final JavaParser parser = new JavaParser();

    private FileIndex file(String source, String text) {
        return FileIndex.of(parser.parse(source, text, JavaParser.Level.EXPRESSIONS));
    }

    @Test
    void duplicateQualifiedNamesRemainAmbiguousForBothImportForms() {
        CodeIndex symbols = new CodeIndex();
        symbols.put(file("module-a/Target.java", "package shared; public class Target {}"));
        symbols.put(file("module-b/Target.java", "package shared; public class Target {}"));
        for (String imported : List.of("shared.Target", "shared.*")) {
            FileIndex consumer =
                    file(
                            "Consumer.java",
                            "package client; import "
                                    + imported
                                    + "; class Consumer { Target target; }");
            Resolution result =
                    new TypeResolver(symbols).resolve(consumer, consumer.references().getFirst());
            assertInstanceOf(Resolution.Ambiguous.class, result);
            assertEquals(2, ((Resolution.Ambiguous) result).candidates().size());
        }
    }

    @Test
    void retainsDocumentationImportsAndBodyRolesWithOriginalSpans() {
        String text =
                "package example;\r\n"
                    + "import java.util.List;\r\n"
                    + "/** See {@link Service#run(int)}. */\r\n"
                    + "class Service { int count; void run(int value) { this.count += value; } }";
        FileIndex file = file("Service.java", text);
        var occurrences = SourceReferences.of(file);
        assertTrue(
                occurrences.stream()
                        .anyMatch(
                                reference ->
                                        reference.origin() == SourceReference.Origin.DOCUMENTATION
                                                && reference.target().equals("Service#run(int)")
                                                && text.substring(
                                                                reference.span().start().offset(),
                                                                reference.span().end().offset())
                                                        .contains("{@link")));
        assertTrue(
                occurrences.stream()
                        .anyMatch(reference -> reference.role() == SourceReference.Role.IMPORTS));
        assertTrue(
                occurrences.stream()
                        .anyMatch(
                                reference ->
                                        reference.role() == SourceReference.Role.READS_WRITES));
        assertEquals(file.javaDocs(), file.withSource("renamed.java").javaDocs());
    }

    @Test
    void resolvesOverloadsReceiversAndInheritedMembers() {
        FileIndex file =
                file(
                        "Sample.java",
                        "package sample; class Parent { void run(int value) {} } class Other { void"
                            + " run(int value) {} } class Service extends Parent { void run(String"
                            + " value) {} void use(Service service, Other other) { service.run(1);"
                            + " service.run(\"text\"); other.run(2); } }");
        CodeIndex symbols = new CodeIndex();
        symbols.put(file);
        var targets =
                file.bodies().stream()
                        .flatMap(body -> body.references().stream())
                        .filter(
                                reference ->
                                        reference.role() == BodyReference.Role.CALLS
                                                && reference.name().equals("run"))
                        .map(
                                reference ->
                                        ((MemberResolution.Resolved)
                                                        symbols.resolveMember(
                                                                file.source(), reference))
                                                .target()
                                                .symbolId()
                                                .value())
                        .toList();
        assertEquals(
                List.of(
                        "sample.Parent#run(int)",
                        "sample.Service#run(String)",
                        "sample.Other#run(int)"),
                targets);
    }

    @Test
    void partialBodyDoesNotInvalidateHeadersOrCreateDefiniteUsages() {
        FileIndex file =
                file(
                        "Partial.java",
                        "class Partial { void broken() { receiver.call(; next(); } void valid() {"
                                + " valid(); } }");
        assertTrue(file.isStructureComplete());
        assertTrue(
                file.problems().stream()
                        .allMatch(problem -> problem.phase() == ParseProblem.Phase.BODY));
        var broken =
                file.bodies().stream()
                        .filter(body -> body.owner().value().contains("broken"))
                        .findFirst()
                        .orElseThrow();
        assertFalse(file.isBodyComplete(broken.owner()));
        assertFalse(broken.references().isEmpty());
        CodeIndex symbols = new CodeIndex();
        symbols.put(file);
        assertInstanceOf(
                MemberResolution.Unresolved.class,
                symbols.resolveMember(file.source(), broken.references().getFirst()));
        assertTrue(
                SourceReferences.of(file).stream()
                        .filter(reference -> broken.owner().equals(reference.owner()))
                        .noneMatch(SourceReference::reliable));
    }

    @Test
    void unknownArgumentAndMissingTypeAreNeverGuessed() {
        FileIndex file =
                file(
                        "Unknown.java",
                        "class Unknown { void run(int value) {} void run(String value) {} Missing"
                                + " absent; void use() { run(missing); } }");
        CodeIndex symbols = new CodeIndex();
        symbols.put(file);
        var call =
                file.bodies().stream()
                        .flatMap(body -> body.references().stream())
                        .filter(reference -> reference.name().equals("run"))
                        .findFirst()
                        .orElseThrow();
        assertFalse(
                symbols.resolveMember(file.source(), call) instanceof MemberResolution.Resolved);
        assertInstanceOf(
                Resolution.Unresolved.class,
                new TypeResolver(symbols)
                        .resolve(
                                file,
                                file.references().stream()
                                        .filter(reference -> reference.name().equals("Missing"))
                                        .findFirst()
                                        .orElseThrow()));
    }

    @Test
    void staticImportsAndLexicalShadowingUseTheirActualOwners() {
        CodeIndex symbols = new CodeIndex();
        symbols.put(
                file(
                        "Utility.java",
                        "package shared; public class Utility { public static void work(int value)"
                                + " {} }"));
        FileIndex caller =
                file(
                        "Caller.java",
                        "package client; import static shared.Utility.work; class First { void"
                            + " run(int value) {} } class Second { void run(int value) {} } class"
                            + " Caller { void use(First target) { work(1); { Second target = new"
                            + " Second(); target.run(2); } target.run(3); } }");
        symbols.put(caller);
        var targets =
                caller.bodies().stream()
                        .flatMap(body -> body.references().stream())
                        .filter(reference -> reference.role() == BodyReference.Role.CALLS)
                        .map(
                                reference ->
                                        ((MemberResolution.Resolved)
                                                        symbols.resolveMember(
                                                                caller.source(), reference))
                                                .target()
                                                .symbolId()
                                                .value())
                        .toList();
        assertEquals(
                List.of(
                        "shared.Utility#work(int)",
                        "client.Second#run(int)",
                        "client.First#run(int)"),
                targets);
    }

    @Test
    void typeVariablesRemainLocalAndMethodReferencesRemainDistinctFromCalls() {
        FileIndex file =
                file(
                        "Generic.java",
                        "class Generic<T> { T value; void run(int value) {} void run(String value)"
                                + " {} Object reference() { return this::run; } }");
        CodeIndex symbols = new CodeIndex();
        symbols.put(file);
        var variable =
                file.references().stream()
                        .filter(reference -> reference.name().equals("T"))
                        .findFirst()
                        .orElseThrow();
        assertInstanceOf(
                Resolution.TypeVariable.class, new TypeResolver(symbols).resolve(file, variable));
        var reference =
                file.bodies().stream()
                        .flatMap(body -> body.references().stream())
                        .filter(item -> item.role() == BodyReference.Role.METHOD_REFERENCE)
                        .findFirst()
                        .orElseThrow();
        assertInstanceOf(
                MemberResolution.Ambiguous.class, symbols.resolveMember(file.source(), reference));
        assertTrue(
                SourceReferences.of(file).stream()
                        .anyMatch(item -> item.role() == SourceReference.Role.METHOD_REFERENCE));
    }

    @Test
    void binaryDeclarationsAndExternalSymbolsUseTheSameLookupContract() throws Exception {
        FileIndex external;
        try (var bytes = java.util.List.class.getResourceAsStream("List.class")) {
            external =
                    new org.jmouse.java.index.binary.ClassFileIndexer()
                            .index("jdk/java/util/List.class", bytes.readAllBytes())
                            .file();
        }
        CodeIndex symbols = new CodeIndex();
        symbols.externalSymbols(
                new ExternalSymbols(
                        name ->
                                name.equals("java.util.List")
                                        ? java.util.Optional.of(external)
                                        : java.util.Optional.empty()));
        FileIndex consumer =
                file("Consumer.java", "import java.util.List; class Consumer { List values; }");
        assertInstanceOf(
                Resolution.Resolved.class,
                new TypeResolver(symbols).resolve(consumer, consumer.references().getFirst()));
        var list = symbols.findTypes("java.util.List").getFirst();
        assertTrue(
                symbols.findMembers(list).stream()
                        .anyMatch(member -> member.name().equals("size")));
    }
}
