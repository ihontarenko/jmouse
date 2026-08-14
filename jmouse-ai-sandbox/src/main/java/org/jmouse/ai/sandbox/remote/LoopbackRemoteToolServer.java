package org.jmouse.ai.sandbox.remote;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.jmouse.ai.mcp.McpToolServer;
import org.jmouse.ai.mcp.client.McpSyncRemoteToolServer;
import org.jmouse.ai.mcp.client.RemoteCallResult;
import org.jmouse.ai.mcp.client.RemoteTool;
import org.jmouse.ai.mcp.client.RemoteToolException;
import org.jmouse.ai.mcp.client.RemoteToolServer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A second workshop's protocol server, reached without a socket.
 *
 * <p><strong>What this stands in for, and what it does not.</strong> Two sandbox instances genuinely
 * talking over a pipe would need a subprocess, a classpath and a startup race, and would prove one thing
 * this arrangement does not: that the bytes survive the wire. Everything <em>else</em> the ticket asks
 * for is proved here and proved honestly, because the call goes through the far server's real
 * {@link McpServerFeatures.SyncToolSpecification} handler — the same one a socket would reach after
 * deserialising — and therefore through its dispatcher, its permission gate, its scope resolution, its
 * guards and its refusal rendering. What is skipped is the transport, and the transport is the SDK's.
 *
 * <p>⚠️ <strong>The translation is not skipped either.</strong> Both directions go through
 * {@link McpSyncRemoteToolServer}'s own static translators rather than a second copy of them here —
 * which is what stops this class from quietly disagreeing with the real client about, say, what an
 * absent {@code destructiveHint} means. What the real client still has that nothing calls is purely
 * the SDK's: opening a session, and following a page cursor.
 *
 * <p>The exchange handed to that handler is null, which is what the server's own code already ignores.
 *
 * <p>⚠️ {@link #withoutTools} exists because of something the demonstration is <em>supposed</em> to run
 * into: the far workshop offers a destructive action, and a destructive remote tool is refused at
 * registration. Excluding it by name is how the second half of the scenario gets to happen at all, and
 * it is also exactly what a product does when a remote server offers something this installation has
 * decided not to expose.
 */
public final class LoopbackRemoteToolServer implements RemoteToolServer {

    private final String        serverName;
    private final McpToolServer far;
    private final Set<String>   excluded;

    private boolean closed;

    public LoopbackRemoteToolServer(String serverName, McpToolServer far) {
        this(serverName, far, Set.of());
    }

    private LoopbackRemoteToolServer(String serverName, McpToolServer far, Set<String> excluded) {
        this.serverName = serverName;
        this.far        = far;
        this.excluded   = excluded;
    }

    /** The same server, with some of what it offers left out of this installation's catalogue. */
    public LoopbackRemoteToolServer withoutTools(String... names) {
        return new LoopbackRemoteToolServer(serverName, far, Set.of(names));
    }

    @Override
    public String serverName() {
        return serverName;
    }

    @Override
    public List<RemoteTool> listTools() {
        requireOpen();

        return specifications().stream()
                .map(McpServerFeatures.SyncToolSpecification::tool)
                .filter(tool -> !excluded.contains(tool.name()))
                .map(McpSyncRemoteToolServer::asRemoteTool)
                .toList();
    }

    @Override
    public RemoteCallResult call(String toolName, Map<String, Object> arguments) {
        requireOpen();

        McpServerFeatures.SyncToolSpecification specification = specifications().stream()
                .filter(candidate -> candidate.tool().name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new RemoteToolException(serverName,
                        "'" + serverName + "' has no tool called '" + toolName + "'. It offers "
                        + specifications().stream()
                                .map(offered -> offered.tool().name())
                                .collect(Collectors.joining(", ")) + "."));

        return McpSyncRemoteToolServer.asCallResult(specification.callHandler()
                .apply(null, new McpSchema.CallToolRequest(toolName, arguments)));
    }

    @Override
    public void close() {
        closed = true;
    }

    private List<McpServerFeatures.SyncToolSpecification> specifications() {
        return far.specifications();
    }

    private void requireOpen() {
        if (closed) {
            throw new RemoteToolException(serverName,
                    "'" + serverName + "' was disconnected and this call did not reach it.");
        }
    }
}
