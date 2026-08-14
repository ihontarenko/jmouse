/**
 * The tool mechanism: what a tool is, and the one gate that stands between a caller and one.
 *
 * <p>The organising sentence of these modules is that <strong>the catalogue is one, the dispatcher is
 * one, and every transport is an adapter over them</strong>. An in-app assistant, a Model Context
 * Protocol server and a management API are three ways to arrive at the same {@code ToolDispatcher};
 * none of them is a second implementation of it, and an assistant never speaks MCP to reach a tool
 * that is already in its own catalogue.
 *
 * <p>Two types carry that guarantee. {@code ToolCatalog} publishes {@code PublishedTool} — name,
 * title, description, schema, hints — and nothing else, so a transport structurally cannot hold a
 * handler. {@code ToolDispatcher} alone resolves one, in a fixed order: identity, existence,
 * permission, scope, guards, work. The order is not arbitrary and its reasoning belongs on the
 * dispatcher itself.
 *
 * <p>The action, not the tool, is the unit of the catalogue. A tool is only a namespace over a
 * domain; everything that varies — the arguments, the permission it costs, whether it changes
 * anything — varies per action, and modelling the tool as the unit would force one permission to
 * cover reading and deleting alike.
 *
 * <p>This package depends on {@code jmouse-core} and nothing else. Persistence, HTTP, dependency
 * injection, a model provider and an authorization engine are all somebody else's; the seams under
 * {@link org.jmouse.ai.spi} are how they get in.
 */
package org.jmouse.ai;
