/**
 * The mapping builder's server half — a form of rows on one side, `.jmm` on the other.
 *
 * <h2>Library-side, and that is the whole point of it</h2>
 *
 * <p>A mapping builder is a screen about a jMouse <em>language</em>, the way a query builder is. Built
 * inside one product it is a screen the next product copies, and a copied screen is a second
 * implementation of the rendering — which is the one thing this feature exists to prevent. So the
 * library serves it and a product mounts it.
 *
 * <p>Shaped like {@code jmouse-ai-management} deliberately: a separate artifact rather than part of any
 * starter, a route prefix nobody has to accept, no authorization annotation, and no error body. A
 * product that wants its own builder works against {@code org.jmouse.mapper.el.builder} directly and
 * never takes this module.
 *
 * <h2>The property a reviewer should check</h2>
 *
 * <p><strong>Nothing here writes the language.</strong> No controller concatenates a rule, quotes a
 * literal, or joins a filter chain. Rows become a {@code MappingDocumentNode} and the node is handed to
 * {@code JmmSourceTranslator} — the same call an editor's save goes through, so a document a browser
 * built and a document a person typed are rendered by one piece of code. A browser that assembled
 * {@code "reference : reference | trim | upper"} would be a second writer of the language, and the
 * first thing a second writer gets wrong is quoting.
 *
 * <h2>What this module refuses to assume</h2>
 *
 * <ul>
 *   <li><strong>A route prefix.</strong> {@code jmouse.mapper.management.prefix}, defaulting to
 *       something that plainly belongs to a library rather than to the product mounting it.
 *   <li><strong>Which classes are mappable.</strong> The product declares a
 *       {@link org.jmouse.mapper.management.MappableTypeSource}; there is no library default, because a
 *       guess of {@code nameEnds("Dto")} finds nothing in the first product it meets and looks like it
 *       is working while it does.
 *   <li><strong>An authorization annotation.</strong> Nothing here is guarded — the product mounts
 *       these behind its own gate. A product using {@code jmouse-access} states the rule with an
 *       {@code ExternalAccessRules} bean.
 * </ul>
 */
package org.jmouse.mapper.management;
