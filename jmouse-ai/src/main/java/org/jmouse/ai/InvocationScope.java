package org.jmouse.ai;

import java.util.Objects;

/**
 * Where a call ran, and how that was chosen.
 *
 * <p><strong>{@code defaulted} travels into the response, and that is not decoration.</strong>
 * <em>"Which workspace did that go into"</em> is the question whose wrong answer surfaces a week later
 * in the inventory rather than immediately in the transcript. Saying whether the caller named the
 * scope or a default supplied it is what makes the echo worth reading — an echo that always says the
 * same thing is skipped, and one that says <em>"your default workspace"</em> is the sentence somebody
 * stops at.
 *
 * <p>{@code kind} is what lets one mechanism serve products that scope by different things. A
 * workspace, a persona, a project and a tenant are the same idea wearing four names, and carrying the
 * name rather than assuming one is the difference between a library and a product's class that
 * happens to compile elsewhere. It is also what the refusals read: <em>"there is no single default
 * persona"</em> is a sentence a model can act on, where <em>"there is no single default scope"</em> is
 * not.
 *
 * <p>⚠️ <strong>A scope is optional, including for a write.</strong> An action whose records belong to
 * an account rather than to a place is not scope-confined, so every path that touches an
 * {@code InvocationScope} has to survive it being {@code null} — the guards, the confirmation
 * fingerprint and every refusal sentence each carry a scopeless half for exactly this.
 *
 * @param kind      what sort of place this is, in the product's own word: {@code workspace},
 *                  {@code persona}, {@code project}
 * @param id        the identifier the product knows it by
 * @param name      what a person calls it — what a model saw in conversation, and what a refusal quotes
 * @param defaulted whether the caller named it, or a default supplied it
 */
public record InvocationScope(String kind, String id, String name, boolean defaulted) {

    public InvocationScope {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
    }

    /** The scope the caller asked for by name. */
    public static InvocationScope named(String kind, String id, String name) {
        return new InvocationScope(kind, id, name, false);
    }

    /** The one the caller can see, standing in for an argument nobody sent. */
    public static InvocationScope defaulted(String kind, String id, String name) {
        return new InvocationScope(kind, id, name, true);
    }

    /**
     * How the echo reads in a response somebody will skim.
     *
     * <p>The parenthetical is the whole value of the field: a reader who wanted a different workspace
     * only notices when the sentence admits that nobody chose this one.
     */
    public String echo() {
        return defaulted
                ? label() + " (your default " + kind + ")"
                : label();
    }

    /** The name, falling back to the identifier for a scope whose product does not name them. */
    public String label() {
        return name == null || name.isBlank() ? id : name;
    }

    /** The identifier of a scope that may not be there, for a call that is not confined to one. */
    public static String identifierOf(InvocationScope scope) {
        return scope == null ? null : scope.id();
    }

    /** The label of a scope that may not be there, for a refusal that has to name it or not. */
    public static String labelOf(InvocationScope scope) {
        return scope == null ? null : scope.label();
    }
}
