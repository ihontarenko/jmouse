package org.jmouse.query.sql;

import java.util.Optional;

/**
 * Resolves the name a document writes after {@code on} into the table it means.
 *
 * <p>A product implements it, because a product is the only thing that knows whether {@code inventory}
 * is a table, a section, a purpose, or all three. ⚠️ That question is still open in the design, and this
 * interface is deliberately shaped so it does not have to be answered here: whatever {@code inventory}
 * turns out to address, resolving it is one method.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface QueryTargets {

    /**
     * The target this name refers to.
     *
     * @param name the name written after {@code on}
     * @return the target, or empty when nothing here is called that
     */
    Optional<QueryTarget> target(String name);

    /**
     * A resolver that knows exactly one target.
     *
     * <p>⚠️ Honest rather than convenient: it refuses <em>every other name</em> by returning empty, so a
     * document naming something else is refused rather than quietly run against the only table there is.
     * A single-target installation is common; a single-target resolver that ignores the name is a trap.</p>
     *
     * @param target the one target
     * @return a resolver answering for that name and no other
     */
    static QueryTargets only(QueryTarget target) {
        return name -> target.name().equals(name) ? Optional.of(target) : Optional.empty();
    }
}
