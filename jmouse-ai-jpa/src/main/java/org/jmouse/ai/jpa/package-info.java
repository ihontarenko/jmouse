/**
 * The rows the AI modules own: tool-call counters, the invocation trail, provider settings, and
 * pending confirmations for products with nowhere else to hold them.
 *
 * <p>Whoever owns the table owns the mapping. A product entity mapped over a library's schema is kept
 * honest only by schema validation, which is a hope rather than a contract — so these tables and their
 * migrations ship here, as they do for storage and access.
 *
 * <p>⚠️ Consuming products must add this package to their entity scan. Forgetting starts cleanly and
 * dies on the first query, which is the worst shape a failure can have.
 *
 * <p>Jakarta Persistence only. No Spring Data, no Spring: transaction demarcation belongs to whoever
 * calls, not to the library.
 */
package org.jmouse.ai.jpa;
