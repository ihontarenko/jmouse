/**
 * The other half of the sandbox: the port to a model, exercised against something that answers.
 *
 * <p>{@code org.jmouse.ai.sandbox} runs tools; this runs {@code jmouse-ai-provider}. They are separate
 * because the two mechanisms are separate — a product may want either without the other, and
 * {@code jmouse-ai-conversation} is the only place they meet.
 *
 * <p>What is checked here cannot be read off the source: <strong>the body a model actually puts on the
 * wire.</strong> A canned answer proves nothing about a translation, and a text-only exchange
 * round-trips through anything, so the request carries the two shapes a response-only translation gets
 * away with ignoring — an assistant turn holding a tool call, and the turn answering it.
 */
package org.jmouse.ai.sandbox.provider;
