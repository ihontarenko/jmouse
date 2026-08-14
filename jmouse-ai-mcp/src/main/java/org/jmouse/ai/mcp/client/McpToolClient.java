package org.jmouse.ai.mcp.client;

import org.jmouse.ai.RefusalReason;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolDefinition;
import org.jmouse.ai.ToolInvocation;
import org.jmouse.ai.ToolOrigin;
import org.jmouse.ai.ToolRefusedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A server somebody else runs, contributed to this application's catalogue as ordinary tools.
 *
 * <p>It <em>is</em> a {@link ToolDefinition}, which is the whole point: a product adds it the same way
 * it adds a feature's own definition, and from every angle downstream — the dispatcher, the guards, the
 * trail, an assistant choosing what to call — a remote action is an action. It passes the same
 * permission gate, is counted the same way, and appears in the same list.
 *
 * <p><strong>What a caller cannot tell, and must not be able to.</strong> Nothing in the published
 * shape of these actions says "remote" except {@link ToolOrigin#REMOTE}, which is carried for a
 * management screen and a person reading a trail. A model choosing between a local action and a
 * forwarded one is choosing between two capabilities, not between two mechanisms.
 *
 * <h2>Three decisions this class makes, and why</h2>
 *
 * <p><strong>A destructive remote tool is refused at registration.</strong> ⚠️ Confirmation works by
 * showing somebody a resolved list of the records a call would destroy — and a remote tool cannot
 * resolve that list, because the records are on a machine this application cannot query. Registered
 * anyway, it would be a destructive action whose preview is empty, which reads as <em>"nothing will be
 * affected"</em> immediately before something is. So it is refused, here, with a sentence saying which
 * server and which tool, rather than at the catalogue's own check where the message could only say that
 * something lacks a resolver.
 *
 * <p><strong>A remote tool that merely <em>writes</em> is registered</strong>, and what that costs is
 * stated rather than hidden: it reaches the rate limit and the deduplication guard, and it slips past
 * the ceiling and the confirmation threshold, because both of those count records and this one resolves
 * none. The protocol's own convention — that anything not marked read-only should be assumed
 * destructive — is deliberately <em>not</em> applied, since applying it would refuse every remote write
 * tool in existence and that is a policy an installation should choose out loud rather than inherit.
 *
 * <p><strong>Remote actions are not scope-confined.</strong> This installation's places do not exist on
 * the other machine, and a scope resolved here and forwarded there would be an identifier the remote
 * has never seen. A server whose tools need narrowing takes an argument of its own for it.
 */
public final class McpToolClient implements RemoteTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpToolClient.class);

    /**
     * Arguments this library adds on the way in, which the remote never declared and would reject.
     *
     * <p>Not {@link ToolInvocation#RESERVED_ARGUMENTS}: {@code scope} and {@code limit} are absent from
     * that reason. A remote action is never scope-confined so {@code scope} cannot appear, and
     * {@code limit} is an ordinary argument a remote server may well declare itself — stripping it would
     * silently drop what a caller asked for.
     */
    private static final Set<String> LOCALLY_ADDED_ARGUMENTS =
            Set.of(ToolInvocation.CONFIRM_ARGUMENT, ToolInvocation.ALLOW_DUPLICATE_ARGUMENT);

    /** What a product's trail is told about a forwarded action, carried opaquely and never read here. */
    public static final String REMOTE_SERVER_ATTRIBUTE = "remoteServer";

    /** The tool's name on the far side, which is not the name it is published under here. */
    public static final String REMOTE_TOOL_ATTRIBUTE = "remoteTool";

    private final RemoteToolServer   server;
    private final RemoteToolSettings settings;
    private final List<ToolAction>   actions;

    /**
     * Connects, lists, and turns what came back into actions.
     *
     * <p>Everything happens in the constructor, so an unreachable server fails while
     * {@link #orAbsent} is still there to catch it, and a constructed client is one whose tools are
     * already in hand.
     *
     * @throws RemoteToolException   when the server cannot be reached or listed
     * @throws IllegalStateException when it offers a tool that declares itself destructive
     */
    public McpToolClient(RemoteToolServer server, RemoteToolSettings settings) {
        this.server   = server;
        this.settings = settings;
        this.actions  = server.listTools().stream().map(this::actionOf).toList();

        LOGGER.info("Connected to '{}' — {} remote action(s) under '{}': {}",
                settings.serverName(), actions.size(), settings.toolName(),
                String.join(", ", actions.stream().map(ToolAction::name).toList()));
    }

    /**
     * The same, except that a server which is not there leaves its tools absent instead of stopping the
     * application.
     *
     * <p>⚠️ <strong>Connecting and registering are caught separately, and that split is the point.</strong>
     * Everything {@code connect} does is somebody else's machine being reachable or not — including
     * building a transport, which can fail with anything a transport feels like throwing — so all of it
     * degrades to absent tools. Registration is a different question: <strong>a destructive remote tool
     * is a configuration disagreement between two installations, not an outage.</strong> It will still be
     * destructive tomorrow, and swallowing it would leave a product believing it had a capability that
     * was quietly dropped every start. So that one propagates, and only that one.
     *
     * <p>The absence is logged at warning level with the reason, because a set of tools that silently
     * stopped existing is the failure an operator otherwise discovers from an assistant saying it cannot
     * do something it could do last week.
     */
    public static RemoteTools orAbsent(
            Supplier<RemoteToolServer> connect, RemoteToolSettings settings) {

        RemoteToolServer server;

        try {
            server = connect.get();

        } catch (RuntimeException unreachable) {
            // Deliberately every runtime failure, not only RemoteToolException. A transport that will
            // not construct, a credential store that is empty, a host name that does not resolve — from
            // this application's side they are one fact: that server is not available right now, and
            // that must not be a reason for this application not to start.
            return absent(settings, unreachable);
        }

        try {
            return new McpToolClient(server, settings);

        } catch (RemoteToolException unlistable) {
            server.close();

            return absent(settings, unlistable);
        }
    }

    private static RemoteTools absent(RemoteToolSettings settings, RuntimeException reason) {
        LOGGER.warn("'{}' is not answering, so its tools are absent from this catalogue and anything "
                  + "that needs them will report that they do not exist. Everything local is "
                  + "unaffected. Reason: {}",
                settings.serverName(), reason.getMessage(), reason);

        return new AbsentServer(settings.toolName());
    }

    @Override
    public String toolName() {
        return settings.toolName();
    }

    @Override
    public List<ToolAction> actions() {
        return actions;
    }

    /** Let go of the connection. */
    @Override
    public void close() {
        server.close();
    }

    // ── One remote tool ──────────────────────────────────────────────────────────

    private ToolAction actionOf(RemoteTool tool) {
        refuseDestruction(tool);

        ToolAction.Builder action = ToolAction.builder()
                .toolName(settings.toolName())
                .name(tool.name())
                .title(tool.title() == null ? tool.name() : tool.title())
                .description(describe(tool))
                .publishedSchema(tool.inputSchema())
                .requiredPermission(settings.permissionFor(tool.name()))
                .origin(ToolOrigin.REMOTE)
                .traceAttribute(REMOTE_SERVER_ATTRIBUTE, settings.serverName())
                .traceAttribute(REMOTE_TOOL_ATTRIBUTE,   tool.name())
                .handler(invocation -> forward(tool, invocation));

        if (tool.readOnly()) {
            action.readOnly();
        }

        return action.build();
    }

    /**
     * The remote's own description, with where it comes from appended.
     *
     * <p>The remote's sentence is passed through untouched — rewriting somebody else's tool description
     * is how a model ends up calling it for a reason its author never intended. What is added is the
     * one fact the remote could not have written: that this is somewhere else, and so may be
     * unavailable in a way a local action never is.
     */
    private String describe(RemoteTool tool) {
        String described = tool.description() == null || tool.description().isBlank()
                ? tool.name()
                : tool.description();

        return described + " (Runs on '" + settings.serverName() + "', a server this application "
             + "connects to. It may be unavailable independently of everything else here.)";
    }

    private void refuseDestruction(RemoteTool tool) {
        if (!tool.destructive()) {
            return;
        }

        throw new IllegalStateException(
                "'" + settings.serverName() + "' offers a tool called '" + tool.name() + "' that "
                + "declares itself destructive, and it cannot be registered. A destructive action is "
                + "protected by showing somebody the list of records it would destroy before it runs, "
                + "and that list lives on '" + settings.serverName() + "' where this application cannot "
                + "read it — so the preview would be empty and would read as 'nothing will be affected' "
                + "immediately before something was. Either that tool stops declaring itself "
                + "destructive on the server that owns it, or this server is not registered here.");
    }

    private Object forward(RemoteTool tool, ToolInvocation invocation) {
        RemoteCallResult result = server.call(tool.name(), forwardable(invocation.arguments()));

        if (!result.refused()) {
            return result.payload();
        }

        // Its own reason rather than a permission refusal: this call was refused by somebody else's
        // rules, and sending whoever reads the trail to look at this installation's policy would waste
        // their afternoon.
        throw new ToolRefusedException(RefusalReason.REMOTE_REFUSED,
                "'" + settings.serverName() + "' refused this: " + result.text()
                + " That refusal is the remote server's, not this application's — its rules about what "
                + "this connection may do are its own. Nothing here was changed.");
    }

    private static Map<String, Object> forwardable(Map<String, Object> arguments) {
        Map<String, Object> forwarded = new LinkedHashMap<>(arguments);
        LOCALLY_ADDED_ARGUMENTS.forEach(forwarded::remove);

        return forwarded;
    }

    /** A server that was not answering when this application started. Contributes nothing, quietly. */
    private record AbsentServer(String toolName) implements RemoteTools {

        @Override
        public List<ToolAction> actions() {
            return List.of();
        }

        @Override
        public void close() {
            // There was never a connection. Closing to nothing is the honest answer, and it is what
            // lets a product close whatever orAbsent handed it without asking which of the two it got.
        }
    }
}
