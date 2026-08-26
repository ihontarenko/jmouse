package org.jmouse.liveblocks;

/**
 * How this product answers one kind of directive.
 *
 * <p>The seam that keeps the resolution engine from knowing what an issue, a sprint or a part is. The
 * engine declares this interface and dispatches to whatever is registered; each implementation lives
 * beside the concept that owns its meaning and is the only side holding a repository.
 *
 * <h2>⚠️ It narrows to the reader, and the library cannot do that for it</h2>
 *
 * <p>Nothing in this module knows who is calling — there is no principal in the signature on purpose. A
 * product reads its own subject the way it already does everywhere else, and <strong>each resolver is
 * responsible for answering only what that person may see</strong>: a thing they hold nothing at comes
 * back {@link DirectiveStatus#NOT_FOUND} or {@link DirectiveStatus#NO_ACCESS}, never resolved.
 *
 * <p>Putting a principal type in this signature is how a seam stops being one — it would be either a
 * product's own class, which no other product can implement against, or a lowest-common-denominator
 * identifier that every resolver then has to re-resolve.
 *
 * <h2>⚠️ A miss is a return value, never an exception</h2>
 *
 * <p>The engine catches whatever escapes and turns it into {@link DirectiveStatus#NOT_FOUND} so one bad
 * row cannot cost a page — but that is a backstop, not the contract. A resolver that cannot answer says
 * so, because only it knows <em>which</em> miss it is.
 *
 * <h2>⚠️ Registered as a bean, and none is a working installation</h2>
 *
 * <p>A product with no resolvers answers {@link DirectiveStatus#UNKNOWN_DIRECTIVE} to everything, which
 * renders as a notice on every block. That is a correct answer from a product nobody quotes yet, not a
 * broken one.
 */
public interface DirectiveResolver {

    /** The directive name this handles — {@code "issue"}, {@code "sprint"}. Lowercase, and unique. */
    String directive();

    /**
     * Answer one directive for whoever is calling.
     *
     * <p>The argument arrives exactly as the document wrote it, trimmed of nothing: parsing it is the
     * resolver's job, because only it knows whether {@code TSSR-4} is a key, a slug or a number.
     */
    ResolvedDirective resolve(Directive directive);

    /**
     * What a document could refer to — the picker's question, asked before anything has been named.
     *
     * <h2>⚠️ Answering nothing is a complete implementation</h2>
     *
     * <p>The default is an empty list, so every resolver written before this existed goes on compiling
     * and a product that has nothing worth browsing simply offers no picker. That is a correct answer,
     * not a gap: {@code :::stock R-0402-10K} is written from a part number somebody already has in
     * front of them, and a list of every component in a warehouse would help nobody.
     *
     * <h2>⚠️ It is a search, so it narrows to the reader like everything else here</h2>
     *
     * <p>And more sharply than {@link #resolve} does. A resolver leaking one thing needs its identifier
     * guessed first; a <em>suggester</em> that leaks needs nothing at all — it hands over the list. So
     * the same rule with none of the slack: what this returns is what that person could already find in
     * this product's own interface.
     *
     * @param query what has been typed so far. ⚠️ Blank is legitimate and means "the first few" — the
     *              picker opens before anybody types, and answering nothing there reads as broken
     * @param limit the most to return; a suggester may return fewer and never more
     */
    default java.util.List<DirectiveSuggestion> suggest(String query, int limit) {
        return java.util.List.of();
    }

}
