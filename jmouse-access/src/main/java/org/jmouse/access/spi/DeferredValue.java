package org.jmouse.access.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A published value that has not been worked out yet, and will not be unless a rule asks for it.
 *
 * <h2>⚠️ Why laziness is a marker in the bag rather than a type on every signature</h2>
 *
 * <p>A value can be expensive: what a workspace <em>counts</em> is a row, and a naive reading puts a
 * query on the security path — a page of twenty-five rows paying twenty-five times for one fact that
 * cannot change while the request runs. The obvious fix is to publish suppliers instead of values,
 * and the obvious fix is wrong: {@code Map<String, Object>} would become
 * {@code Map<String, Supplier<Object>>} through the publication, the context scope, the condition
 * context and everything that reads one, so a mechanical concern would have rewritten four
 * signatures that are about something else.
 *
 * <p>So the bag stays {@code Map<String, Object>} and <em>this</em> is one of the objects in it.
 * Everything between the publisher and the reader copies it as an ordinary reference and costs
 * nothing. Only the reader unwraps — {@link #resolve(Object)} — and the only reader that has to know
 * is the one binding names into an expression, which already knows <em>which</em> names the rule
 * mentions and can therefore skip the rest entirely.
 *
 * <p>⚠️ <strong>Resolved at most once, and never twice.</strong> Two rules reading the same name in
 * one decision must not produce two queries, and — more sharply — must not be able to disagree.
 *
 * <p>⚠️ <strong>A supplier that throws yields null rather than propagating.</strong> This runs while
 * a decision is being made, and what one of these reaches for is by nature outside the invocation.
 * Letting it out turns a stale header into a 500 on a route that would otherwise have worked — an
 * outage wearing an authorization failure's clothes. Absent is the honest answer, and it is the same
 * trade every other seam on this path makes.
 */
public final class DeferredValue {

    private static final Object UNRESOLVED = new Object();

    private final Supplier<Object> supplier;
    private       Object           resolved = UNRESOLVED;

    private DeferredValue(Supplier<Object> supplier) {
        this.supplier = supplier;
    }

    /** A value to be worked out on first read, if anything reads it. */
    public static DeferredValue of(Supplier<Object> supplier) {
        return new DeferredValue(supplier);
    }

    /**
     * The value behind one entry of a published bag: itself where it is ordinary, the worked-out
     * answer where it was deferred.
     *
     * <p>The one place anything needs to know this type exists.
     */
    public static Object resolve(Object published) {
        return published instanceof DeferredValue deferred ? deferred.get() : published;
    }

    /** Every entry of a bag, resolved — for a reader with no way to know which names it needs. */
    public static Map<String, Object> resolveAll(Map<String, Object> published) {
        if (published.values().stream().noneMatch(DeferredValue.class::isInstance)) {
            return published;
        }

        Map<String, Object> values = new LinkedHashMap<>(published.size());

        published.forEach((name, value) -> values.put(name, resolve(value)));

        return values;
    }

    private synchronized Object get() {
        if (resolved == UNRESOLVED) {
            resolved = read();
        }

        return resolved;
    }

    private Object read() {
        try {
            return supplier.get();
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    @Override
    public String toString() {
        return resolved == UNRESOLVED ? "<deferred>" : String.valueOf(resolved);
    }
}
