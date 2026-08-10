package org.jmouse.access.enforcement;

import org.jmouse.access.ScopeKind;

/**
 * What one call needs, after {@code @RequiresAccess} on the method and on its class have been merged.
 *
 * <p>The scope arrives here already <strong>resolved</strong>: the annotation carries a name, and
 * {@link AccessRequirements} looks it up in the catalogue as it reads the declaration. So everything
 * downstream works with a {@link ScopeKind} and a typo has already failed by the time anything asks a
 * question with it.
 *
 * @param permission what the caller must hold, or blank where the endpoint gates on a module alone
 * @param module     the feature module, or blank where it belongs to none
 * @param scope      how far the endpoint reaches, which decides what the target must name
 * @param resource   the kind of row it acts on, or {@code void.class} where it acts on none
 * @param resourceId which parameter carries that row's identifier, or blank for the sole identifier
 */
public record AccessRequirement(
        String    permission,
        String    module,
        ScopeKind scope,
        Class<?>  resource,
        String    resourceId
) {

    public boolean namesAPermission() {
        return !permission.isBlank();
    }

    public boolean namesAModule() {
        return !module.isBlank();
    }

    /** Whether the target comes from a row rather than from the request. */
    public boolean namesARow() {
        return resource != void.class;
    }

    /** What a startup message and a shadow-mode warning call this. */
    public String describe() {
        StringBuilder description = new StringBuilder();

        if (namesAPermission()) {
            description.append(permission).append(' ');
        }
        description.append('@').append(scope.name());
        if (namesAModule()) {
            description.append(" module=").append(module);
        }
        if (namesARow()) {
            description.append(" about=").append(resource.getSimpleName());
        }

        return description.toString();
    }
}
