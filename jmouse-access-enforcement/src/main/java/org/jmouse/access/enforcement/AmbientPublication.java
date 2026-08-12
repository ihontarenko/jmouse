package org.jmouse.access.enforcement;

import java.util.function.Supplier;

/**
 * Where a product attaches the values that are true of every call, by name.
 *
 * <p>Handed to {@link AmbientAccessValues#publish}, which registers against it and returns. Two
 * things are read out of the result and they are read at very different times, which is the whole
 * reason this is a registration seam rather than a map factory:
 *
 * <ul>
 *   <li><strong>The names</strong>, at startup, so the catalogue that checks rules knows what may be
 *       written. ⚠️ This is what removes the hand-written list a configuration used to repeat beside
 *       the bean — the same fact stated twice, and silently wrong the day the two disagree.
 *   <li><strong>The values</strong>, per guarded call, and only the ones something actually reads.
 * </ul>
 *
 * <h2>⚠️ The body of {@code publish} must read nothing</h2>
 *
 * <p>It registers names and suppliers; it does not work anything out. That is the entire discipline
 * that makes the startup pass safe — {@code publish} is called once outside any request to collect
 * names, and an implementation that read a request attribute there would fail at boot.
 *
 * <p>{@link #attach} exists for a value already in hand: a configuration property, a deployment name,
 * an installation identifier. Anything <em>computed</em> uses {@link #attachLazy}, and the difference
 * is not stylistic — {@code attach(name, readIt())} evaluates {@code readIt()} before {@code attach}
 * is even entered, including during the startup pass. That fails loudly at boot, which is the right
 * direction and needs no guard.
 */
public interface AmbientPublication {

    /**
     * A value already in hand.
     *
     * @param name  what a rule calls it. ⚠️ Read by an administrator, so it is the word they would
     *              use — {@code spaceKind}, not {@code subjectAreaCode}
     * @param value what it is; null attaches the name and no value, which reads to a rule exactly as
     *              an absent value does
     */
    AmbientPublication attach(String name, Object value);

    /**
     * A value worked out on first read, and only if something reads it.
     *
     * <p>The supplier runs <strong>at most once per call</strong> and never at startup. A rule that
     * does not mention the name does not pay for it at all — which is what makes it reasonable for
     * one of these to reach a repository, where an eagerly published value never could.
     *
     * <p>⚠️ It must not throw for ordinary data, and if it does the value is simply absent. A rule
     * reading an absent value does not hold: safe for a conditional allow, and the open direction for
     * a conditional deny.
     */
    AmbientPublication attachLazy(String name, Supplier<Object> value);
}
