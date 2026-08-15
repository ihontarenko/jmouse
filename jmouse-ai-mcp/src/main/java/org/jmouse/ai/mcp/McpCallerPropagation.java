package org.jmouse.ai.mcp;

import io.micrometer.context.ContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeping the authenticated caller attached to a tool call that changes threads.
 *
 * <h2>The failure this exists to prevent</h2>
 *
 * <p><strong>Every tool call is refused as {@code NO_CALLER} while the credential is perfectly valid, and
 * nothing anywhere says why.</strong> A product authenticates a protocol request the ordinary way — a
 * servlet filter chain leaves an {@code Authentication} in a {@link ThreadLocal} — and the MCP servlet
 * transport then answers that request through Reactor. The handler, the dispatcher and the product's
 * {@code CallerResolver} all run on a different thread, where that {@link ThreadLocal} is empty. The
 * request is authenticated and the caller is absent <em>at the same time</em>, so the symptom is a polite
 * refusal rather than a {@code 401} or a stack trace — and every instinct points at the token instead.
 *
 * <p>Reactor closes exactly that gap. With automatic context propagation on, thread-locals are captured
 * where a chain is subscribed — still on the servlet thread, caller present — and restored around every
 * operator, wherever it runs. Whoever owns the {@link ThreadLocal} contributes an accessor saying how:
 * Spring Security registers {@code SecurityContextHolderThreadLocalAccessor} into the
 * {@link ContextRegistry} through a {@code ServiceLoader} file, without being asked and without this
 * module ever naming Spring.
 *
 * <h2>⚠️ Why the library and not each product</h2>
 *
 * <p>Nothing about it is specific to one product's tools, identity model or security configuration. It is
 * a property of <em>serving the catalogue over this transport</em>, which is this module's decision to
 * make and this module's consequence to carry. Left to the products it is the same three lines written
 * once per product, discovered once per product — and because the failure reads as an expired token,
 * discovered slowly every time.
 *
 * <p>⚠️ <strong>{@code io.micrometer:context-propagation} is a hard dependency and must stay one.</strong>
 * {@code reactor-core} declares it optional, and {@link Hooks#enableAutomaticContextPropagation()} does
 * <em>nothing at all</em> without it — no warning, no failure, just the refusal coming back.
 *
 * <p>⚠️ <strong>The hook is global to the process</strong>, which is as narrow as Reactor allows. It is
 * switched on when a server is actually built rather than when this class is loaded, so a product that
 * merely has the module on its classpath — to <em>connect</em> to somebody else's server, say — leaves
 * Reactor exactly as it found it.
 */
final class McpCallerPropagation {

    private static final Logger        LOGGER           =
            LoggerFactory.getLogger(McpCallerPropagation.class);
    private static final AtomicBoolean ENABLED          = new AtomicBoolean();
    private static final String        CONTEXT_REGISTRY = "io.micrometer.context.ContextRegistry";

    private McpCallerPropagation() {
    }

    /**
     * Switches propagation on, once per process.
     *
     * <p>⚠️ Called before a server is handed to a transport, and that timing is the point: capture
     * happens at subscription, so a hook switched on any later would leave the calls before it — and only
     * those — without a caller, which is the hardest version of this bug to believe.
     */
    static void enable() {
        requirePropagationLibrary();

        Hooks.enableAutomaticContextPropagation();

        if (ENABLED.compareAndSet(false, true)) {
            LOGGER.info("Tool calls carry their caller across threads: {} thread-local accessor(s) known",
                    ContextRegistry.getInstance().getThreadLocalAccessors().size());
        }
    }

    /**
     * Refuses to serve the catalogue when the library that makes propagation work is not there.
     *
     * <p><strong>⚠️ Failing here is the point, and it is a choice.</strong> Carrying on would produce a
     * server that starts cleanly, announces its tools, accepts a valid credential — and then refuses
     * every single call for having no caller. That is the failure this whole class exists to end, so
     * trading it for a startup error is the trade worth making: one of them names its own cause at the
     * moment somebody is watching, and the other sends people to look at their token.
     *
     * <p>This module declares {@code io.micrometer:context-propagation} as a hard dependency, so a real
     * build always has it. In practice the class is missing for one reason: a development environment
     * running against a classpath built before that dependency existed — an IDE that has not reimported
     * the project. Hence the message, which is aimed at exactly that person.
     */
    private static void requirePropagationLibrary() {
        // ⚠️ Named as a string on purpose. Writing ContextRegistry.class here would resolve the very
        // class being tested for, so the check would throw the error it exists to replace.
        try {
            Class.forName(CONTEXT_REGISTRY, false, McpCallerPropagation.class.getClassLoader());
        } catch (ClassNotFoundException notOnTheClasspath) {
            throw new IllegalStateException("""
                    Refusing to serve the Model Context Protocol: io.micrometer:context-propagation is \
                    not on the runtime classpath. This module declares it, so the usual cause is a stale \
                    classpath — reimport/reload the Maven project in your IDE and start again. \
                    Without it an authenticated caller cannot follow a tool call onto the thread it runs \
                    on, and every call would be refused as NO_CALLER while the credential is perfectly \
                    valid. See McpCallerPropagation for why that is worth failing over.""",
                    notOnTheClasspath);
        }
    }
}
