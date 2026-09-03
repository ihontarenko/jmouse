package org.jmouse.access.policy;

import org.jmouse.access.policy.model.PolicyPermissionDeclaration;
import org.jmouse.access.spi.AccessTargetRegistry;
import org.jmouse.access.spi.PermissionRelations;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code through} clauses a policy declared, resolved to types — stage 2 of the two the language has.
 *
 * <p>Stage 1 parses text and knows no classes, so it keeps the resource as the word somebody wrote.
 * This is where the word becomes a type, and where a word that names nothing <strong>fails the
 * boot</strong> rather than producing a rule that quietly refuses every request it is asked about.
 *
 * <p>⚠️ <strong>A wrong `through` is the one mistake in this language that permits too much.</strong>
 * Everything else here narrows — a condition never grants, a deny always wins — so a typo elsewhere
 * costs somebody access they should have had, and somebody says so within the hour. A rule pointed at
 * the wrong resource is silent in exactly the opposite direction. That asymmetry is why this is checked
 * at startup with the vocabulary printed, rather than validated lazily on first use.
 */
public final class DeclaredPermissionRelations implements PermissionRelations {

    private final Map<String, Redirect> byPermission;

    private DeclaredPermissionRelations(Map<String, Redirect> byPermission) {
        this.byPermission = byPermission;
    }

    /**
     * Reads the declarations, resolving each {@code through} against the resource vocabulary.
     *
     * @param declarations the permissions the policy states, {@code through} clauses included
     * @param targets      the resource vocabulary — what {@code @AccessResourceName} put in it
     * @throws IllegalStateException where a clause names a resource nothing claims
     */
    public static PermissionRelations of(
            Collection<PolicyPermissionDeclaration> declarations, AccessTargetRegistry targets) {

        Map<String, Redirect> byPermission = new LinkedHashMap<>();

        for (PolicyPermissionDeclaration declaration : declarations) {
            if (declaration.through() == null) {
                continue;
            }

            String   resource = declaration.through().resource();
            Class<?> type     = targets.typeNamed(resource).orElseThrow(() -> new IllegalStateException(
                    "'" + declaration.name() + "' is declared 'through " + declaration.through().quantifier().word()
                    + " " + resource + "', but no resource is called '" + resource + "'. The vocabulary is "
                    + targets.knownTypes() + " — a name comes from @AccessResourceName on a type, or from "
                    + "its AccessTargetResolver where the type belongs to a library. Note that this is not "
                    + "a permission namespace: 'form:read' being declared says nothing about whether a "
                    + "resource called 'form' exists."));

            byPermission.put(declaration.name(), new Redirect(type, declaration.through().quantifier()));
        }

        return byPermission.isEmpty()
                ? PermissionRelations.none()
                : new DeclaredPermissionRelations(byPermission);
    }

    @Override
    public Optional<Redirect> redirectFor(String permission) {
        return Optional.ofNullable(byPermission.get(permission));
    }
}
