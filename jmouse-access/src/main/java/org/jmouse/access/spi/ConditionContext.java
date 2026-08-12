package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.Subject;

import java.util.Map;

/**
 * Everything a condition may see — and nothing else is reachable from one.
 *
 * <p>Deliberately five things, and every one of them was <em>handed</em> to the decision. A predicate
 * that could reach a repository, a clock or a static would be a predicate whose answer depends on
 * something the rule does not mention, and an authorization rule nobody can read from its own text is
 * worse than no rule.
 *
 * <h2>Three of them describe <em>who and where</em>; two describe <em>what is being done</em></h2>
 *
 * <p>{@link #subject()}, {@link #place()} and {@link #resource()} were here first and answer the two
 * questions the engine is built on. {@link #action()} and {@link #values()} are the third question,
 * and the reason they are cheap is that both are <strong>constant for a call</strong>: a purpose read
 * out of the path is the same for every row the call would return, so a rule about it is evaluated
 * once and decides whether the call happens at all. Nothing here narrows a set, so neither
 * resolve-once nor filterability is touched.
 *
 * <p>⚠️ <strong>A value is published by whoever made the call, not discovered.</strong> Nothing in
 * the engine can put something in this bag; an action nobody published is null and a value nobody
 * published is absent, and a rule written against either simply does not hold. That is the correct
 * outcome for a conditional <em>allow</em> and the dangerous one for a conditional <em>deny</em> —
 * which is why a declaration promising a value is held to it at the door rather than here.
 */
public interface ConditionContext {

    /** Who is asking. Identifiers and flags only — a {@link Subject} carries nothing else. */
    Subject subject();

    /** The scope the grant is being evaluated at. */
    ScopeReference place();

    /**
     * The resolved target's row, read-only, or null where the call named none.
     *
     * <p>⚠️ A condition that dereferences it <strong>must tolerate null</strong>: plenty of routes are
     * aimed at a place rather than a row, and a rule that threw on those would refuse them for the
     * wrong reason.
     */
    Object resource();

    /**
     * What is being done — {@code entry.listByPurpose}, {@code label.print} — or null where the caller
     * published none.
     *
     * <p><strong>This is a rule's own scope, and it is the member that makes the whole mechanism
     * safe.</strong> Published values live from publication to the end of the call, which means a
     * value put there at the door is in context for every later check including unrelated ones. A rule
     * written {@code when action == 'entry.listByPurpose' and …} cannot hold anywhere else, and that
     * is the bound — not the lifetime.
     */
    String action();

    /**
     * What the caller published alongside it. Never null; empty where nothing was.
     *
     * <p>Read-only to a condition. A rule reads what it was given and answers; one that could write
     * back would make the next rule's answer depend on which rule ran first.
     */
    Map<String, Object> values();

    /** One published value, or null where nothing published it. */
    default Object value(String name) {
        return values().get(name);
    }

    /** What a caller that publishes nothing sees — the three original members, and no action. */
    static ConditionContext of(Subject subject, ScopeReference place, Object resource) {
        return of(subject, place, resource, null, Map.of());
    }

    /** The five, as one value. */
    static ConditionContext of(
            Subject             subject,
            ScopeReference      place,
            Object              resource,
            String              action,
            Map<String, Object> values) {

        return new Of(subject, place, resource, action, values == null ? Map.of() : Map.copyOf(values));
    }

    record Of(
            Subject             subject,
            ScopeReference      place,
            Object              resource,
            String              action,
            Map<String, Object> values
    ) implements ConditionContext {
    }
}
