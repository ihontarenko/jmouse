package org.jmouse.ai.sandbox;

import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.spi.PermissionVocabulary;
import org.jmouse.ai.spi.ToolAuthorizer;

import java.util.Map;
import java.util.Set;

/**
 * A real permission set, with one caller deliberately missing one — and one permission deliberately
 * held only in one place.
 *
 * <p>Both gaps exist to exercise something the design argues about rather than to be realistic. The
 * missing {@code parts:delete} reaches the scope-free gate, which runs before any scope is resolved so
 * that a caller who may do nothing cannot enumerate the places by reading refusals. The place-bound
 * {@code parts:write} reaches {@link ToolAuthorizer#permitsInScope}, which exists because a product
 * whose permissions are held <em>at places</em> could otherwise only ever be asked the weaker question.
 *
 * <p>The vocabulary is the whole set, so the catalogue's second startup check has something to check
 * against — see {@code CatalogueRefusals} for what happens when an action misspells one.
 */
public final class SandboxPermissions implements ToolAuthorizer, PermissionVocabulary {

    public static final String WORKSHOPS_READ = "workshops:read";
    public static final String PARTS_READ     = "parts:read";
    public static final String PARTS_WRITE    = "parts:write";
    public static final String PARTS_DELETE   = "parts:delete";
    public static final String NOTES_READ     = "notes:read";
    public static final String NOTES_WRITE    = "notes:write";

    private static final Set<String> EVERY_PERMISSION = Set.of(
            WORKSHOPS_READ, PARTS_READ, PARTS_WRITE, PARTS_DELETE, NOTES_READ, NOTES_WRITE);

    /** ⚠️ The second owner holds no {@code parts:delete} anywhere. */
    private static final Map<String, Set<String>> HELD_ANYWHERE = Map.of(
            SandboxCallers.ASSISTANT,
            EVERY_PERMISSION,

            SandboxCallers.SECOND_OWNER,
            Set.of(WORKSHOPS_READ, PARTS_READ, PARTS_WRITE, NOTES_READ, NOTES_WRITE));

    /**
     * ⚠️ …and holds {@code parts:write} only in the garage.
     *
     * <p>A permission absent from this map is held everywhere it is held at all — which is what keeps
     * the common case free, exactly as a flat product should be.
     */
    private static final Map<String, Set<String>> WHERE_WRITE_IS_HELD = Map.of(
            SandboxCallers.SECOND_OWNER,
            Set.of(WorkshopInventory.GARAGE, WorkshopInventory.GARAGE_ANNEXE));

    @Override
    public Set<String> all() {
        return EVERY_PERMISSION;
    }

    @Override
    public boolean permits(CallerIdentity caller, ToolAction action) {
        return HELD_ANYWHERE
                .getOrDefault(caller.callerId(), Set.of())
                .contains(action.requiredPermission());
    }

    @Override
    public boolean permitsInScope(CallerIdentity caller, ToolAction action, InvocationScope scope) {
        if (!permits(caller, action)) {
            return false;
        }

        if (!PARTS_WRITE.equals(action.requiredPermission())) {
            return true;
        }

        Set<String> confinedTo = WHERE_WRITE_IS_HELD.get(caller.callerId());

        return confinedTo == null || confinedTo.contains(scope.id());
    }
}
