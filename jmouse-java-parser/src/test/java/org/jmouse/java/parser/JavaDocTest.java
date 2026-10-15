package org.jmouse.java.parser;

import java.util.List;
import org.jmouse.java.parser.model.BlockTag;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.InlineTag;
import org.jmouse.java.parser.model.JavaDoc;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.MethodDeclaration;
import org.jmouse.java.parser.model.ReferenceState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaDocTest {

    private static final String SOURCE = """
            package org.example;

            /**
             * Converts values. See {@link java.util.Map#get(Object) the map lookup} and
             * {@linkplain Converter plain}; uses {@code Map<K, V> map = {}} and {@literal <b>}.
             *
             * @param <T> the value type
             * @author Ivan
             * @since 1.0
             * @see java.util.Objects#equals(Object, Object)
             * @see "The book"
             * @apiNote kept generically
             */
            public interface Converter<T> {

                /**
                 * {@inheritDoc}
                 * <p>Uses {@value #LIMIT} at most.
                 *
                 * @param input  the text,
                 *               on two lines
                 * @param radix  the base
                 * @return the number
                 * @throws NumberFormatException when it is not one
                 * @exception IllegalStateException when closed
                 * @deprecated use {@link #parse(String)} instead
                 */
                @Deprecated
                int convert(String input, int radix) throws NumberFormatException;

                int LIMIT = 10;
            }

            /** An orphan at the end. */
            """;

    private final JavaSourceFile file = new JavaParser().parse("Converter.java", SOURCE);

    @Test
    void typeCommentIsStructured() {
        assertTrue(file.isComplete(), () -> file.problems().toString());
        JavaDoc javaDoc = file.types().getFirst().javaDoc();

        assertEquals("""
                Converts values. See {@link java.util.Map#get(Object) the map lookup} and
                {@linkplain Converter plain}; uses {@code Map<K, V> map = {}} and {@literal <b>}.""", javaDoc.description());
        assertEquals("the value type", javaDoc.parameter("<T>").orElseThrow().text());
        assertEquals("Ivan", javaDoc.blockTags(BlockTag.Kind.AUTHOR).getFirst().text());
        assertEquals("1.0", javaDoc.blockTags(BlockTag.Kind.SINCE).getFirst().text());

        List<BlockTag> sees = javaDoc.blockTags(BlockTag.Kind.SEE);
        assertEquals("java.util.Objects#equals(Object, Object)", sees.get(0).argument());
        assertEquals(ReferenceState.UNRESOLVED, sees.get(0).referenceState());
        assertNull(sees.get(1).argument());
        assertEquals("\"The book\"", sees.get(1).text());
        assertEquals(ReferenceState.NOT_A_REFERENCE, sees.get(1).referenceState());

        BlockTag other = javaDoc.blockTags(BlockTag.Kind.OTHER).getFirst();
        assertEquals("apiNote", other.name());
        assertEquals("kept generically", other.text());

        List<InlineTag> inline = javaDoc.inlineTags();
        assertEquals(List.of(InlineTag.Kind.LINK, InlineTag.Kind.LINKPLAIN, InlineTag.Kind.CODE, InlineTag.Kind.LITERAL),
                inline.stream().map(InlineTag::kind).toList());
        assertEquals("java.util.Map#get(Object)", inline.get(0).reference());
        assertEquals("the map lookup", inline.get(0).label());
        assertEquals(ReferenceState.UNRESOLVED, inline.get(0).referenceState());
        assertEquals("Map<K, V> map = {}", inline.get(2).content());
        assertEquals(ReferenceState.NOT_A_REFERENCE, inline.get(2).referenceState());
        assertEquals("{@code Map<K, V> map = {}}", inline.get(2).span().slice(SOURCE));
    }

    @Test
    void methodCommentKeepsItsOwnSpanAndAnExplicitOwner() {
        MethodDeclaration convert = (MethodDeclaration) file.types().getFirst().members().getFirst();
        JavaDoc           javaDoc = convert.javaDoc();

        assertEquals("org.example.Converter#convert(String,int)", javaDoc.owner().symbolId().value());
        assertEquals(DeclarationKind.METHOD, javaDoc.owner().kind());
        assertEquals("org.example.Converter.convert", javaDoc.owner().qualifiedName());
        assertEquals("@Deprecated int convert(String input, int radix) throws NumberFormatException", javaDoc.owner().signature());
        assertEquals(convert.span(), javaDoc.owner().span());
        assertTrue(javaDoc.span().end().offset() <= convert.span().start().offset());
        assertEquals(16, javaDoc.span().startLine());
        assertEquals(27, javaDoc.span().endLine());
        assertEquals(28, convert.span().startLine());
        assertTrue(javaDoc.rawText().startsWith("/**") && javaDoc.rawText().endsWith("*/"));
        assertEquals(javaDoc.span().start().offset(), convert.documentedStartOffset());
    }

    @Test
    void inheritedDocumentationAndReferencesAreMarkedNeverInvented() {
        JavaDoc javaDoc = ((MethodDeclaration) file.types().getFirst().members().getFirst()).javaDoc();

        assertTrue(javaDoc.inheritsDocumentation());
        assertEquals("{@inheritDoc}\n<p>Uses {@value #LIMIT} at most.", javaDoc.description());
        InlineTag inherit = javaDoc.inlineTags().getFirst();
        assertEquals(InlineTag.Kind.INHERIT_DOC, inherit.kind());
        assertEquals(ReferenceState.UNRESOLVED, inherit.referenceState());
        InlineTag value = javaDoc.inlineTags().get(1);
        assertEquals(InlineTag.Kind.VALUE, value.kind());
        assertEquals("#LIMIT", value.reference());
    }

    @Test
    void blockTagsCarryTheirArguments() {
        JavaDoc javaDoc = ((MethodDeclaration) file.types().getFirst().members().getFirst()).javaDoc();

        assertEquals("the text,\n              on two lines", javaDoc.parameter("input").orElseThrow().text());
        assertEquals("the base", javaDoc.parameter("radix").orElseThrow().text());
        assertEquals("the number", javaDoc.blockTags(BlockTag.Kind.RETURN).getFirst().text());

        List<BlockTag> thrown = javaDoc.blockTags(BlockTag.Kind.THROWS);
        assertEquals(List.of("NumberFormatException", "IllegalStateException"), thrown.stream().map(BlockTag::argument).toList());
        assertEquals(List.of("throws", "exception"), thrown.stream().map(BlockTag::name).toList());
        assertEquals(ReferenceState.UNRESOLVED, thrown.getFirst().referenceState());

        BlockTag deprecated = javaDoc.blockTags(BlockTag.Kind.DEPRECATED).getFirst();
        assertEquals("use {@link #parse(String)} instead", deprecated.text());
        assertEquals("#parse(String)", deprecated.inlineTags().getFirst().reference());
        assertEquals("@deprecated use {@link #parse(String)} instead", deprecated.span().slice(SOURCE));
    }

    @Test
    void everyCommentIsListedWithItsOwnerOrAsAnOrphan() {
        assertEquals(3, file.javaDocs().size());
        assertEquals("org.example.Converter", file.javaDocs().get(0).owner().symbolId().value());
        assertNull(file.javaDocs().get(2).owner());
        assertEquals("An orphan at the end.", file.javaDocs().get(2).description());
    }
}
