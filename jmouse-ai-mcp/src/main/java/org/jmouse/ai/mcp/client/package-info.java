/**
 * The other half of the protocol: reaching a server somebody else runs, and putting what it can do into
 * the <em>same</em> catalogue as everything local.
 *
 * <p><strong>This is the symmetry the whole design pays off with.</strong> Once the catalogue is the
 * union of local and remote capability, an assistant calls both identically — same permission gate,
 * same scope resolution, same guards, same trail — and <em>"do not speak the protocol to reach
 * yourself"</em> stops being advice somebody has to remember. A local action is already in the
 * catalogue, so there is nothing to connect to. Without this package that rule holds only because
 * everyone keeps holding it.
 *
 * <p><strong>What the product supplies, because the library cannot know it.</strong> A remote server's
 * own idea of what its tools are worth is not this installation's idea, so the permission each remote
 * tool costs is stated here rather than read off the wire — see {@link
 * org.jmouse.ai.mcp.client.RemoteToolSettings}. The catalogue refuses an action with no permission, and
 * it is right to.
 *
 * <p><strong>Origin travels.</strong> Every action registered here carries
 * {@link org.jmouse.ai.ToolOrigin#REMOTE}, so a management screen and a person reading a trail can tell
 * where a capability came from. A <em>caller</em> still cannot, and must not be able to.
 *
 * <p><strong>Failure is isolated and legible.</strong> A server that is down when the application
 * starts leaves its tools absent and says so loudly; it does not stop the application. A server that
 * goes down mid-call refuses that call with a sentence naming which server — a connection refused to
 * somebody else's machine must never read as this application's tool being broken.
 */
package org.jmouse.ai.mcp.client;
