package org.jmouse.ai.spring;

import org.jmouse.ai.ToolCatalog;
import org.jmouse.ai.guard.GuardChain;
import org.jmouse.ai.spi.CallerResolver;
import org.jmouse.ai.spi.ConfirmationStore;
import org.jmouse.ai.spi.InvocationTrace;
import org.jmouse.ai.spi.ToolAuthorizer;
import org.jmouse.ai.view.ProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Says out loud, once, what actually came up.
 *
 * <p><strong>Not decoration.</strong> Boot 4 split autoconfiguration into per-technology modules, and
 * the failure modes of a missing piece differ dangerously: a missing {@code spring-boot-restclient}
 * fails loudly at startup, while a missing {@code spring-boot-flyway} is <em>silent</em> and the
 * migrations simply never run. Every optional module here has the same shape of problem — a product that
 * believes it has a rate limit, a trail, an access engine or a persistent confirmation store, and does
 * not, gets a running application whose protection is quietly absent. One line at startup turns "did
 * that wire?" from a debugging session into a look at the log.
 *
 * <p>So the line names each seam by <em>which implementation is in force</em> rather than by whether one
 * exists. "Authorizer: AccessToolAuthorizer" and "authorizer: PermitEverything" are the two answers that
 * matter, and only the first of them is a product with authorization.
 *
 * <p>Migrations are the one thing deliberately absent from this line: {@code AiFlywayMigrator} logs its
 * own, naming the location, the history table and how many ran — which says more than a boolean here
 * could, and says it at the moment it happens.
 *
 * <p>⚠️ <strong>And a second line, at warning level, when the starting defaults are still in
 * place.</strong> Those exist so an unconfigured application can be tried out; an application that
 * reaches production still holding them has an assistant nobody authenticates, nobody authorizes and
 * nothing records. Discovering that from a bill, or from a caller doing something it should not have
 * been able to, is the outcome this line exists to prevent.
 */
public class AiDiagnostics {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiDiagnostics.class);

    /**
     * Reports on construction, rather than behind a lifecycle annotation, so that the report needs no
     * annotation library on the classpath and cannot be skipped by a context that handles lifecycle
     * callbacks differently.
     */
    public AiDiagnostics(
            ToolCatalog       catalog,
            GuardChain        guards,
            CallerResolver    callerResolver,
            ToolAuthorizer    authorizer,
            ConfirmationStore confirmations,
            InvocationTrace   trace,
            ProviderRegistry  providers,
            AiProperties      properties) {

        LOGGER.info("jMouse AI ready — {} action(s) across {} tool(s), guards [{}], ceiling {}, "
                  + "confirm above {}, callers {}, authorizer {}, previews {}, trace {}, "
                  + "provider {}, protocol {}, screens {}",
                catalog.size(),
                catalog.toolNames().size(),
                String.join(", ", new TreeSet<>(guards.names())),
                properties.getGuards().getPerCallCeiling(),
                properties.getGuards().getConfirmationThreshold(),
                nameOf(callerResolver),
                nameOf(authorizer),
                nameOf(confirmations),
                nameOf(trace),
                describeProvider(providers),
                properties.getProtocol().isEnabled()
                        ? "served at " + properties.getProtocol().getEndpoint()
                        : "off",
                properties.getManagement().isEnabled()
                        ? "mounted at " + properties.getManagement().getPrefix()
                        : "off");

        warnAboutStartingDefaults(callerResolver, authorizer, trace);
    }

    private void warnAboutStartingDefaults(
            CallerResolver callerResolver, ToolAuthorizer authorizer, InvocationTrace trace) {

        List<String> stillDefault = new ArrayList<>();

        if (callerResolver instanceof PermissiveDefaults.AnonymousCallers) {
            stillDefault.add("no CallerResolver — every call runs as the same anonymous caller");
        }

        if (authorizer instanceof PermissiveDefaults.PermitEverything) {
            stillDefault.add("no ToolAuthorizer — every action's declared permission is ignored and "
                           + "every call is permitted");
        }

        if (trace instanceof PermissiveDefaults.NoTrace) {
            stillDefault.add("no InvocationTrace — nothing that happens is written down anywhere");
        }

        if (stillDefault.isEmpty()) {
            return;
        }

        LOGGER.warn("⚠️ jMouse AI is running on its starting defaults: {}. That is deliberate, so an "
                  + "application with one tool definition and no configuration can be tried out — and it "
                  + "is not something to reach production with. A tool handler calls a domain service "
                  + "directly, past whatever authorization is hung on the HTTP layer, so these beans are "
                  + "the only ones standing in front of every action in the catalogue.",
                String.join("; ", stillDefault));
    }

    /**
     * Which implementation is in force, by name.
     *
     * <p>The class name rather than a hand-maintained description, because the useful thing about this
     * line is that it names something a reader can then go and open — and a description would have to be
     * kept in step with a bean nobody here knows about.
     */
    private static String nameOf(Object seam) {
        return seam.getClass().getSimpleName();
    }

    private static String describeProvider(ProviderRegistry providers) {
        return providers.active().map(ProviderRegistry.ActiveProvider::describe).orElse("none configured");
    }
}
