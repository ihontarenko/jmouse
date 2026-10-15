package org.jmouse.parallax.script;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What a document describes — the word it opens with — and which blocks that kind of document may hold.
 *
 * <p>A kind is the <em>structure</em> of a definition — what is mapped to what — never its subject: a fractal is an
 * {@code iteration} whose output folds the orbit of every point into a picture, a sequence is a {@code curve} over an
 * integer input, a distribution is a curve whose output is a {@code density}, a random walk is an {@code iteration} whose
 * steps draw. The subject is {@code meta { category }}; the engine that computes a document is derived from its
 * structure where it is read.</p>
 *
 * <p>A block a kind does not have is refused with its line, never ignored: a {@code pins} block on a surface is a
 * mistake the author must hear about, not a silent no-op.</p>
 */
public enum Kind {

    CURVE("curve", Role.COMPUTABLE, Blocks.MATHEMATICS),
    SURFACE("surface", Role.COMPUTABLE, Blocks.MATHEMATICS),
    HYPERSURFACE("hypersurface", Role.COMPUTABLE, Blocks.MATHEMATICS),
    FIELD("field", Role.COMPUTABLE, Blocks.MATHEMATICS),
    DYNAMICS("dynamics", Role.COMPUTABLE, Blocks.with(Blocks.EVOLVING, "image")),
    ITERATION("iteration", Role.COMPUTABLE, Blocks.EVOLVING),
    MAPPING("mapping", Role.COMPUTABLE, Blocks.MATHEMATICS),
    REGION("region", Role.COMPUTABLE, Blocks.MATHEMATICS),
    ITERATED_FUNCTION_SYSTEM("iterated_function_system", Role.COMPUTABLE, Blocks.with(Blocks.MATHEMATICS, "maps")),
    LINDENMAYER_SYSTEM("lindenmayer_system", Role.COMPUTABLE, Blocks.with(Blocks.MATHEMATICS, "axiom", "rules", "turtle")),
    MATRIX("matrix", Role.COMPUTABLE, Blocks.MATHEMATICS),
    POINTS("points", Role.COMPUTABLE, Blocks.with(Blocks.MATHEMATICS, "points", "run")),
    CONSTRUCTION("construction", Role.COMPUTABLE, Blocks.with(Blocks.MATHEMATICS, "steps")),
    POLYTOPE("polytope", Role.COMPUTABLE, Blocks.with(Blocks.MATHEMATICS, "vertices", "edges", "run")),
    VALUE("value", Role.COMPUTABLE, Blocks.MATHEMATICS),
    PROGRAM("program", Role.COMPUTABLE, Blocks.with(Blocks.MATHEMATICS, "state", "run")),
    COMPONENT("component", Role.COMPUTABLE, Blocks.COMPONENT),
    CIRCUIT("circuit", Role.COMPUTABLE, Blocks.CIRCUIT),
    SYMBOL("symbol", Role.DRAWING, Set.of("meta")),
    FAMILY("family", Role.SHARED, Set.of("meta", "parameters", "formulas")),
    FUNCTIONS("functions", Role.SHARED, Set.of("meta", "formulas")),
    CATEGORY("category", Role.CATALOGUE, Set.of("meta")),
    COLLECTION("collection", Role.CATALOGUE, Set.of("meta", "items", "section")),
    COURSE("course", Role.CATALOGUE, Set.of("meta", "lesson", "step")),
    INSTRUMENT("instrument", Role.CATALOGUE, Set.of("meta", "objects", "representations", "opens")),
    GRAPH("graph", Role.COMPOSITION, Set.of("meta", "parameters", "formulas", "clock", "panes", "lines", "expect"));

    /** What a document of a kind is for. */
    public enum Role {
        /** An object that computes: it has inputs, parameters and outputs. */
        COMPUTABLE,
        /** A drawing other documents use. */
        DRAWING,
        /** Values or definitions other documents share: a logic family, a library of jMF functions. */
        SHARED,
        /** Lines drawn together on panes and moved by one clock: a graph. It computes nothing itself; its lines do. */
        COMPOSITION,
        /** Organises the catalogue: sections, collections, courses and the instruments objects open in. */
        CATALOGUE
    }

    private final String      keyword;
    private final Role        role;
    private final Set<String> blocks;

    Kind(String keyword, Role role, Set<String> blocks) {
        this.keyword = keyword;
        this.role = role;
        this.blocks = blocks;
    }

    public String keyword() {
        return keyword;
    }

    public Role role() {
        return role;
    }

    /** The block names a document of this kind may hold, {@code meta} included. */
    public Set<String> blocks() {
        return blocks;
    }

    public boolean allows(String block) {
        return blocks.contains(block);
    }

    /** Blocks a document may hold more than once: every other block is written at most once. */
    public static final Set<String> REPEATABLE_BLOCKS = Set.of("expect", "analysis", "step", "lesson", "section");

    /**
     * Words at the top level of a body that are declarations, not blocks. {@code extends} is a component's base — the
     * physics it specializes; {@code use} pulls a shared document in, what it is decided by that document's kind (a
     * logic family, a jMF library, a dependency); {@code units} names a multi-unit package's units.
     */
    public static final Set<String> DECLARATIONS = Set.of("extends", "units", "use");

    /** The lines a {@code symbol} document is drawn with, written at the top level of its body. */
    public static final Set<String> DRAWING_LINES = Set.of("size", "path", "pin", "label", "indicator");

    public static Optional<Kind> ofKeyword(String word) {
        return Arrays.stream(values()).filter(kind -> kind.keyword.equals(word)).findFirst();
    }

    private static final class Blocks {

        static final Set<String> MATHEMATICS = Set.of("meta", "parameters", "inputs", "outputs", "formulas", "view", "expect");

        static final Set<String> EVOLVING = with(MATHEMATICS, "state", "equations");

        static final Set<String> COMPONENT = Set.of("meta", "parameters", "formulas", "pins", "nodes", "state",
                "equations", "behaviour", "symbol", "parts", "nets", "view", "expect", "display", "measure", "leads");

        static final Set<String> CIRCUIT = Set.of("meta", "parameters", "formulas", "sheet", "parts", "nets", "layout",
                "ports", "probes", "analysis", "symbol", "view", "expect");

        static Set<String> with(Set<String> base, String... more) {
            return Stream.concat(base.stream(), Stream.of(more)).collect(Collectors.toUnmodifiableSet());
        }

        private Blocks() {
        }
    }
}
