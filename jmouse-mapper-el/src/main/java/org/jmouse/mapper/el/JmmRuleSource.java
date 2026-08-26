package org.jmouse.mapper.el;

import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.binding.TypeMappingRule;
import org.jmouse.mapper.binding.TypeMappingRuleSource;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a mapper asks when it wants to know whether a {@code .jmm} file has anything to say about a
 * type pair. 📄
 *
 * <p>This is the whole of the seam between the language and the engine. Everything the reader does —
 * lexing, parsing, resolving type names, compiling an expression per rule — happens once, when files
 * are read, and produces {@link TypeMappingRule}s. From then on the engine asks this the same way it
 * asks the DSL registry and the annotation source, and cannot tell which of the three answered.</p>
 *
 * <p>⚠️ <strong>Reading a file is not this class's job and must not become it.</strong> A rule source
 * is consulted on the mapping path — the registry memoizes per pair, but a source that parsed on
 * demand would put a parser behind a cache miss, and a malformed file would surface as a failed
 * mapping in production rather than as a refused load at startup. Files are read once, up front, by
 * whatever assembles this; a lookup here is a map read and nothing else.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmRuleSource implements TypeMappingRuleSource {

    private final Map<TypePair, TypeMappingRule> rules = new ConcurrentHashMap<>();

    /**
     * Register the rule compiled for one pair.
     *
     * @param sourceType the source type the file named
     * @param targetType the target type the file named
     * @param rule       what the file said about the pair
     * @return this source, so a reader can chain registrations
     * @throws IllegalStateException when a pair is declared twice, because a target is described in
     *                               one file and two answers for one property have no precedence
     *                               between them
     */
    public JmmRuleSource register(Class<?> sourceType, Class<?> targetType, TypeMappingRule rule) {
        TypePair        pair     = new TypePair(sourceType, targetType);
        TypeMappingRule previous = rules.putIfAbsent(pair, Objects.requireNonNull(rule, "rule"));

        if (previous != null) {
            throw new IllegalStateException(
                    "A mapping from '%s' to '%s' is already declared. A target is described in one file."
                            .formatted(sourceType.getName(), targetType.getName()));
        }

        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Answers only for a pair a file named exactly. A source that widened this to assignable types
     * would decide, silently and per lookup, which of several files applies to a subclass — and the
     * language has no syntax for saying which should win.</p>
     */
    @Override
    public TypeMappingRule find(Class<?> sourceType, Class<?> targetType, MappingContext context) {
        return rules.get(new TypePair(sourceType, targetType));
    }

    /**
     * How many pairs were declared across every file read into this source.
     *
     * @return the number of registered rules
     */
    public int size() {
        return rules.size();
    }

    /**
     * The pair a rule is filed under.
     *
     * @param sourceType type mapped from
     * @param targetType type mapped into
     */
    private record TypePair(Class<?> sourceType, Class<?> targetType) {}
}
