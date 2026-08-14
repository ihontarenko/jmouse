/**
 * The tool-calling loop, and only the loop.
 *
 * <p>Per round: render the catalogue's published tools for the provider, ask the model, and hand every
 * tool call it makes to {@code ToolDispatcher} — the same dispatcher a Model Context Protocol client
 * reaches from outside the process, in the same transaction, under the same caller. Append the
 * results, ask again, until the model stops or the budget runs out.
 *
 * <p>A budget rather than a round counter, because rounds are only half of what runs away: token usage
 * arrives on every response and is what actually costs. A conversation that has spent its budget ends
 * with a stated reason rather than an exception carrying a magic number.
 *
 * <p>A refusal is rendered as a result the model reads and retries against, never as a transport
 * error. A client told "something went wrong" reports exactly that to the user; a model told
 * <em>why</em> it was refused corrects itself and calls again. Both entry points render refusals the
 * same way, from the same text, which is why that text lives one module down.
 *
 * <p>This package holds no prompt, persists no conversation, and knows no product's vocabulary. Those
 * belong to the caller and are the first things that will try to leak in here.
 */
package org.jmouse.ai.conversation;
