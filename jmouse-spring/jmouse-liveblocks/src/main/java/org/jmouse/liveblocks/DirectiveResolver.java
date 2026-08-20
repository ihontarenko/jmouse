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

}
