/**
 * The rows the AI modules own: tool-call counters, provider settings, and pending confirmations for
 * products with nowhere else to hold them.
 *
 * <p>⚠️ <strong>Not an invocation trail, and that is settled rather than missing.</strong> A product
 * records a tool call and a human action as the <em>same</em> event, on purpose, so that "everything
 * created this week" stays one query rather than two reconciled by hand — a decision this library must
 * not make for it. Audit is an {@code InvocationTrace} a product writes; counters are here because
 * every adopter wants them and wants them identically. A product that wants both composes the two
 * behind one implementation, which is the arrangement that interface exists to allow.
 *
 * <p>Whoever owns the table owns the mapping. A product entity mapped over a library's schema is kept
 * honest only by schema validation, which is a hope rather than a contract — so these tables and their
 * migrations ship here, as they do for storage and access.
 *
 * <p>⚠️ <strong>Consuming products must add {@code org.jmouse.ai.jpa.entity} to their entity scan.</strong>
 * Forgetting starts cleanly and dies on the first query, which is the worst shape a failure can have.
 *
 * <p>Jakarta Persistence only. No Spring Data, no Spring: transaction demarcation belongs to whoever
 * calls — except where it deliberately does not, which is
 * {@link org.jmouse.ai.jpa.JpaCallCounter} and {@link org.jmouse.ai.jpa.JpaConfirmationStore}, and each
 * says why on itself.
 */
package org.jmouse.ai.jpa;
