/**
 * The library's own migrations, and how a dialect is chosen.
 *
 * <p>⚠️ The dialect comes from the DATA SOURCE, never from an application profile — a product may have
 * no profile naming its database, or may name it differently from every other product here. The
 * connection always knows.
 *
 * <p>⚠️ These files are APPEND-ONLY from first release. Editing one that has run in somebody's
 * installation breaks {@code validate-on-migrate} there, and no amount of local correctness fixes that.
 */
package org.jmouse.telegram.jpa.migration;
