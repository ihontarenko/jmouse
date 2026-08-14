package org.jmouse.ai.mcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import org.jmouse.ai.PublishedTool;
import org.jmouse.ai.RefusalRendering;
import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.ToolOutcome;
import org.jmouse.ai.ToolRefusedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * The catalogue, published over the protocol. Decides nothing.
 *
 * <p>Every tool a client can see is a {@link PublishedTool}, and every handler does exactly one thing:
 * call {@link ToolDispatcher}. <strong>There is no second path into an action</strong> — which is now a
 * fact the compiler holds rather than a promise this class keeps, because a {@code PublishedTool}
 * carries no handler to call. A transport that wanted to invoke one directly has nothing to invoke.
 *
 * <p>The SDK's own annotation scanning is deliberately unused. The same catalogue serves an in-app
 * assistant and, in ticket 11, tools borrowed from another server; a tool discovered by annotation
 * would exist in one of those worlds and have nowhere to declare the permission it costs.
 *
 * <h2>Hints travel</h2>
 *
 * <p>Read-only and destructive become the protocol's own annotations, so a client can warn somebody
 * <em>before</em> a call rather than explain afterwards. They are hints and nothing rests on them: the
 * guards run whatever a client believed.
 *
 * <h2>A refusal is a result, never a protocol error</h2>
 *
 * <p>⚠️ The distinction is the difference between a tool that works and one that appears broken. A
 * protocol error tells the <em>client</em> the call was malformed, and a client tells its user that
 * something went wrong and stops. An error result tells the <em>model</em> its attempt was rejected and
 * hands it the sentence explaining why, and a model told why corrects itself and calls again. Only the
 * second is a conversation that gets anywhere.
 */
public final class McpToolServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpToolServer.class);

    private final ToolDispatcher dispatcher;
    private final String         serverName;
    private final String         serverVersion;

    public McpToolServer(ToolDispatcher dispatcher, String serverName, String serverVersion) {
        this.dispatcher    = dispatcher;
        this.serverName    = serverName;
        this.serverVersion = serverVersion;
    }

    // ── What a transport is handed ───────────────────────────────────────────────

    /**
     * Every action, as a protocol tool bound to the dispatcher.
     *
     * <p>Handed to a transport a product wired itself, for a product that wants the specifications
     * without this class also choosing how they are served.
     */
    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        List<McpServerFeatures.SyncToolSpecification> specifications =
                dispatcher.catalog().published().stream().map(this::specificationOf).toList();

        LOGGER.info("Publishing {} action(s) over the Model Context Protocol", specifications.size());

        return specifications;
    }

    /** A server over the streamable HTTP transport — what a servlet container is given in ticket 10. */
    public McpSyncServer serving(McpStreamableServerTransportProvider transport) {
        return McpServer.sync(transport)
                .serverInfo(serverName, serverVersion)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(specifications())
                .build();
    }

    /** The same over a single-session transport — stdio, and anything else outside a web stack. */
    public McpSyncServer serving(McpServerTransportProvider transport) {
        return McpServer.sync(transport)
                .serverInfo(serverName, serverVersion)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(specifications())
                .build();
    }

    // ── One tool ─────────────────────────────────────────────────────────────────

    private McpServerFeatures.SyncToolSpecification specificationOf(PublishedTool tool) {
        McpSchema.Tool published = McpSchema.Tool.builder()
                .name(tool.publishedName())
                .title(tool.title())
                .description(tool.description())
                .inputSchema(tool.inputSchema())
                .annotations(McpSchema.ToolAnnotations.builder()
                        .title(tool.title())
                        .readOnlyHint(tool.readOnly())
                        .destructiveHint(tool.destructive())
                        .build())
                .build();

        return new McpServerFeatures.SyncToolSpecification(
                published, (exchange, request) -> call(tool, request));
    }

    /**
     * One call, and the three things that can come back from it.
     *
     * <p>The text content is the one-line preamble — where it ran, and whether anything actually
     * happened — because a client rendering only that must not show a cheerful location over an
     * operation awaiting confirmation. The structured content is the payload with its scope.
     */
    private McpSchema.CallToolResult call(PublishedTool tool, McpSchema.CallToolRequest request) {
        Map<String, Object> arguments = request.arguments() == null ? Map.of() : request.arguments();

        try {
            ToolOutcome outcome = dispatcher.dispatch(tool.publishedName(), arguments);

            return McpSchema.CallToolResult.builder()
                    .addTextContent(outcome.describe())
                    .structuredContent(outcome.asStructuredContent())
                    .isError(false)
                    .build();

        } catch (ToolRefusedException refusal) {
            // The identical sentence an in-app assistant reads. Written once, in jmouse-ai, because two
            // implementations of one paragraph is how one of them ends up saying less than the other.
            return errorResult(RefusalRendering.render(refusal));

        } catch (RuntimeException failure) {
            LOGGER.warn("{} failed: {}", tool.qualifiedName(), failure.getMessage(), failure);

            return errorResult(RefusalRendering.renderFailure(tool.publishedName(), failure));
        }
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(message)
                .isError(true)
                .build();
    }
}
