package org.jmouse.ai.sandbox;

import org.jmouse.ai.ToolCatalog;
import org.jmouse.ai.ToolDefinition;
import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.guard.CeilingGuard;
import org.jmouse.ai.guard.ConfirmationGuard;
import org.jmouse.ai.guard.DeduplicationGuard;
import org.jmouse.ai.guard.EmptyDestructionGuard;
import org.jmouse.ai.guard.GuardChain;
import org.jmouse.ai.guard.GuardSettings;
import org.jmouse.ai.guard.InMemoryConfirmationStore;
import org.jmouse.ai.guard.InMemoryDuplicateCallStore;
import org.jmouse.ai.guard.RateLimitGuard;
import org.jmouse.ai.guard.TokenBucketCallerRateLimiter;
import org.jmouse.ai.spi.CallerRateLimiter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * One workshop, one catalogue, and however many dispatchers are wanted over them.
 *
 * <p>Extracted the moment there was a second entry point, which is exactly what it is for: an
 * assistant driving these tools through a conversation and a client calling them from outside the
 * process must reach <strong>the same actions through the same guards under the same caller</strong>,
 * or the comparison proves nothing. Two entry points each building their own would be two worlds that
 * happen to look alike.
 *
 * <p>⚠️ Mutable, and shared on purpose. A tool call made through one entry point is visible to the
 * next through another — which is the whole point of comparing them.
 */
public final class SandboxWorkshop {

    /**
     * Small enough that both are reachable with a dozen records, and far enough apart to keep them
     * distinguishable: five parts on one shelf need confirming, eight in one workshop are refused.
     */
    public static final GuardSettings SETTINGS = GuardSettings.defaults()
            .withPerCallCeiling(6)
            .withConfirmationThreshold(3);

    private final WorkshopInventory  inventory   = new WorkshopInventory();
    private final SandboxCallers     callers     = new SandboxCallers();
    private final SandboxPermissions permissions = new SandboxPermissions();
    private final RecordingTrace     trace       = new RecordingTrace();
    private final ToolCatalog        catalog;

    public SandboxWorkshop() {
        this(List.of());
    }

    /**
     * The same workshop, plus whatever else this installation can reach.
     *
     * <p>Written this way rather than with a mutable list because it is the arrangement a real product
     * has: <strong>the catalogue is the union of what this application can do and what it has connected
     * to</strong>, vetted once as a whole set. Contributing a remote server's tools is contributing
     * definitions, and nothing here needs to know that some of them are somewhere else.
     */
    public SandboxWorkshop(List<ToolDefinition> alsoReachable) {
        List<ToolDefinition> definitions = new ArrayList<>(List.of(
                new WorkshopTool(inventory),
                new PartTool(inventory),
                new PartMovementTool(inventory),
                new NoteTool(inventory)));

        definitions.addAll(alsoReachable);

        this.catalog = ToolCatalog.of(definitions, permissions);
    }

    /** The ordinary one: five guards, generous bucket, in-memory stores. */
    public ToolDispatcher dispatcher() {
        return dispatcherWith(new TokenBucketCallerRateLimiter(60, Duration.ofMinutes(1)));
    }

    /**
     * The same catalogue behind a differently wired chain.
     *
     * <p>⚠️ Each dispatcher gets its <strong>own</strong> confirmation and duplicate stores, which is
     * the honest arrangement rather than a shortcut: two processes serving one product have exactly
     * this problem, and it is why {@code JpaConfirmationStore} exists.
     */
    public ToolDispatcher dispatcherWith(CallerRateLimiter rateLimiter) {
        GuardChain guards = new GuardChain(List.of(
                new RateLimitGuard(rateLimiter),
                new CeilingGuard(SETTINGS),
                new EmptyDestructionGuard(),
                new ConfirmationGuard(
                        new InMemoryConfirmationStore(SETTINGS.confirmationLifetime()), SETTINGS),
                new DeduplicationGuard(
                        new InMemoryDuplicateCallStore(SETTINGS.deduplicationWindow()))));

        return new ToolDispatcher(
                catalog, callers, permissions, new SandboxScopeResolver(inventory), guards, trace);
    }

    public WorkshopInventory inventory() {
        return inventory;
    }

    public SandboxCallers callers() {
        return callers;
    }

    public SandboxPermissions permissions() {
        return permissions;
    }

    public RecordingTrace trace() {
        return trace;
    }

    public ToolCatalog catalog() {
        return catalog;
    }
}
