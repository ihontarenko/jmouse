package org.jmouse.ai.sandbox;

import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.RefusalReason;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.spi.ScopeResolver;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns the workshop a caller named into the scope an action runs in.
 *
 * <p>Follows both of the rules {@link ScopeResolver}'s javadoc asks for, and the sandbox is arranged so
 * that all four outcomes are reachable: the assistant sees one workshop and so has a default, and the
 * second owner sees three of which two share a name, so naming nothing is undetermined and naming
 * "Garage" is ambiguous.
 *
 * <p>⚠️ Resolves against the <strong>caller</strong> rather than the acting subject. A service
 * credential is confined to a subset of its owner's places, and resolving against the owner would let
 * an identifier from outside that subset resolve straight through the boundary the whole arrangement
 * is scoped by.
 */
public final class SandboxScopeResolver implements ScopeResolver {

    private final WorkshopInventory inventory;

    public SandboxScopeResolver(WorkshopInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public InvocationScope resolve(CallerIdentity caller, ToolAction action, String requestedScope) {
        List<Workshop> visible = inventory.workshopsVisibleTo(caller.callerId());

        if (requestedScope == null || requestedScope.isBlank()) {
            return applyDefault(visible);
        }

        return matchByName(visible, requestedScope.trim());
    }

    private InvocationScope matchByName(List<Workshop> visible, String requestedName) {
        List<Workshop> matches = visible.stream()
                .filter(workshop -> workshop.name().equalsIgnoreCase(requestedName))
                .toList();

        if (matches.size() == 1) {
            Workshop matched = matches.getFirst();
            return InvocationScope.named(Workshop.KIND, matched.id(), matched.name());
        }

        if (matches.isEmpty()) {
            throw new ToolRefusedException(RefusalReason.UNKNOWN_SCOPE,
                    "This caller cannot see a workshop called '" + requestedName + "'. " + describe(visible));
        }

        // Two of one name, both visible. Picking either silently is how work lands in the wrong place,
        // and the discovery is neither immediate nor pleasant.
        throw new ToolRefusedException(RefusalReason.AMBIGUOUS_SCOPE,
                "'" + requestedName + "' matches " + matches.size() + " workshops this caller can see: "
                + matches.stream().map(workshop -> workshop.name() + " (" + workshop.id() + ")")
                         .collect(Collectors.joining(", "))
                + ". Rename one of them, or ask which was meant — this will not be resolved by guessing.");
    }

    private InvocationScope applyDefault(List<Workshop> visible) {
        if (visible.size() == 1) {
            Workshop only = visible.getFirst();
            return InvocationScope.defaulted(Workshop.KIND, only.id(), only.name());
        }

        if (visible.isEmpty()) {
            throw new ToolRefusedException(RefusalReason.UNDETERMINED_SCOPE,
                    "This caller has no workshop to act in, so there is nothing to name.");
        }

        throw new ToolRefusedException(RefusalReason.UNDETERMINED_SCOPE,
                "This caller can see " + visible.size() + " workshops, so there is no single default to "
                + "fall back on. Name one with the 'scope' argument: "
                + visible.stream().map(Workshop::name).collect(Collectors.joining(", ")) + ".");
    }

    private String describe(List<Workshop> visible) {
        return visible.isEmpty()
                ? "It cannot see any workshop at all."
                : "It can see: " + visible.stream().map(Workshop::name).collect(Collectors.joining(", "))
                  + ".";
    }
}
