package org.jmouse.ai.sandbox.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.conversation.SettingsProviderRegistry;
import org.jmouse.ai.management.ManagementRoutes;
import org.jmouse.ai.management.ProviderController;
import org.jmouse.ai.management.ToolCallHistoryController;
import org.jmouse.ai.management.ToolCatalogController;
import org.jmouse.ai.management.UnknownPublishedToolException;
import org.jmouse.ai.management.UsageController;
import org.jmouse.ai.provider.ProviderSettings;
import org.jmouse.ai.provider.ProviderSettingsSource;
import org.jmouse.ai.sandbox.SandboxCallers;
import org.jmouse.ai.sandbox.SandboxWorkshop;
import org.jmouse.ai.sandbox.Transcript;
import org.jmouse.ai.view.ProviderRegistry;
import org.jmouse.ai.view.ToolCatalogView;

import java.util.Map;

/**
 * The four management endpoints, answered.
 *
 * <p>The fourth entry point. It runs a handful of calls through the ordinary dispatcher so there is a
 * trail worth reading, then asks each controller what it would return and prints the JSON a client would
 * receive.
 *
 * <p><strong>No container, and that is deliberate rather than a shortcut.</strong> These controllers are
 * plain methods over four read ports, returning plain values — which is most of the claim the module
 * makes about itself. Standing a servlet container up would exercise Spring's request mapping and prove
 * nothing about them that is not visible here; what it would add is a Spring application inside a module
 * whose whole value is that it has no framework in it.
 *
 * <p>⚠️ <strong>The scenario worth reading twice is the last one.</strong> A key IS configured, and the
 * provider endpoint's JSON does not contain it — because the port it reads has no method that could
 * answer with one. That is the difference between a screen that is careful and a screen that cannot leak.
 */
public final class ManagementSandbox {

    /** ⚠️ A real-looking credential, on purpose: the point is that it does not appear in any output. */
    private static final String CONFIGURED_KEY = "sk-ant-sandbox-0123456789abcdef";

    private final Transcript      transcript = new Transcript();
    private final SandboxWorkshop workshop   = new SandboxWorkshop();
    private final ObjectMapper    json       = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final ToolCatalogView           tools;
    private final ToolCatalogController     catalogue;
    private final ToolCallHistoryController history;
    private final UsageController           usage;
    private final ProviderController        provider;

    public static void main(String[] arguments) {
        new ManagementSandbox().run();
    }

    private ManagementSandbox() {
        this.tools     = ToolCatalogView.over(workshop.catalog());
        this.catalogue = new ToolCatalogController(tools);
        this.history   = new ToolCallHistoryController(workshop.trace(), tools);
        this.usage     = new UsageController(workshop.trace());
        this.provider  = new ProviderController(new SettingsProviderRegistry(configuredProvider()));
    }

    private void run() {
        somethingToLookAt();
        whatCanBeDone();
        whatHasBeenCalled();
        howMuchHasBeenUsed();
        theKeyThatIsNotInTheAnswer();
        aNameThatIsNotPublished();
    }

    // ── The scenarios ────────────────────────────────────────────────────────────

    private void somethingToLookAt() {
        transcript.scenario("Something to look at",
                "Six calls through the ordinary dispatcher, deliberately mixed: two that work, one "
                + "refused for a permission, one for a scope, one preview, and one name that does not "
                + "exist. A management screen over a trail of nothing but successes is a screen nobody "
                + "would have needed.");

        ToolDispatcher dispatcher = workshop.dispatcher();

        workshop.callers().actAs(SandboxCallers.ASSISTANT_FOR_OWNER);
        call(dispatcher, "workshops_list", Map.of());
        call(dispatcher, "parts_list",     Map.of("shelf", "A"));
        call(dispatcher, "parts_restock",  Map.of("shelf", "A", "by", 1));
        call(dispatcher, "parts_explode",  Map.of());

        workshop.callers().actAs(SandboxCallers.PERSON);
        call(dispatcher, "parts_discard",  Map.of("scope", "Bench", "shelf", "A"));
        call(dispatcher, "parts_list",     Map.of("scope", "Garage"));
    }

    private void whatCanBeDone() {
        transcript.scenario("GET " + prefix() + "/tools/...",
                "The shape of the catalogue, and one action in full: what it costs, where it comes "
                + "from, whether it destroys anything, and the schema a client would validate against. "
                + "Every field is declared in the source of a tool and none of it is a secret - and "
                + "there is no field for a handler, because the port cannot reach one. The whole "
                + "listing is the same records, fourteen times over, and is left out for length.");

        show(prefix() + "/tools/summary", catalogue.summary());
        show(prefix() + "/tools/parts_discard", catalogue.tool("parts_discard"));
    }

    private void whatHasBeenCalled() {
        transcript.scenario("GET " + prefix() + "/calls",
                "The trail, newest first, and the same rows narrowed to one caller and one action. "
                + "Answered off the sandbox's own recording trace - which is the arrangement the read "
                + "ports exist for: a product stores its trail in whatever shape it already had, and "
                + "answers these three questions from it.");

        show(prefix() + "/calls?limit=4", history.calls(null, 4));
        show(prefix() + "/calls?caller=" + SandboxCallers.SECOND_OWNER,
                history.calls(SandboxCallers.SECOND_OWNER, 0));
        show(prefix() + "/tools/parts_list/calls", history.callsOf("parts_list", 0));
    }

    private void howMuchHasBeenUsed() {
        transcript.scenario("GET " + prefix() + "/usage",
                "Counted by caller, action AND outcome. The outcome stays in the grain rather than "
                + "being summed away, because a caller whose calls are mostly MISSING_PERMISSION is the "
                + "single most useful thing this screen reports - and a total per caller hides exactly "
                + "that.");

        show(prefix() + "/usage", usage.usage(null, null));
    }

    private void theKeyThatIsNotInTheAnswer() {
        transcript.scenario("GET " + prefix() + "/provider",
                "A key IS configured - '" + CONFIGURED_KEY + "'. Look for it below. It is not there, "
                + "and not because this screen remembered to leave it out: the port it reads has no "
                + "method that answers with a key, so there is nothing here to forget.");

        show(prefix() + "/provider", provider.provider().getBody());

        transcript.note("");
        transcript.note("...and the same fact stated for a person: "
                      + describeProvider());
    }

    private void aNameThatIsNotPublished() {
        transcript.scenario("A name that is not published",
                "The controller throws and does not render. What a client sees is the product's own "
                + "error body - because a library that answered with a document of its own would put a "
                + "second error format into an application that already has one, and the first person "
                + "to notice would be a client author reconciling them.");

        try {
            catalogue.tool("parts_explode");
            transcript.note("ANSWERED - which means the lookup stopped refusing.");

        } catch (UnknownPublishedToolException unknown) {
            transcript.note("THREW " + unknown.getClass().getSimpleName()
                          + " for '" + unknown.publishedName() + "'");
            transcript.note("  " + unknown.getMessage());
        }
    }

    // ── Driving one call, and printing one answer ────────────────────────────────

    private void call(ToolDispatcher dispatcher, String publishedName, Map<String, Object> arguments) {
        try {
            dispatcher.dispatch(publishedName, arguments);

        } catch (ToolRefusedException expected) {
            // Every refusal here is one the scenario asked for; what matters is the row it leaves.
        }
    }

    private void show(String request, Object answer) {
        transcript.note("");
        transcript.note("  > GET " + request);

        try {
            json.writeValueAsString(answer).lines().forEach(line -> transcript.note("    " + line));

        } catch (Exception unwritable) {
            transcript.note("    (could not be written as JSON: " + unwritable.getMessage() + ")");
        }
    }

    private String describeProvider() {
        return provider.provider().getBody() == null
                ? "no provider is configured"
                : provider.provider().getBody().describe();
    }

    private static String prefix() {
        // What ManagementRoutes.PREFIX resolves to where nothing overrides it. This driver has no
        // Environment to resolve the placeholder against, and printing the raw '${...}' would say less
        // than printing the address the endpoints are actually mounted at.
        return ManagementRoutes.DEFAULT_PREFIX;
    }

    /** A provider that is fully configured, key and all. */
    private static ProviderSettingsSource configuredProvider() {
        return ProviderSettingsSource.fixed(new ProviderSettings(
                "anthropic", "claude-sonnet-5", CONFIGURED_KEY, null, 4_096));
    }

}
