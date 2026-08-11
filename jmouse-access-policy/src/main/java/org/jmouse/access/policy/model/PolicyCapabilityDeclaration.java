package org.jmouse.access.policy.model;

import java.util.List;

/**
 * A capability the file declares — one thing a grant can be about, written down.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * capabilities {
 *     paid {
 *         parametric-search
 *         custody
 *     }
 *
 *     limit  seat          "Seats"            per organization
 *     quota  storage-byte  "Storage written"  per organization, space
 * }
 * </pre>
 *
 * <p>produces, in order:
 *
 * <pre>
 *   new PolicyCapabilityDeclaration("parametric-search", null,              "gate",  List.of(),  true,  …)
 *   new PolicyCapabilityDeclaration("custody",           null,              "gate",  List.of(),  true,  …)
 *   new PolicyCapabilityDeclaration("seat",         "Seats",           "limit", List.of("organization"), true, …)
 *   new PolicyCapabilityDeclaration("storage-byte", "Storage written", "quota", List.of("organization","space"), true, …)
 * </pre>
 *
 * <p>⚠️ Everything here is <strong>text</strong>, like every other record in this package. Whether
 * {@code organization} is a registered scope and whether {@code seat} is a capability the code knows
 * are questions for binding, which has the catalogues. A parser reads characters.
 *
 * <p>⚠️ <strong>The {@code paid { }} sub-block is not a fourth kind.</strong> It lists keys that are
 * closed until granted and says nothing about their shape — a paid capability with no number is a
 * gate, and one listed in {@code limit} is a limit whether or not it also appears there. Modelling it
 * as a kind would make {@code paid} and {@code limit} mutually exclusive, which they are not.
 *
 * @param key         what a grant names it by
 * @param displayName what a screen shows, or null where the file gave none
 * @param kind        {@code gate}, {@code limit} or {@code quota}, as written — mapped onto
 *                    {@link org.jmouse.access.CapabilityKind} at binding
 * @param scopes      the scope names after {@code per}, in the order written, or empty for "anywhere"
 * @param paid        whether it is closed until something grants it
 */
public record PolicyCapabilityDeclaration(
        String       key,
        String       displayName,
        String       kind,
        List<String> scopes,
        boolean      paid,
        SourceSpan   at
) {

    public PolicyCapabilityDeclaration {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }
}
