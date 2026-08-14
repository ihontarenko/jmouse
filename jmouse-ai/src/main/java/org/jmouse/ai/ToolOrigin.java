package org.jmouse.ai;

/**
 * Where an action's work happens.
 *
 * <p>Carried into {@link PublishedTool} so a management screen can say where a capability comes from,
 * and for nothing else — <strong>a caller cannot tell the difference and must not be able to.</strong>
 * The catalogue is the union of what this application can do and what it has connected to; both pass
 * the same permission gate and the same guards, and an assistant calls both the same way.
 *
 * <p>That indistinguishability is what makes the design pay off. Without one catalogue, the cheapest
 * way to give an in-app assistant the actions an MCP server already publishes is to point a client at
 * the application's own endpoint — HTTP to self, a second authentication, a second transaction, and a
 * permission gate evaluated against the wrong caller. With one catalogue there is nothing to connect
 * to, because a local action is already in it.
 */
public enum ToolOrigin {

    /** The handler is a method in this application. */
    LOCAL,

    /** The handler forwards to a server this application has connected to. */
    REMOTE
}
