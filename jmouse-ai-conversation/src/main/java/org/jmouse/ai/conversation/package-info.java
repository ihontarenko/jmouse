/**
 * The tool-calling loop, and only the loop.
 *
 * <p>Ask the model, hand what it asked for to {@code ToolDispatcher}, hand the results back, ask again,
 * and stop when it finishes or when the budget does. Everything else a loop is tempted to grow — a tool
 * lookup, a permission check, an {@code execute} method, an exception wrapper — already exists one
 * module down, and a second copy of any of it is how an in-app assistant and an external client stop
 * agreeing about what a caller may do.
 *
 * <p>The budget bounds rounds <em>and</em> tokens, because a conversation is resent whole every round
 * and a round cap alone bounds the wrong quantity.
 *
 * <p>⚠️ <strong>No prompt, no persistence, no product vocabulary.</strong> Those belong to the caller
 * and are the first things that will try to leak in here. English in this package that is not a refusal
 * or a stated reason for stopping is a bug.
 */
package org.jmouse.ai.conversation;
