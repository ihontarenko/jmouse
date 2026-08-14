/**
 * What a management screen is allowed to ask, and nothing it could act through.
 *
 * <p>Four read ports, shipped in {@code jmouse-ai} and therefore always present. The controllers over
 * them are a separate, optional artifact — a product that wants its own screen implements it against
 * these and never takes that module; a product that wants a working page in an afternoon takes it and
 * mounts it under its own prefix. Neither is the default, because there is no sensible default: a
 * library controller cannot know a product's error model, its authorization annotation or its admin
 * route conventions.
 *
 * <p><strong>Everything here reads.</strong> Not one of these ports carries a handler, a dispatcher or
 * anything that could reach one — {@link org.jmouse.ai.PublishedTool} is the richest thing any of them
 * returns. That is the property a reviewer will want to check, and it is checked: the whole point of
 * splitting the screens off is that they cannot become a second way into an action.
 *
 * <p>⚠️ And {@link org.jmouse.ai.view.ProviderRegistry} never returns a key. It answers <em>whether</em>
 * one is set. A management screen that can read a key back is a management screen that leaks one.
 */
package org.jmouse.ai.view;
