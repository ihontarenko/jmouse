package org.jmouse.access.spi;

import java.util.Map;

/**
 * What is being done, for as long as it is being done — published by whoever is doing it.
 *
 * <p>{@link ConditionContext} carries an action and a bag of named values; this is where they come
 * from. A caller opens a window, states what the call is, and closes it. Every decision taken inside
 * that window sees them, including a programmatic {@code engine.decide(…)} three layers down, which
 * is where decisions are most often really made.
 *
 * <h2>⚠️ The core does not decide the lifetime, and that is the whole design</h2>
 *
 * <p>"Request-scoped" is what a web application will make of this. The engine has no request.
 * Whoever calls {@link #push} owns the window and closes it — an enforcement guard around a method,
 * a game loop around a player's turn, a job around its own body. A core that reached for a framework's
 * request scope would be a core that cannot run in a game.
 *
 * <h2>⚠️ A stack, not a slot</h2>
 *
 * <p>A nested publication <strong>covers</strong> the outer one and restores it on close. Replacing
 * it would mean an outer rule stops seeing its own action the moment an inner call returns, and that
 * is a refusal nobody can reproduce. {@link AutoCloseable} so the restore is
 * {@code try}-with-resources rather than discipline.
 *
 * <h2>⚠️ What a window does not do</h2>
 *
 * <p>It does not bound who can read it. A value published at the door is in context for every check
 * that happens before the window closes, related or not. What bounds a rule is {@link #action()} —
 * which is why an action is its own member rather than one more value in the bag.
 */
public interface AccessContextScope {

    /**
     * A scope that publishes nothing and accepts every publication silently.
     *
     * <p>What an installation that has not adopted this gets: {@link #action()} is null, the bag is
     * empty, and every condition written about either reads false. Deliberately not a failure — a
     * product wiring the engine without one has simply not started using actions yet.
     */
    static AccessContextScope none() {
        return new AccessContextScope() {

            @Override
            public AutoCloseable push(String action, Map<String, Object> values) {
                return () -> {
                    // Nothing was published, so there is nothing to restore.
                };
            }

            @Override
            public String action() {
                return null;
            }

            @Override
            public Map<String, Object> values() {
                return Map.of();
            }
        };
    }

    /**
     * Publishes an action and its values until the returned handle is closed.
     *
     * @param action what is being done, or null where the caller only has values to publish
     * @param values what it is being done with — copied, so a caller may reuse its map
     * @return the handle that restores whatever was published before this call
     */
    AutoCloseable push(String action, Map<String, Object> values);

    /** What is being done right now, or null where nothing published one. */
    String action();

    /** What was published alongside it. Never null; empty where nothing was. */
    Map<String, Object> values();

    /** One published value, or null where nothing published it. */
    default Object value(String name) {
        return values().get(name);
    }
}
