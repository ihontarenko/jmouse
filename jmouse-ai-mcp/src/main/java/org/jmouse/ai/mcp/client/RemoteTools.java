package org.jmouse.ai.mcp.client;

import org.jmouse.ai.ToolDefinition;

/**
 * What a connected server contributes, and the connection behind it.
 *
 * <p>Exists so that {@link McpToolClient#orAbsent} can answer with one type whether the server was
 * there or not. Without it the method returns a bare {@link ToolDefinition} and a product that wants to
 * close the connection at shutdown has to ask {@code instanceof} first — which is the shape of code
 * that gets written once, gets it right, and is then copied without the check.
 *
 * <p>⚠️ {@link #close()} narrows {@link AutoCloseable}'s to throw nothing, so closing needs no catch.
 * A server that was never reached closes to nothing at all, which is the honest answer rather than a
 * special case a caller has to know about.
 */
public interface RemoteTools extends ToolDefinition, AutoCloseable {

    @Override
    void close();
}
