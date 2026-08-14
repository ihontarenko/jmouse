/**
 * The consumer the AI modules would otherwise not have until adoption.
 *
 * <p>Building the library before any product adopts it means every seam signature and every guard
 * boundary is a hypothesis until something calls it. This is what calls it: every seam implemented,
 * tool definitions covering the awkward cases — read-only, destructive with a record resolver,
 * scope-confined, nested object-list arguments — driven through all three entry points: the dispatcher
 * directly, the conversation runner, and a Model Context Protocol client.
 *
 * <p>If a shape is wrong, this is where it hurts, which is far cheaper than finding out during a
 * product cutover. Never published, and never depended on by anything.
 */
package org.jmouse.ai.sandbox;
