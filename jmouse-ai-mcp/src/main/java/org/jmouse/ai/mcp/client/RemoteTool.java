package org.jmouse.ai.mcp.client;

import java.util.Map;

/**
 * One thing a server this application connected to says it can do.
 *
 * <p>Everything the protocol reports about a tool and nothing this application decides about it. What
 * it <em>costs</em> is deliberately absent: a remote server's idea of what its tools are worth is not
 * this installation's idea, and that answer comes from {@link RemoteToolSettings} instead.
 *
 * <p>A plain record rather than the protocol's own type so that {@link RemoteToolServer} can be
 * implemented by something that is not the Model Context Protocol SDK — a fake, a second protocol, an
 * in-process bridge — without any of them having to construct a schema object they do not own.
 *
 * @param name        the tool as the remote server names it
 * @param title       a short label, or null
 * @param description what it does, written for a model. Passed through unchanged: rewriting somebody
 *                    else's tool description is how a model ends up calling it for the wrong reason
 * @param inputSchema the arguments, as the remote published them
 * @param readOnly    whether the remote says it changes nothing
 * @param destructive whether the remote says it removes or overwrites something. ⚠️ See
 *                    {@link McpToolClient} — a tool that says yes is refused at registration
 */
public record RemoteTool(
        String              name,
        String              title,
        String              description,
        Map<String, Object> inputSchema,
        boolean             readOnly,
        boolean             destructive
) {

    public RemoteTool {
        inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
    }
}
