/**
 * Reusable embedding execution and token forecasting without product persistence or conversation tools.
 *
 * <p>{@link org.jmouse.ai.embeddings.EmbeddingModel} is the caller's operation contract.
 * {@link org.jmouse.ai.embeddings.HttpEmbeddingModel} composes an open
 * {@link org.jmouse.ai.embeddings.EmbeddingProtocol} with an injected bounded transport.
 * Each protocol owns only its request and response format. New protocols are registered as models,
 * without a closed protocol enum. Provider management remains in jmouse-ai-provider.</p>
 *
 * <p>Requests preserve text and native dimension expectations. Responses preserve original input order,
 * immutable coordinates and nullable reported usage. Operational errors carry typed safe reasons,
 * without credentials, corpus text or provider response bodies. Scheduling, cost admission, retries,
 * cancellation and storage belong to the adopting application.</p>
 */
package org.jmouse.ai.embeddings;
