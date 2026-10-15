package org.jmouse.parallax.script.form;

import org.jmouse.parallax.script.Kind;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The language as a form, server validation and code completion see it: every kind, the blocks each may hold, and
 * the fields of each block's lines.
 *
 * <p>It is read off the reader's own definitions — {@link Kind} for kinds, their roles and blocks, the repeatable
 * blocks and the declarations; the codecs for the fields, whose allowed words are the reader's enumerations. Nothing
 * of the grammar is restated where it is served, and a block the reader learns appears here without an edit.</p>
 *
 * @param kinds        every kind of document
 * @param blocks       every block, once, with the kinds that may hold it
 * @param declarations the top-level statements every kind may write: {@code uses}, {@code family}, {@code units}
 * @param fieldTypes   every field type, each of which has its widget
 */
public record LanguageSchema(List<KindSchema> kinds, List<BlockSchema> blocks, List<FieldSchema> declarations,
                             List<FieldType> fieldTypes) {

    /**
     * @param keyword the word a document of the kind opens with
     * @param role    what a document of the kind is for
     * @param blocks  the blocks it may hold, in alphabetical order
     * @param lines   the statements of one line it is drawn or described with at the top level ({@code size},
     *                {@code path}, {@code pin}, {@code label} for a symbol)
     */
    public record KindSchema(String keyword, Kind.Role role, List<String> blocks, List<String> lines) {
    }

    /** Body blocks whose braces hold another language. */
    static final Set<String> BODIES = Set.of("formulas", "behaviour", "run");

    /** Blocks the schematic canvas edits. */
    static final Set<String> SCHEMATIC = Set.of("parts", "nets");

    public static LanguageSchema describe() {
        List<KindSchema> kinds = Arrays.stream(Kind.values()).map(kind -> new KindSchema(kind.keyword(), kind.role(),
                List.copyOf(new TreeSet<>(kind.blocks())),
                kind == Kind.SYMBOL ? List.copyOf(new TreeSet<>(Kind.DRAWING_LINES)) : List.of())).toList();
        Set<String> names = new TreeSet<>();
        Arrays.stream(Kind.values()).forEach(kind -> names.addAll(kind.blocks()));
        List<BlockSchema> blocks = names.stream().map(LanguageSchema::block).toList();
        return new LanguageSchema(kinds, blocks, StatementCodecs.DECLARATIONS, List.of(FieldType.values()));
    }

    static BlockSchema block(String name) {
        List<String> kinds = Arrays.stream(Kind.values()).filter(kind -> kind.allows(name)).map(Kind::keyword).toList();
        List<FieldSchema> header = name.equals("symbol") ? List.of(StatementCodecs.SYMBOL_USES) : List.of();
        return new BlockSchema(name, editorOf(name), Kind.REPEATABLE_BLOCKS.contains(name), header,
                LineCodecs.of(name).fields(), kinds);
    }

    static BlockSchema.Editor editorOf(String block) {
        if (BODIES.contains(block)) {
            return BlockSchema.Editor.BODY;
        }
        if (SCHEMATIC.contains(block)) {
            return BlockSchema.Editor.SCHEMATIC;
        }
        return BlockSchema.Editor.LINES;
    }
}
