package org.jmouse.ai.mcp.client;

import java.util.List;
import java.util.Map;

/**
 * A server this application has connected to, reduced to the two questions a catalogue needs answered.
 *
 * <p>An interface rather than the protocol client itself, for a reason that shows up immediately:
 * {@link McpToolClient} turns a remote server into catalogue entries, and everything interesting about
 * that translation — how a permission is assigned, what happens to a destructive tool, how a failure
 * reads — is testable and demonstrable only if the server on the other side can be something other than
 * a socket. {@link McpSyncRemoteToolServer} is the one that speaks the real protocol; it is not the only
 * thing that may sit here.
 *
 * <p>Two methods, because two is all a catalogue wants: what can you do, and do this. Everything else
 * the protocol offers — resources, prompts, sampling, roots — is a conversation this library is not
 * having, and an interface that mentioned them would have to be implemented by everything that never
 * will.
 *
 * <p><strong>Implementations connect eagerly</strong>, so that a server which is not there fails while
 * something is still willing to catch it. See {@link McpToolClient#orAbsent} for what catches it.
 */
public interface RemoteToolServer extends AutoCloseable {

    /**
     * How this server is named to a person — in a log line, in a refusal, on a management screen.
     *
     * <p>The product's name for it, not the server's own, because the product is what has to be able to
     * find it again when it stops answering.
     */
    String serverName();

    /**
     * Everything it says it can do.
     *
     * @throws RemoteToolException when it cannot be reached or its answer cannot be read
     */
    List<RemoteTool> listTools();

    /**
     * Run one of them.
     *
     * @param toolName  the tool as the <em>remote</em> names it, never the local published name
     * @param arguments the arguments, already stripped of anything this library added
     * @throws RemoteToolException when it cannot be reached, or answers with something unreadable
     */
    RemoteCallResult call(String toolName, Map<String, Object> arguments);

    /** Let go of the connection. Narrowed from {@link AutoCloseable} so that closing needs no catch. */
    @Override
    void close();
}
