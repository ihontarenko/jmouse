package org.jmouse.access.policy.model;

import org.jmouse.access.spi.PermissionRelations.Quantifier;

/**
 * A permission's {@code through} clause, as the file wrote it.
 *
 * <pre>
 * field:write "…" through each form   →  new PolicyPermissionRedirect("form", Quantifier.EACH)
 * field:write "…" through any form    →  new PolicyPermissionRedirect("form", Quantifier.ANY)
 * </pre>
 *
 * <p>⚠️ <strong>The resource is kept as the WORD, not resolved to a type here.</strong> Stage 1 parses
 * text and knows no classes; the word is checked against the resource vocabulary in stage 2, where the
 * registry exists — and that check is what turns {@code through frm} into a startup failure listing the
 * names that would have worked, rather than a rule that silently refuses everything. This record is
 * therefore the string-shaped half of {@link org.jmouse.access.spi.PermissionRelations.Redirect}, which
 * is the same clause once a type is known.
 *
 * <p>The quantifier is compulsory and shares its enum with the engine — see {@link Quantifier} for why
 * {@code through form} deliberately does not parse.
 *
 * @param resource   the resource name written after the quantifier — the word an
 *                   {@code @AccessResourceName} declares, never a permission namespace
 * @param quantifier how many of the related rows have to allow it
 */
public record PolicyPermissionRedirect(String resource, Quantifier quantifier) {

    /** How the clause is written back out — the exact text a round trip has to reproduce. */
    public String toSource() {
        return "through " + quantifier.word() + " " + resource;
    }
}
