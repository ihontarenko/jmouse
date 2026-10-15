/**
 * Reranking: reordering candidate documents by relevance to one query, through a bounded HTTP call.
 *
 * <p>{@link org.jmouse.ai.reranking.RerankModel} is the contract callers use;
 * {@link org.jmouse.ai.reranking.HttpRerankModel} composes one wire
 * {@link org.jmouse.ai.reranking.RerankProtocol} with one {@link org.jmouse.ai.reranking.RerankTransport}.
 * The module holds no persistence, routing, retries or product policy — those are the adopter's.
 */
package org.jmouse.ai.reranking;
