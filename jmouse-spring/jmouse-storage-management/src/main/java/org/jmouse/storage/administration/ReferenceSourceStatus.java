package org.jmouse.storage.administration;

/**
 * 🔗 One reference source, and whether it is answering.
 *
 * <h3>⚠️ Why this screen exists at all</h3>
 *
 * <p>The sweeper reclaims what appears in <strong>no</strong> declared source. So the question "which
 * sources are registered, and what does each of them report" is the difference between a sweep that
 * reclaims leaked bytes and one that deletes live data — and until now there was nowhere to ask it.
 * A source that answers <strong>zero</strong> is the shape worth staring at: it may be honest, or it may
 * be a query that silently stopped matching.</p>
 *
 * @param name       what the source calls itself in a report
 * @param references how many identifiers it currently reports
 * @param failed     whether asking it threw, in which case {@code references} is meaningless
 * @param failure    what it threw, or {@code null}
 */
public record ReferenceSourceStatus(String name, long references, boolean failed, String failure) {
}
