package org.jmouse.ai.mcp.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A remote server reached over the Model Context Protocol itself.
 *
 * <p>The one implementation of {@link RemoteToolServer} that speaks the actual protocol, and the only
 * class in the client half that touches the SDK. Everything above it — how a permission is assigned,
 * what happens to a destructive tool, how a failure reads to a model — works against the interface, so
 * none of it is testable only against a socket.
 *
 * <p>Transport is the caller's choice and deliberately not this class's: stdio for a tool that runs as a
 * subprocess, streamable HTTP for a service, whatever the SDK grows next. What this adds is the
 * translation and the failure story, both of which are the same whichever pipe the bytes went down.
 *
 * <p>⚠️ <strong>Every SDK failure becomes a {@link RemoteToolException} naming the server.</strong> The
 * exceptions the protocol client raises describe a session or a transport; nothing in them says which of
 * three configured servers stopped answering, and that is the only thing anybody wants to know at the
 * moment one does.
 */
public final class McpSyncRemoteToolServer implements RemoteToolServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpSyncRemoteToolServer.class);

    /** Long enough for a server doing real work, short enough that a hung one is not mistaken for slow. */
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String        serverName;
    private final McpSyncClient client;

    /**
     * Connects and completes the protocol handshake.
     *
     * @throws RemoteToolException when the server is not there, or will not agree a session
     */
    public McpSyncRemoteToolServer(String serverName, McpClientTransport transport) {
        this(serverName, transport, DEFAULT_REQUEST_TIMEOUT);
    }

    public McpSyncRemoteToolServer(
            String serverName, McpClientTransport transport, Duration requestTimeout) {

        this.serverName = serverName;

        // ⚠️ Building the client is inside the try as well as handshaking with it. A transport that
        // will not construct — a bad address, a subprocess that is not on the path, a credential store
        // that is empty — throws whatever it likes from here, and letting that escape raw would stop
        // an application from starting because somebody else's machine is being restarted.
        try {
            this.client = McpClient.sync(transport).requestTimeout(requestTimeout).build();

            McpSchema.InitializeResult agreed = client.initialize();

            LOGGER.info("'{}' is {} {}", serverName,
                    agreed.serverInfo().name(), agreed.serverInfo().version());

        } catch (RuntimeException unreachable) {
            throw failure("could not be reached", unreachable);
        }
    }

    /** For a client built elsewhere — already initialised, already configured. */
    public McpSyncRemoteToolServer(String serverName, McpSyncClient client) {
        this.serverName = serverName;
        this.client     = client;
    }

    @Override
    public String serverName() {
        return serverName;
    }

    /**
     * Every tool it offers, following the cursor to the end.
     *
     * <p>Paging is followed rather than ignored, because a truncated first page is a catalogue that is
     * silently missing capability — and the way that surfaces is an assistant insisting a tool does not
     * exist while somebody is looking at it in the server's own documentation.
     */
    @Override
    public List<RemoteTool> listTools() {
        List<RemoteTool> collected = new ArrayList<>();
        String           cursor    = null;

        try {
            do {
                McpSchema.ListToolsResult page = cursor == null
                        ? client.listTools()
                        : client.listTools(cursor);

                page.tools().stream().map(McpSyncRemoteToolServer::asRemoteTool).forEach(collected::add);
                cursor = page.nextCursor();

            } while (cursor != null);

        } catch (RuntimeException unreadable) {
            throw failure("could not be asked what it offers", unreadable);
        }

        return List.copyOf(collected);
    }

    @Override
    public RemoteCallResult call(String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolResult answer;

        try {
            answer = client.callTool(new McpSchema.CallToolRequest(toolName, arguments));

        } catch (RuntimeException unanswered) {
            throw failure("did not answer a call to '" + toolName + "'", unanswered);
        }

        return asCallResult(answer);
    }

    @Override
    public void close() {
        try {
            client.close();

        } catch (RuntimeException alreadyGone) {
            // Closing a connection that is already broken is the ordinary case at shutdown, and a
            // failure here has nothing left to protect.
            LOGGER.debug("'{}' did not close cleanly: {}", serverName, alreadyGone.getMessage());
        }
    }

    // ── Translation ──────────────────────────────────────────────────────────────

    /*
     * Public, and this is the one place either direction of the translation is written.
     *
     * ⚠️ Not a convenience. Anything else that speaks the protocol on this application's behalf — an
     * in-process bridge, a fake, a second transport — has to turn the SDK's types into these, and a
     * second copy of that mapping is how one of them ends up reading `destructiveHint` differently
     * from the other. It lives here because this is a class the architecture rule already lets see
     * the SDK, so putting it anywhere more neutral would mean widening that rule.
     */

    /** One tool as the protocol describes it, as the catalogue needs it. */
    public static RemoteTool asRemoteTool(McpSchema.Tool tool) {
        McpSchema.ToolAnnotations hints = tool.annotations();

        return new RemoteTool(
                tool.name(),
                tool.title(),
                tool.description(),
                tool.inputSchema(),
                hints != null && Boolean.TRUE.equals(hints.readOnlyHint()),
                // ⚠️ TRUE only where the server said so. The protocol's own convention is that an
                // absent hint on a writing tool means destructive, and applying it here would refuse
                // every remote write tool in existence — a policy an installation should choose out
                // loud rather than inherit from a default. See McpToolClient.
                hints != null && Boolean.TRUE.equals(hints.destructiveHint()));
    }

    /**
     * One answer as the protocol sends it, as the dispatcher needs it.
     *
     * <p>⚠️ The whole of the distinction lives in this method: {@code isError} is what separates
     * <em>"here is your answer"</em> from <em>"I will not do that"</em>, and collapsing the two would
     * leave a refusal from somebody else's machine looking exactly like a successful result whose
     * payload happened to be an apology.
     */
    public static RemoteCallResult asCallResult(McpSchema.CallToolResult answer) {
        String said = textOf(answer);

        return Boolean.TRUE.equals(answer.isError())
                ? RemoteCallResult.refused(said)
                : RemoteCallResult.answered(answer.structuredContent(), said);
    }

    private static String textOf(McpSchema.CallToolResult answer) {
        return answer.content() == null
                ? ""
                : answer.content().stream()
                        .filter(McpSchema.TextContent.class::isInstance)
                        .map(McpSchema.TextContent.class::cast)
                        .map(McpSchema.TextContent::text)
                        .collect(Collectors.joining(" "));
    }

    private RemoteToolException failure(String whatHappened, RuntimeException cause) {
        return new RemoteToolException(serverName,
                "'" + serverName + "' " + whatHappened + ": " + describe(cause)
                + ". That is a server this application connects to, not a fault in this application's "
                + "own tools — everything local is unaffected.", cause);
    }

    private static String describe(RuntimeException failure) {
        return failure.getMessage() == null
                ? failure.getClass().getSimpleName()
                : failure.getClass().getSimpleName() + " (" + failure.getMessage() + ")";
    }
}
