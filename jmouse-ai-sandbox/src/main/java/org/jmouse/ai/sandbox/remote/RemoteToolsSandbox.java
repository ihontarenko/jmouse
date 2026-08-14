package org.jmouse.ai.sandbox.remote;

import org.jmouse.ai.PublishedTool;
import org.jmouse.ai.ToolDefinition;
import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.ToolOutcome;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.mcp.McpToolServer;
import org.jmouse.ai.mcp.client.McpToolClient;
import org.jmouse.ai.mcp.client.RemoteToolException;
import org.jmouse.ai.mcp.client.RemoteToolServer;
import org.jmouse.ai.mcp.client.RemoteToolSettings;
import org.jmouse.ai.sandbox.SandboxPermissions;
import org.jmouse.ai.sandbox.SandboxWorkshop;
import org.jmouse.ai.sandbox.Transcript;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Two workshops, one catalogue, and a caller that cannot tell them apart.
 *
 * <p>The third entry point, and the one that shows why the whole arrangement was worth building. Every
 * scenario below is about the same claim from a different side: <strong>once the catalogue is the union
 * of local capability and remote capability, a remote action is an action.</strong> It passes the same
 * permission gate, is refused in the same sentences, is counted in the same trail, and is chosen by an
 * assistant on the same terms — and <em>"do not speak the protocol to reach yourself"</em> stops being
 * advice anybody has to remember, because a local tool is already in the catalogue and there is nothing
 * to connect to.
 *
 * <p>⚠️ The two workshops are genuinely separate instances with separate inventories, separate dispatchers
 * and separate guards. Nothing below reaches across except through the far server's own protocol handler.
 */
public final class RemoteToolsSandbox {

    /** The namespace the far server's tools land in here. Chosen by this installation, not by it. */
    private static final String DEPOT = "depot";

    /** What the far server is called in log lines, refusals and this transcript. */
    private static final String DEPOT_SERVER = "parts-depot";

    /** Its two destructive actions, which cannot be forwarded and are left out by name. */
    private static final String[] UNFORWARDABLE = {"parts_discard", "notes_delete"};

    private final Transcript      transcript = new Transcript();
    private final SandboxWorkshop depot      = new SandboxWorkshop();
    private final McpToolServer   depotServer;

    public static void main(String[] arguments) {
        new RemoteToolsSandbox().run();
    }

    private RemoteToolsSandbox() {
        this.depotServer = new McpToolServer(depot.dispatcher(), DEPOT_SERVER, "1.0.0");
    }

    private void run() {
        whatADestructiveRemoteToolCosts();
        oneCatalogueOfTwoOrigins();
        aCallerThatCannotTell();
        aRemoteRefusalIsLegibleAsOne();
        aServerThatWasNotThere();
        aServerThatWentAwayMidCall();
    }

    // ── The scenarios ────────────────────────────────────────────────────────────

    private void whatADestructiveRemoteToolCosts() {
        transcript.scenario("A destructive remote tool is refused at registration",
                "The far workshop deletes parts and notes. Confirmation works by showing somebody the "
                + "records a call would destroy - and that list lives on the other machine, where this "
                + "application cannot read it. Registered anyway it would preview nothing and then "
                + "destroy something, so registration refuses and says which tool and which server.");

        try {
            new McpToolClient(everything(), settings());
            transcript.note("REGISTERED - which is wrong, and means the refusal has stopped working.");

        } catch (IllegalStateException refused) {
            transcript.note("REFUSED AT REGISTRATION");
            transcript.note("  " + refused.getMessage());
        }
    }

    private void oneCatalogueOfTwoOrigins() {
        transcript.scenario("One catalogue, two origins",
                "The same workshop again, minus the two it cannot forward. Its actions arrive under a "
                + "namespace THIS installation chose, costing a permission THIS installation decided, "
                + "and are vetted by the same startup checks as everything local - so a remote name a "
                + "protocol would reject fails here rather than at a client's connect time.");

        SandboxWorkshop local = workshopReaching(McpToolClient.orAbsent(this::forwardable, settings()));

        transcript.note("Catalogue: " + local.catalog().size() + " action(s) across "
                      + local.catalog().toolNames().size() + " tool(s)");
        transcript.note("");
        transcript.note(String.format("    %-26s %-10s %-14s %s",
                "PUBLISHED NAME", "ORIGIN", "PERMISSION", "HINTS"));

        local.catalog().published().forEach(tool -> transcript.note(String.format("    %-26s %-10s %-14s %s",
                tool.publishedName(), tool.origin(), tool.requiredPermission(), hintsOf(tool))));
    }

    private void aCallerThatCannotTell() {
        transcript.scenario("A caller that cannot tell",
                "Two calls written identically, one landing in this process and one on the other "
                + "machine. Both are guarded, both are counted, both come back the same shape. What "
                + "differs is whose place answered: this dispatcher says 'not confined to one place', "
                + "because a scope HERE is not a scope THERE - and the far server's own echo travels "
                + "back inside the payload, naming the workshop IT chose.");

        SandboxWorkshop local      = workshopReaching(McpToolClient.orAbsent(this::forwardable, settings()));
        ToolDispatcher  dispatcher = local.dispatcher();

        attempt(dispatcher, "parts_list",       arguments("shelf", "A"));
        attempt(dispatcher, "depot_parts_list", arguments("shelf", "A"));

        transcript.note("");
        transcript.note("...and the local inventory is untouched by the second call: "
                      + local.inventory().partCount() + " part(s) here, "
                      + depot.inventory().partCount() + " part(s) there.");
    }

    private void aRemoteRefusalIsLegibleAsOne() {
        transcript.scenario("A remote refusal is legible as a remote refusal",
                "There is no part 'part-404' on the far machine, and its dispatcher refuses on its own "
                + "terms. What comes back keeps the far server's own sentence AND names the server, "
                + "because a REMOTE_REFUSED that read as this application's own refusal would send "
                + "whoever is on call to look at this installation's policy and find nothing wrong.");

        SandboxWorkshop local = workshopReaching(McpToolClient.orAbsent(this::forwardable, settings()));

        attempt(local.dispatcher(), "depot_parts_move", arguments("partId", "part-404", "toShelf", "C"));
    }

    private void aServerThatWasNotThere() {
        transcript.scenario("A server that was not there",
                "It must not stop the application. Its tools are absent, the absence is logged loudly, "
                + "and everything local still works - because the alternative is a product that will "
                + "not start because somebody else's machine is being restarted.");

        SandboxWorkshop local = workshopReaching(McpToolClient.orAbsent(this::unreachable, settings()));

        transcript.note("Catalogue: " + local.catalog().size() + " action(s) - "
                      + String.join(", ", local.catalog().publishedNames()));
        transcript.note("");
        transcript.note("...and a caller asking for one of the absent ones is told what does exist:");

        attempt(local.dispatcher(), "depot_parts_list", arguments());

        transcript.note("");
        transcript.note("...while everything local is unaffected:");

        attempt(local.dispatcher(), "workshops_list", arguments());
    }

    private void aServerThatWentAwayMidCall() {
        transcript.scenario("A server that went away mid-call",
                "Registered and reachable at startup, gone by the time it was called. This is a FAILURE "
                + "rather than a refusal, and the difference is not pedantry: every refusal here ends by "
                + "promising nothing was changed, and a call that vanished into a broken connection "
                + "cannot promise that - the request may have arrived and been carried out with the "
                + "answer lost on the way back.");

        LoopbackRemoteToolServer connection = forwardable();
        SandboxWorkshop          local      = workshopReaching(
                McpToolClient.orAbsent(() -> connection, settings()));

        connection.close();

        try {
            local.dispatcher().dispatch("depot_parts_list", arguments());
            transcript.note("ANSWERED - which means the disconnection was not noticed.");

        } catch (RemoteToolException gone) {
            transcript.note("FAILED (not refused)");
            transcript.note("  " + gone.getMessage());
        }
    }

    // ── The two workshops ────────────────────────────────────────────────────────

    private RemoteToolSettings settings() {
        // One permission covering the whole server - the simplest honest default - with the listing
        // costing less than the writing, which is what per-tool overrides are for.
        return RemoteToolSettings.of(DEPOT_SERVER, DEPOT, SandboxPermissions.PARTS_WRITE)
                .costing("parts_list",     SandboxPermissions.PARTS_READ)
                .costing("workshops_list", SandboxPermissions.WORKSHOPS_READ)
                .costing("notes_list",     SandboxPermissions.NOTES_READ);
    }

    private LoopbackRemoteToolServer everything() {
        return new LoopbackRemoteToolServer(DEPOT_SERVER, depotServer);
    }

    private LoopbackRemoteToolServer forwardable() {
        return everything().withoutTools(UNFORWARDABLE);
    }

    private RemoteToolServer unreachable() {
        throw new RemoteToolException(DEPOT_SERVER,
                "'" + DEPOT_SERVER + "' could not be reached: ConnectException (Connection refused). "
                + "That is a server this application connects to, not a fault in this application's own "
                + "tools - everything local is unaffected.");
    }

    private SandboxWorkshop workshopReaching(ToolDefinition remote) {
        return new SandboxWorkshop(List.of(remote));
    }

    // ── Driving one call ─────────────────────────────────────────────────────────

    private void attempt(
            ToolDispatcher through, String publishedName, Map<String, Object> arguments) {

        transcript.calling("assistant-1 for owner-1", publishedName, arguments);

        try {
            ToolOutcome outcome = through.dispatch(publishedName, arguments);
            transcript.ran(outcome);

        } catch (ToolRefusedException refusal) {
            transcript.refused(refusal);
        }
    }

    private static Map<String, Object> arguments(Object... pairs) {
        Map<String, Object> arguments = new LinkedHashMap<>();

        for (int index = 0; index < pairs.length; index += 2) {
            arguments.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }

        return arguments;
    }

    private static String hintsOf(PublishedTool tool) {
        if (tool.destructive()) {
            return "destructive";
        }

        return tool.readOnly() ? "read-only" : "writes";
    }
}
