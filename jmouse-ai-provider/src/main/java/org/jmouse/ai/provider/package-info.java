/**
 * The port to a language model, and the implementations that speak to one.
 *
 * <p>{@code ChatModel} is what a caller holds. Which provider is behind it — Anthropic directly,
 * OpenAI, or an HTTP gateway elsewhere speaking the canonical shape — is configuration, and the
 * conversation loop cannot tell the difference. That is the point: the same code runs against a
 * provider in development and a shared gateway in production, and neither is named in the caller.
 *
 * <p>Settings are read per call rather than bound at startup, so rotating a key or switching provider
 * takes effect on the next request with no restart.
 *
 * <p>Timeouts belong here rather than to the caller. A hung provider otherwise blocks the calling
 * thread indefinitely, and enough of those exhaust a server's whole pool — generous but finite, because
 * a provider can legitimately take a while to finish a multi-tool-call response.
 *
 * <p>Content blocks stay opaque maps, read through a thin accessor. Over-modelling every provider's
 * block variants would be duplicate maintenance of a shape nothing in this path interprets — the loop
 * needs to know that a block is a tool call and what it asks for, and nothing more.
 *
 * <p>Deliberately independent of {@code jmouse-ai}: talking to a model and running a tool are two
 * mechanisms that meet only in {@code jmouse-ai-conversation}.
 */
package org.jmouse.ai.provider;
