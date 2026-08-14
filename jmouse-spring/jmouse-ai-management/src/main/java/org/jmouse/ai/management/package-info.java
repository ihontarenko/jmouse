/**
 * A working management page in an afternoon, for a product that does not want to write one.
 *
 * <p><strong>A separate artifact rather than part of the starter, and the split is the design.</strong>
 * The read ports live in {@code jmouse-ai} and are always shipped; these controllers are opt-in. A
 * product that wants its own screen implements it over {@link org.jmouse.ai.view.ToolCatalogView},
 * {@link org.jmouse.ai.view.ToolCallHistory}, {@link org.jmouse.ai.view.UsageTotals} and
 * {@link org.jmouse.ai.view.ProviderRegistry}, and never takes this module. A product that wants a page
 * now adds it and mounts it under its own prefix.
 *
 * <p>Neither is the default, because there is no sensible default to pick. Shipping controllers in the
 * starter would force the first product's route prefixes, error bodies and authorization conventions on
 * every later one — which is why neither of the other jMouse starters ships a single controller.
 *
 * <h2>The property a reviewer should check</h2>
 *
 * <p><strong>These controllers read, and structurally cannot do anything else.</strong> Not one of them
 * holds a {@code ToolDispatcher}, a {@code ToolCatalog} or a {@code ToolAction}; the richest thing any
 * of them can reach is a {@link org.jmouse.ai.PublishedTool}, which carries no handler. So this module
 * cannot become a second way into an action — not by convention, but because there is nothing here to
 * call. That is worth verifying rather than believing, and there is an architecture rule that does.
 *
 * <h2>What this module refuses to assume</h2>
 *
 * <ul>
 *   <li><strong>A route prefix.</strong> {@code jmouse.ai.management.prefix}, defaulting to something
 *       that plainly belongs to a library rather than to the product mounting it.
 *   <li><strong>An authorization annotation.</strong> Nothing here is guarded — ⚠️ the product mounts
 *       these behind its own gate, and a product that mounts them behind nothing has published its call
 *       history and its provider configuration.
 *   <li><strong>An error body.</strong> These throw; a product's own handler decides what a client sees.
 * </ul>
 */
package org.jmouse.ai.management;
