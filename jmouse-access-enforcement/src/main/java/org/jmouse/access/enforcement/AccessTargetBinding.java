package org.jmouse.access.enforcement;

import org.jmouse.access.AccessTarget;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeNature;
import org.jmouse.access.Subject;
import org.jmouse.access.spi.AccessTargetRegistry;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where a call is aimed, worked out from the call itself.
 *
 * <p>The structural half of {@link RequiresAccess}, and the half that closes the oldest hole in most
 * products: a header naming the place <em>performs no authorization</em>, so every check that begins
 * from one begins from something the caller chose.
 *
 * <p>So the answers are tried in order of how much the caller controls them:
 *
 * <ol>
 *   <li><strong>The row itself.</strong> A declared resource hands the identifier the method already
 *       takes to that feature's resolver, and the places and the owner all come back from the row. A
 *       caller cannot move a row somewhere else by sending a different header.
 *   <li><strong>A place the route names.</strong> Part of the URL, so it is at least the place the
 *       request is about — and the one the caller will be refused over.
 *   <li><strong>{@linkplain AmbientPlace The ambient place.}</strong> Last, and only where the route
 *       named none of that kind.
 * </ol>
 *
 * <p>Parameter bindings are worked out once per method. What a method's parameters are called cannot
 * change while the application runs, and re-reading annotations on the security path of every request
 * is the kind of cost that only ever surfaces as a product that feels slow.
 */
public class AccessTargetBinding {

    private final AccessTargetRegistry targets;
    private final ScopeCatalog         scopes;
    private final ParameterNaming      naming;
    private final AmbientPlace         ambient;

    private final Map<Method, MethodArguments> argumentsByMethod = new ConcurrentHashMap<>();

    public AccessTargetBinding(
            AccessTargetRegistry targets,
            ScopeCatalog         scopes,
            ParameterNaming      naming,
            AmbientPlace         ambient) {

        this.targets = targets;
        this.scopes  = scopes;
        this.naming  = naming == null ? ParameterNaming.declared() : naming;
        this.ambient = ambient == null ? AmbientPlace.none() : ambient;
    }

    /**
     * The target for one call, or nothing where the declaration names a row that does not exist.
     *
     * <p>Empty is a refusal rather than an unscoped call, and that distinction is the whole safety of
     * this class: an unscoped target passes every axis that is about a place, so reading "no such row"
     * as "no place" would turn a typo into an open door.
     */
    public Optional<AccessTarget> bind(
            Method method, Object[] arguments, AccessRequirement required, Subject subject) {

        MethodArguments parameters = argumentsByMethod.computeIfAbsent(
                method, cached -> MethodArguments.of(cached, naming));

        Optional<AccessTarget> aimed = required.namesARow()
                ? fromTheRow(parameters, arguments, required)
                : Optional.of(fromTheRequest(parameters, arguments, required, subject));

        return aimed.map(target -> withModule(target, required));
    }

    /** Where the row lives, asked of the feature that owns it. */
    private Optional<AccessTarget> fromTheRow(
            MethodArguments parameters, Object[] arguments, AccessRequirement required) {

        return identifierName(parameters, required)
                .flatMap(name -> parameters.valueOf(name, arguments))
                .flatMap(identifier -> targets.resolve(required.resource(), identifier));
    }

    private AccessTarget fromTheRequest(
            MethodArguments parameters, Object[] arguments, AccessRequirement required, Subject subject) {

        AccessTarget aimed = placesNamedBy(parameters, arguments, required.scope());

        // A create route has no row yet, so it aims at the caller as the owner of what is about to
        // exist — otherwise a grant over one's own rows could never satisfy one, and everybody would
        // be able to edit their own rows and unable to make one. Read off the scope's nature rather
        // than off a constant: "the rows I own" is the model's scope, not any product's.
        return required.scope().nature() == ScopeNature.OWN_ROWS
                ? aimed.withOwner(subject.ownedRowsBelongTo())
                : aimed;
    }

    /**
     * Every place this call names, read off the route and — for the ambient one — off the request.
     *
     * <p>The binder knows no place by name: it asks the catalogue for the floors and each floor for
     * the parameter that names it, so a third floor is a registration and this loop does not change.
     * Places wider than the endpoint declares are read too where the route happens to name one: a
     * route under an account that also names a workspace is about both, and dropping the wider one
     * would lose the grant that covers the whole account.
     */
    private AccessTarget placesNamedBy(
            MethodArguments parameters, Object[] arguments, ScopeKind declared) {

        AccessTarget aimed = AccessTarget.installation();

        for (ScopeKind floor : scopes.floors()) {
            Optional<String> named = floor.requestParameter()
                    .flatMap(parameter -> parameters.valueOf(parameter, arguments));

            if (named.isPresent()) {
                aimed = aimed.at(floor, named.get());
            }
        }

        return withAmbientPlace(aimed, declared);
    }

    private AccessTarget withAmbientPlace(AccessTarget aimed, ScopeKind declared) {
        ScopeKind kind = ambient.kind();

        if (kind == null || !kind.isAtLeastAsWideAs(declared) || aimed.names(kind)) {
            return aimed;
        }

        return ambient.instance().map(instance -> aimed.at(kind, instance)).orElse(aimed);
    }

    private Optional<String> identifierName(MethodArguments parameters, AccessRequirement required) {
        return required.resourceId().isBlank()
                ? parameters.soleIdentifierName()
                : Optional.of(required.resourceId());
    }

    private AccessTarget withModule(AccessTarget target, AccessRequirement required) {
        return required.namesAModule() ? target.withModule(required.module()) : target;
    }
}
