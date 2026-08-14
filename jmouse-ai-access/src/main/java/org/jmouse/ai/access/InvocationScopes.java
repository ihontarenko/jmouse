package org.jmouse.ai.access;

import org.jmouse.access.AccessTarget;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeReference;
import org.jmouse.ai.InvocationScope;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A scope, said in both vocabularies.
 *
 * <p>Two mechanisms arrived at the same idea from different directions. The access engine calls a place
 * a {@link ScopeReference} and knows how places nest; a tool invocation calls it an
 * {@link InvocationScope} and knows what a caller named it and whether a default supplied it. They are
 * the same place, and mapping them is what stops a product growing a <strong>second scope
 * vocabulary</strong> beside the one it already has — two lists of places that agree until one of them
 * gains a floor.
 *
 * <p>The kind travels as a string on the tool side, deliberately: {@code jmouse-ai} must work for a
 * product that has no scope model at all. Here it is resolved through {@link ScopeCatalog#byName}, and
 * a name the catalogue does not hold is a <strong>wiring mistake, refused loudly</strong> — a tool
 * declaring a scope kind the engine has never heard of would otherwise be authorized against the
 * installation instead of against the place it named, which is the permissive direction.
 */
public final class InvocationScopes {

    private final ScopeCatalog scopes;

    public InvocationScopes(ScopeCatalog scopes) {
        this.scopes = scopes;
    }

    /**
     * The place a tool call is running in, as the engine names it.
     *
     * @return empty for an action that is not confined to a scope — which is legal, including for a
     *         write, and means the call is aimed at no place rather than at an unknown one
     */
    public Optional<ScopeReference> referenceOf(InvocationScope scope) {
        return scope == null
                ? Optional.empty()
                : Optional.of(ScopeReference.of(kindOf(scope.kind()), scope.id()));
    }

    /**
     * What one tool call is aimed at.
     *
     * <p>⚠️ Carries the place and <strong>nothing else</strong>. The owner is deliberately left unset:
     * a tool handler decides for itself whose rows it is about, and a target that guessed an owner here
     * would answer <em>"is this mine"</em> on behalf of code that has not been reached yet. An unscoped
     * action is aimed at the installation, which is a real answer rather than a missing one.
     */
    public AccessTarget targetOf(InvocationScope scope) {
        return referenceOf(scope)
                .map(place -> AccessTarget.installation().at(place.type(), place.id()))
                .orElseGet(AccessTarget::installation);
    }

    /**
     * The other direction: a place the engine knows, as a scope a tool call can run in.
     *
     * <p>Marked as named rather than defaulted, because a reference that came from the engine was
     * arrived at by a rule rather than fallen back to — and the whole value of the {@code defaulted}
     * flag is that a caller who wanted somewhere else notices.
     *
     * @param label what a person calls the place; the reference carries an identifier and no name
     */
    public InvocationScope invocationScopeOf(ScopeReference reference, String label) {
        return InvocationScope.named(reference.type().name(), reference.id(), label);
    }

    private ScopeKind kindOf(String name) {
        return scopes.byName(name).orElseThrow(() -> new IllegalStateException(
                "A tool declares its scope as '" + name + "', which is not a scope this installation "
                + "has. The access vocabulary holds " + describeKinds() + ". A tool authorized against "
                + "a place nobody knows about would be authorized against the installation instead, "
                + "which is the permissive direction — so this refuses rather than falls back."));
    }

    private String describeKinds() {
        return scopes.all().stream().map(ScopeKind::name).collect(Collectors.joining(", "));
    }
}
