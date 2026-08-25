package org.jmouse.query.spring.source;

import java.util.Set;

/**
 * A declaration named a table this installation does not publish.
 *
 * <h2>⚠️ The message lists what IS allowed, on purpose</h2>
 *
 * <p>This refusal is read by somebody editing a mapping, and the useful half is not *no* — it is *these
 * are the tables you have*. A refusal that only says no sends a person to ask whoever configured the
 * allow-list, and the answer they get back is this same list.</p>
 *
 * <p>⚠️ Disclosing the allow-list is not a leak: it is a list of what the product deliberately
 * published, and this refusal only reaches somebody who already holds the permission to write
 * declarations. Withholding it would protect nothing and cost every edit a conversation.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class UnpublishedTableException extends RuntimeException {

    private final Set<String> refused;

    public UnpublishedTableException(Set<String> refused, Set<String> allowed) {
        super(allowed.isEmpty()
                      ? ("this installation publishes no tables to authored declarations, so %s cannot be "
                         + "reached; set `jmouse.query.sources.published-tables` to change that")
                              .formatted(String.join(", ", refused))
                      : "%s %s not published; this installation publishes %s".formatted(
                              String.join(", ", refused),
                              refused.size() == 1 ? "is" : "are",
                              String.join(", ", allowed)));

        this.refused = refused;
    }

    /** The tables that were refused, for a caller that wants to mark them rather than print a sentence. */
    public Set<String> refused() {
        return refused;
    }
}
