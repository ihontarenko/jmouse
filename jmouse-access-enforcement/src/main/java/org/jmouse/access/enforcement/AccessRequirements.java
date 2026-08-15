package org.jmouse.access.enforcement;

import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Reads {@link RequiresAccess} off a method and its class, and states what that call actually needs.
 *
 * <p>The rule is <em>method wins, class is the default</em> — attribute by attribute, so a controller
 * declares its module and its scope once and each method adds only the permission its own verb needs.
 *
 * <p>"Written" means "differs from the attribute's declared default", asked of the annotation type
 * rather than repeated as literals here. It used to be Spring's {@code MergedAnnotation.hasDefaultValue}
 * doing exactly the same comparison; plain reflection loses nothing, and losing the dependency is the
 * point.
 *
 * <p>The type hierarchy is searched, because a declaration on an interface or a base controller has to
 * reach the class that implements it — an annotation that stopped at the concrete type would quietly
 * unguard every route that inherited its rule.
 */
public class AccessRequirements {

    private final ScopeCatalog        scopes;
    private final ExternalAccessRules external;

    public AccessRequirements(ScopeCatalog scopes) {
        this(scopes, ExternalAccessRules.none());
    }

    /**
     * @param external what the product declares about types it does not own — a library's controllers,
     *                 which cannot carry the annotation. See {@link ExternalAccessRules} for why the
     *                 alternative, a URL rule gated on a role, is worse than it looks
     */
    public AccessRequirements(ScopeCatalog scopes, ExternalAccessRules external) {
        this.scopes   = scopes;
        this.external = external;
    }

    /** What this call needs, or nothing where it declares nothing. */
    public Optional<AccessRequirement> of(Method method, Class<?> targetClass) {
        RequiresAccess onMethod = findOnMethod(method, targetClass);
        RequiresAccess onClass  = findOnType(targetClass);

        if (onMethod == null && onClass == null) {
            // ⚠️ Only here, and never above: an annotation always wins. A type that states its own rule
            // means it, and a product must not be able to widen somebody else's declared requirement
            // from the outside without touching the declaration.
            return external.forMethod(method, targetClass).map(this::resolve);
        }

        return Optional.of(new AccessRequirement(
                merged(onMethod, onClass, RequiresAccess::permission, ""),
                merged(onMethod, onClass, RequiresAccess::module,     ""),
                scopeOf(merged(onMethod, onClass, RequiresAccess::scope, "")),
                merged(onMethod, onClass, RequiresAccess::resource,   void.class),
                merged(onMethod, onClass, RequiresAccess::resourceId, "")));
    }

    /** An external declaration, read in exactly the shape an annotation would have been. */
    private AccessRequirement resolve(ExternalAccessRules.Declaration declared) {
        return new AccessRequirement(
                declared.permission(),
                declared.module(),
                scopeOf(declared.scope()),
                declared.resource(),
                declared.resourceId());
    }

    /**
     * The scope a declaration names, or the widest where it names none.
     *
     * <p>This is where a mistyped scope stops. Resolving at read time rather than at decision time is
     * deliberate: the declaration validator reads every route at startup, so an unknown name is a
     * failure to boot rather than a 500 on whichever route nobody exercised.
     */
    private ScopeKind scopeOf(String name) {
        if (name.isBlank()) {
            return scopes.everything();
        }

        return scopes.byName(name).orElseThrow(() -> new IllegalStateException(
                "@RequiresAccess names the scope '" + name + "', which this installation does not "
                + "register. Known scopes: " + scopes.all().stream().map(ScopeKind::name).toList()
                + ". A scope nobody registered cannot be decided about, and treating it as unscoped "
                + "would silently widen the route."));
    }

    /**
     * One attribute, read from the method where it was written there and from the class otherwise.
     *
     * @param unspecified the attribute's declared default — what "nobody wrote this" means
     */
    private static <T> T merged(
            RequiresAccess onMethod, RequiresAccess onClass, Function<RequiresAccess, T> read, T unspecified) {

        if (onMethod != null && !unspecified.equals(read.apply(onMethod))) {
            return read.apply(onMethod);
        }
        if (onClass != null && !unspecified.equals(read.apply(onClass))) {
            return read.apply(onClass);
        }

        return unspecified;
    }

    /**
     * The declaration on this method, or on the same method where a supertype declares it.
     *
     * <p>The concrete class is searched first — an override that states its own rule means it, and a
     * base class it happens to extend must not win over it.
     */
    private static RequiresAccess findOnMethod(Method method, Class<?> targetClass) {
        RequiresAccess direct = method.getAnnotation(RequiresAccess.class);

        if (direct != null) {
            return direct;
        }

        for (Class<?> type : hierarchyOf(targetClass)) {
            try {
                RequiresAccess inherited = type
                        .getDeclaredMethod(method.getName(), method.getParameterTypes())
                        .getAnnotation(RequiresAccess.class);

                if (inherited != null) {
                    return inherited;
                }
            } catch (NoSuchMethodException absentHere) {
                // This type simply does not declare the method — an ordinary answer, not a fault.
            }
        }

        return null;
    }

    private static RequiresAccess findOnType(Class<?> targetClass) {
        for (AnnotatedElement type : hierarchyOf(targetClass)) {
            RequiresAccess declared = type.getAnnotation(RequiresAccess.class);

            if (declared != null) {
                return declared;
            }
        }

        return null;
    }

    /** The class, its superclasses and every interface any of them implements, nearest first. */
    private static List<Class<?>> hierarchyOf(Class<?> type) {
        List<Class<?>> hierarchy = new ArrayList<>();

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            hierarchy.add(current);
            hierarchy.addAll(List.of(current.getInterfaces()));
        }

        return hierarchy;
    }
}
