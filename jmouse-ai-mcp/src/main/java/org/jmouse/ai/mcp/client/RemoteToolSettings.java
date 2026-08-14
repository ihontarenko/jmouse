package org.jmouse.ai.mcp.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the product decides about a server it connected to, because the library cannot.
 *
 * <p>Two of these three are the whole reason this type exists.
 *
 * <p><strong>The namespace.</strong> A remote server's tools arrive under a name this installation
 * chooses, not one the server chose. Two servers offering a {@code search} tool must not collide, and a
 * caller reading {@code partsCatalogue.search} learns something a caller reading {@code search} does not.
 *
 * <p><strong>The permission.</strong> ⚠️ A remote server's own idea of what its tools are worth is not
 * this installation's idea, and the catalogue refuses an action with no permission — correctly, because
 * a handler reaches past every HTTP-layer check a product has. So somebody has to say what a remote tool
 * costs here, and the honest simple default is <em>one permission covering the whole server</em>, with
 * per-tool overrides where a server offers both a listing and a rewrite and they should not cost the
 * same.
 *
 * @param serverName          how the server is named to a person; also what a failure message says
 * @param toolName            the local namespace its tools land in
 * @param permission          what every tool from this server costs unless overridden
 * @param permissionOverrides per remote tool name, for the ones that should cost something else
 */
public record RemoteToolSettings(
        String              serverName,
        String              toolName,
        String              permission,
        Map<String, String> permissionOverrides
) {

    public RemoteToolSettings {
        permissionOverrides = permissionOverrides == null ? Map.of() : Map.copyOf(permissionOverrides);
    }

    /** One permission covering everything the server offers — the simplest honest arrangement. */
    public static RemoteToolSettings of(String serverName, String toolName, String permission) {
        return new RemoteToolSettings(serverName, toolName, permission, Map.of());
    }

    /** The same, with one tool costing something else. Chainable. */
    public RemoteToolSettings costing(String remoteToolName, String permission) {
        Map<String, String> extended = new LinkedHashMap<>(permissionOverrides);
        extended.put(remoteToolName, permission);

        return new RemoteToolSettings(serverName, toolName, this.permission, extended);
    }

    /** What one remote tool costs here. */
    public String permissionFor(String remoteToolName) {
        return permissionOverrides.getOrDefault(remoteToolName, permission);
    }
}
