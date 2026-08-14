package org.jmouse.ai;

import java.util.Map;

/**
 * What a transport is allowed to see. <strong>No handler.</strong>
 *
 * <p>This type is the point of the module. A catalogue that handed out whole actions would leave the
 * guarantee <em>"there is no second path into an action, so there is no way to reach one without
 * passing the permission gate"</em> as a claim in a javadoc, honoured by every transport that
 * remembered to. Here it is the type system: {@link ToolCatalog} publishes these, the handler is
 * reachable only through {@link ToolDispatcher}, and a transport that wanted to call one directly
 * has nothing to call.
 *
 * <p>That matters because transports multiply. A Model Context Protocol server, an in-app assistant
 * and a management screen are three different authors reading the same catalogue, and the first one
 * to invoke a handler it happens to be holding does so without a permission check, a scope, a rate
 * limit or a trace — and nothing in a review would look wrong.
 *
 * <p>The permission is here even though a transport does not enforce it, because a management screen
 * answering <em>"what does this cost"</em> is a legitimate reader and the answer is not a secret: it is
 * declared in the source of every tool. What is not here is anything that lets the answer be
 * bypassed.
 *
 * @param publishedName  the wire spelling, {@code tool_action}
 * @param qualifiedName  how it is spoken in refusals, logs and conversation, {@code tool.action}
 * @param title          a short label for a client's tool picker
 * @param description    what it does, written for a model deciding whether to call it
 * @param inputSchema    JSON Schema for the arguments
 * @param requiredPermission what a caller must hold to run it
 * @param readOnly       true when it cannot change anything — a client may surface this as a hint
 * @param destructive    true when it removes or overwrites data
 * @param scopeConfined  whether it accepts and echoes a scope
 * @param origin         local, or forwarded to a server this application connected to
 */
public record PublishedTool(
        String              publishedName,
        String              qualifiedName,
        String              title,
        String              description,
        Map<String, Object> inputSchema,
        String              requiredPermission,
        boolean             readOnly,
        boolean             destructive,
        boolean             scopeConfined,
        ToolOrigin          origin
) {

    /** The namespace, for a screen grouping actions by the domain they belong to. */
    public String toolName() {
        return qualifiedName.substring(0, qualifiedName.indexOf('.'));
    }
}
