package org.jmouse.access.spring.jpa;

import org.jmouse.access.jpa.StoredConditions;
import org.jmouse.access.policy.ConditionCompiler;
import org.jmouse.access.spi.GrantCondition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A stored condition, compiled once per distinct expression rather than once per row.
 *
 * <h2>⚠️ The cache is the whole point, not an optimisation</h2>
 *
 * <p>A condition in a document is compiled at load, once, because a document loads once. A condition
 * in a <em>column</em> is read on every resolution that touches the row — and one expression is
 * typically on every entry of a bundle, on every assignment of that role, for every account holding
 * it. Compiling per read would put a parser inside the authorization path, where its cost is paid by
 * every request and its failure mode is a 500 on a permission check.
 *
 * <p><strong>Keyed by source text</strong>, which is the whole identity of a condition: two rows
 * spelling the same predicate are the same predicate, and one spelling it differently is a different
 * one even where it means the same thing. Nothing here needs to know which row it came from.
 *
 * <h2>⚠️ Unbounded, deliberately</h2>
 *
 * <p>The keys are the distinct condition expressions an installation has written, which is a number
 * bounded by how much policy somebody typed — tens, not millions. An eviction policy here would add a
 * failure mode (recompiling under load) to defend against a growth that cannot happen, and a
 * condition that <em>could</em> grow unboundedly would mean something had started generating policy,
 * which is a problem an LRU would hide rather than solve.
 */
public class CompiledStoredConditions implements StoredConditions {

    private final ConditionCompiler            compiler;
    private final Map<String, GrantCondition> compiled = new ConcurrentHashMap<>();

    public CompiledStoredConditions(ConditionCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public GrantCondition of(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        return compiled.computeIfAbsent(source, compiler::compile);
    }
}
