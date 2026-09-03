package org.jmouse.query.el.lexer;

import org.jmouse.el.lexer.Token;

/**
 * The keywords a {@code .jmq} document adds on top of the expression language.
 *
 * <p>Everything a query is actually made of — identifiers, strings, brackets, braces, operators, the
 * converter pipes — is already a {@link org.jmouse.el.lexer.BasicToken}. Only the words below belong
 * to this language, which is a fair measure of how small a dialect it is.</p>
 *
 * <p>Ids are grouped so a reader can tell what a word is for at a glance: {@code 10xxx} opens a
 * declaration, {@code 11xxx} is a clause inside one, {@code 12xxx} modifies a clause, and
 * {@code 19xxx} is <strong>reserved</strong>. Every id is distinct — they identify a token type, and
 * two words sharing one would be two names for a single thing.</p>
 *
 * <h2>⚠️ {@code function} is deliberately absent from this enum</h2>
 *
 * <p>It is already {@link org.jmouse.el.language.lexer.LanguageToken#T_FUNCTION}, in the shared
 * statement layer both this dialect and {@code .jmp} draw on. Declaring a second token for the same
 * word would not produce an error: the recognizer is asked most-general-first, the shared token would
 * always answer, and this enum's constant would simply never be produced — a keyword that silently
 * never matches. So the shared one is used, and this note is here because its absence otherwise looks
 * like an oversight.</p>
 *
 * <h2>⚠️ Reserved words are recognised, then refused</h2>
 *
 * <p>A word this language may want later is lexed <em>now</em> and rejected by the parser with a
 * message saying so. The alternative — leaving it to lex as an identifier — is what makes a keyword
 * impossible to introduce afterwards: by then documents exist in which the word is somebody's column
 * name, and adding the keyword silently changes what they mean.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum QueryToken implements Token.Type {

    /** {@code view "name" on target { … }} — a named, stored query. */
    T_VIEW(10000, "view"),

    /**
     * {@code view "…" on inventory} — what the query runs against.
     *
     * <p>⚠️ Read as an opaque identifier and resolved by nobody here. Whether a target names a section,
     * a purpose, or both is a product's decision and deliberately not one this grammar takes.</p>
     */
    T_ON(10200, "on"),

    /** {@code where <expression>} — which rows. */
    T_WHERE(11000, "where"),

    /** {@code order <expression> [asc|desc] {, …}} — in what sequence. */
    T_ORDER(11100, "order"),

    /** {@code columns <expression> [as name] {, …}} — what to return. */
    T_COLUMNS(11200, "columns"),

    /** {@code … asc} — the default direction, spelled out. */
    T_ASC(12000, "asc"),

    /** {@code … desc} */
    T_DESC(12100, "desc"),

    /**
     * {@code userIds as int[]}, {@code sum(x) as total} — a type annotation, and a projection alias.
     *
     * <p>⚠️ <strong>A type is written with {@code as} because {@code :} was already taken.</strong>
     * Core jME's {@code ParametersParser} reads {@code name : expression} as a <em>default value</em>,
     * and jMT's {@code macro} relies on that. Giving this dialect its own meaning for the same
     * punctuation would be invisible until somebody copied a line between the two.</p>
     */
    T_AS(12200, "as"),

    // ── Declaring where the data is ───────────────────────────────────────────────────────────────
    //
    // ⚠️ A source is declared in THIS language rather than in a companion file, because a view is
    // cross-product and a mapping is not: `view … on inventory` must run in any product that declares
    // an `inventory` source. Keeping both in one grammar means one parser, one highlighter and one
    // un-parse — the same reason `.jmp` declares `scopes { }` instead of carrying a sidecar.

    /** {@code source inventory { … }} — where the rows are and what may be asked of them. */
    T_SOURCE(10300, "source"),

    /** {@code from form_entries as e key id} — the table, its alias, and what a join points at. */
    T_FROM_TABLE(11300, "from"),

    /** {@code bag field_entries on entry_id key field value text_value} — a table of loose values. */
    T_BAG(11400, "bag"),

    /**
     * {@code join statuses on status_id key id} — a row in another table, reached by one hop.
     *
     * <p>⚠️ <strong>One hop, and deliberately not a general join.</strong> What a product needs is
     * {@code issue.status.category} — a value that lives one table away because it was normalised there.
     * Arbitrary joins would make a saved query able to reach anything the database holds, which is the
     * opposite of what a declared source is for: the schema is the confinement.</p>
     *
     * <p>⚠️ The alias is keyed on the joined TABLE, not on the attribute — unlike a bag. Two attributes
     * from {@code statuses} are two columns of one row, so they share one join; two attributes from a bag
     * are two different rows and must not.</p>
     */
    T_JOIN(11800, "join"),

    /**
     * {@code collection labels on issue_id value name} — many rows per row, asked about with a test.
     *
     * <p>⚠️ Why this is not a bag: a bag row says <em>which</em> attribute it is and holds one value per
     * attribute, so it maps to an expression. A collection has no key column and many rows per owner, so
     * it maps to no expression at all — only to {@code EXISTS}. Reaching it through a join would multiply
     * the rows of the result, which is the bug this separate declaration exists to make impossible.</p>
     */
    T_COLLECTION(11900, "collection"),

    /** {@code attribute entry[name] from "f-name" text in bag} — one thing a query may write. */
    T_ATTRIBUTE(11500, "attribute"),

    /** {@code … key id} — which column another table joins against. */
    T_KEY(12300, "key"),

    /** {@code … value text_value} — which column holds a bag row's value. */
    T_VALUE(12400, "value"),

    /**
     * {@code priority: int, label: "How urgent"} — what a PERSON reads where a query writes a name.
     *
     * <h2>⚠️ Why the language carries this at all, rather than leaving it to whoever draws a builder</h2>
     *
     * <p>Because otherwise the attribute names are written twice: once here, and once in a map of
     * pretty words beside a screen — with nothing holding the two together. Rename an attribute in the
     * declaration and the map is keyed on a name that no longer exists; the lookup misses and the
     * builder falls back to showing {@code p.unsourced}. Nothing errors. The label simply stops
     * appearing, and only somebody who remembers it was ever there notices.</p>
     *
     * <h2>⚠️ It is a comma-clause, like {@code default:}, and that was not a free choice</h2>
     *
     * <p>A structure's field line spends its one colon on the type ({@code priority: int}), and the
     * language already had a way to say more about a field afterwards. Inventing a second shape —
     * {@code priority:"How urgent": int}, or a bare quoted string after the name — would mean two
     * grammars for trailing facts in the same block, and the next fact would have to pick one.</p>
     *
     * <p>⚠️ <strong>A label is never queryable.</strong> A query writes the name; this is what a person
     * reads. Accepting it as a second spelling would mean a saved query stops parsing the day somebody
     * improves the wording.</p>
     */
    T_LABEL(12800, "label"),

    /**
     * {@code bag field_entries on entry_id key field value text_value matching form_entry_id} — the
     * column on the FILTERED row that a bag hangs off, when it is not that row's own key.
     *
     * <h2>⚠️ The shape this exists for, and why nothing else expresses it</h2>
     *
     * <p>An ordinary bag row points straight at the row being filtered:
     * {@code bag.entry_id = root.id}. But a product whose subject area is <em>about</em> something that
     * has a bag has a second shape — an asset is a row of its own carrying a state and a due date, and
     * everything a person recognises it by lives on the entry it describes. There the correlation is
     * {@code field_entries.form_entry_id = assets.form_entry_id}: one hop sideways, not down.</p>
     *
     * <p>⚠️ Omitted, the bag hangs off the target's key, which is what every ordinary bag wants. So this
     * word changes nothing that already parses — and without it such a source parses, compiles,
     * correlates against the wrong column and matches <strong>nothing</strong>, with no error anywhere.</p>
     *
     * <h2>⚠️ Why {@code matching} rather than another {@code key} or {@code on}</h2>
     *
     * <p>The line already spends {@code on} on the bag's own column and {@code key} on the column that
     * says which attribute a row is. A third of either would read as a fourth name of the same kind. The
     * clause is a correlation — <em>this bag's foreign key matches that column</em> — and the word says
     * so, which is the only thing that stops a reader guessing which side it names.</p>
     */
    T_MATCHING(12900, "matching"),

    /**
     * {@code join <table> through <attribute> key <column>} — the foreign key is an attribute's value.
     *
     * <h2>⚠️ What it is for</h2>
     *
     * <p>{@code join} normally reads a column of the row's own table: {@code join statuses on status_id
     * key id}. A product that keeps its values in a bag keeps its foreign keys there too, and then there
     * is no such column — Innoventa's stock position holds the part it counts as a bag row, so the part's
     * table is unreachable from any column of {@code form_entries}.</p>
     *
     * <p>{@code through} names the ATTRIBUTE instead, and the attribute already knows what the store
     * calls it and how it is reached. So the same word serves a bag row, a plain column, or a value one
     * join further on, and none of the language's spelling rules leak into a mapping.</p>
     *
     * <h2>⚠️ Why {@code through} rather than widening {@code on}</h2>
     *
     * <p>{@code on} takes a COLUMN and has since the first source was written. Letting it take either
     * would make the two indistinguishable in a document — a reader could not tell {@code on part} from
     * {@code on part} — and a column and an attribute may perfectly well share a name. Two words, two
     * meanings, no guessing.</p>
     */
    T_THROUGH(13000, "through"),

    /** {@code … in bag} / {@code … in column} — how an attribute is reached. */
    T_COLUMN(12500, "column"),

    /** {@code group <expression> {, …}} — what the rows are gathered by. */
    T_GROUP(11600, "group"),

    /**
     * {@code having <expression>} — which GROUPS survive.
     *
     * <p>⚠️ Not a second {@code where}. {@code where} filters rows before they are gathered; this filters
     * the groups afterwards. An aggregate belongs here and is refused there, and the refusal says which.</p>
     */
    T_HAVING(11700, "having"),

    /** {@code limit <n>} — at most this many rows. */
    T_LIMIT(11950, "limit"),

    /**
     * {@code structure <name> { … }} — the SHAPE: attribute names, their types, their defaults.
     *
     * <p>⚠️ Portable, and knowing nothing about storage. That is the whole reason it is a declaration of
     * its own rather than half of a mapping: one structure may have several mappings, and a view names the
     * structure.</p>
     */
    T_STRUCTURE(10400, "structure"),

    /**
     * {@code mapping <structure>[:<variant>] { … }} — the BINDING: which table, column, file or cell.
     *
     * <p>⚠️ Never portable, and it never names a database vendor — quoting and paging come from the
     * dialect the connection reports.</p>
     */
    T_MAPPING(10500, "mapping"),

    /** {@code attributes { … }} — the sub-block a mapping puts its bindings in. */
    T_ATTRIBUTES(11510, "attributes"),

    /** {@code fetch …} — what the query brings back. */
    T_FETCH(11250, "fetch"),

    /** {@code uses(name as type)} — the ambient values a view is allowed to read. */
    T_USES(10250, "uses"),

    /** {@code default: <value>} — part of what a structure promises. */
    T_DEFAULT(12600, "default"),

    /** {@code attributes: identity} — every attribute reads the entry of its own name. */
    T_IDENTITY(12700, "identity"),

    /** {@code file: 'rows.csv'} — a mapping whose rows are in a file. */
    T_FILE(11520, "file"),

    // ── Reserved ──────────────────────────────────────────────────────────────────────────────────
    // Lexed so that they can be refused by name, never so that they can be used.

    /** Reserved — bounded results. */
    T_OFFSET(19030, "offset"),

    /** Reserved — a fuller projection surface. */
    T_SELECT(19100, "select"),

    /** Reserved — a fuller projection surface. */
    T_DISTINCT(19130, "distinct"),

    /** Reserved — set operations. */
    T_UNION(19200, "union"),

    /** Reserved — set operations. */
    T_INTERSECT(19210, "intersect"),

    /** Reserved — set operations. */
    T_EXCEPT(19220, "except"),

    /** Reserved — named sub-expressions. */
    T_WITH(19300, "with"),

    /** Reserved — history predicates, in the shape JQL has them. */
    T_WAS(19400, "was"),

    /** Reserved — history predicates. */
    T_CHANGED(19410, "changed"),

    /**
     * ⚠️ Reserved <strong>permanently</strong>. jMQ reads.
     *
     * <p>Reserving the three writing verbs costs nothing and buys something real: a reader meeting a
     * {@code .jmq} file never has to wonder whether some dialect of it might modify data, and a future
     * maintainer cannot introduce that by accident.</p>
     */
    T_INSERT(19900, "insert"),

    /** ⚠️ Reserved permanently — see {@link #T_INSERT}. */
    T_UPDATE(19910, "update"),

    /** ⚠️ Reserved permanently — see {@link #T_INSERT}. */
    T_DELETE(19920, "delete");

    /** Ids at or above this are recognised but not part of the language yet. */
    private static final int RESERVED_FROM = 19000;

    private final int      type;
    private final String[] values;

    QueryToken(final int type, final String... values) {
        this.type = type;
        this.values = values;
    }

    @Override
    public int getTypeId() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) getEnumType().getClass();
    }

    @Override
    public String[] getTokenTemplates() {
        return values;
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }

    /**
     * The word as it is written, for a message that has to name it.
     *
     * @return the keyword's spelling
     */
    public String spelling() {
        return values.length > 0 ? values[0] : name();
    }

    /**
     * Whether this word is recognised but not yet part of the language.
     *
     * <p>Derived from the id band rather than kept as a list elsewhere, so that adding a reserved word
     * is one line and forgetting to register it somewhere is not possible.</p>
     *
     * @return {@code true} when the parser must refuse it by name
     */
    public boolean isReserved() {
        return type >= RESERVED_FROM;
    }
}
