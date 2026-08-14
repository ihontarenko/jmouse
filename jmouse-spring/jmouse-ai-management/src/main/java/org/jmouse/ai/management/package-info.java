/**
 * Optional REST controllers over the AI read ports.
 *
 * <p>The read ports themselves live in {@code jmouse-ai} and are always available: what tools exist
 * and what each costs, what has been called and by whom, how much has been spent, which provider is
 * active. A product that wants its own management screen implements it over those and never adds this
 * module.
 *
 * <p>This module is for the other case — a working page in an afternoon — and is deliberately not part
 * of the starter. Shipping controllers in a starter would force the first product's route prefixes,
 * error bodies and authorization conventions on every later one.
 *
 * <p>These controllers read. They never reach a tool handler — only the dispatcher can — so this
 * cannot become a second way into an action.
 */
package org.jmouse.ai.management;
