package org.jmouse.access.enforcement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a type <em>somebody else wrote</em> requires — the declaration for a class that cannot carry the
 * annotation.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@link RequiresAccess} is read off a class or a method, which works perfectly right up to the first
 * handler a product does not own. A library ships controllers — a management screen, an upload endpoint,
 * a protocol surface — and a product cannot annotate them. Every product that met this reached for the
 * same fallback: a URL-prefix rule in the web layer, gated on a <strong>role</strong>, because a role is
 * the only thing the web layer knows how to compare.
 *
 * <p>That answer is wrong in a way that is easy to miss. A role is not a permission: it says what
 * somebody holds <em>somewhere</em>, with no target, so a rule written that way never reaches the scope,
 * the module switch, an entitlement or a condition. The product then has two authorization models —
 * one for its own routes and a weaker one for everybody else's — and the weaker one is on exactly the
 * routes nobody remembers reviewing.
 *
 * <p>So the declaration moves rather than the enforcement: a product states, in its own code, what a
 * foreign type needs, and it is then gated by the same engine, on the same axes, with the same refusals
 * as everything it wrote itself.
 *
 * <h2>How it reads</h2>
 *
 * <pre>{@code
 * @Bean
 * ExternalAccessRules aiManagementAccess() {
 *     return ExternalAccessRules.builder()
 *             .type(ProviderController.class, Declaration.permission("ai:read").atScope("GLOBAL"))
 *             .type(ProviderAdministrationController.class,
 *                   Declaration.permission("ai:administer").atScope("GLOBAL"))
 *             .build();
 * }
 * }</pre>
 *
 * <p>⚠️ <strong>An annotation always wins.</strong> A type that declares its own rule means it, and a
 * product must not be able to quietly widen somebody else's declared requirement from the outside. These
 * rules answer only where nothing is declared.
 *
 * <p>⚠️ <strong>A method rule narrows, it does not replace.</strong> Naming a method states what that
 * one needs; everything else on the type keeps the type's rule. A type with no rule and one named method
 * leaves the rest un-gated, which is the one shape worth saying out loud, because it looks guarded.
 */
public interface ExternalAccessRules {

    /**
     * What a foreign type needs, in the annotation's own vocabulary.
     *
     * <p>The same five attributes {@link RequiresAccess} carries, as a value rather than as an
     * annotation instance — the scope is still a <em>name</em> here and is resolved by
     * {@link AccessRequirements}, so a typo fails the same way and in the same place either way.
     *
     * @param permission what the caller must hold, or blank to gate on a module alone
     * @param module     the feature module, or blank where it belongs to none
     * @param scope      the scope's name, or blank for the widest
     * @param resource   the kind of row it acts on, or {@code void.class} where it acts on none
     * @param resourceId which parameter carries that row's identifier, or blank for the sole identifier
     */
    record Declaration(
            String   permission,
            String   module,
            String   scope,
            Class<?> resource,
            String   resourceId
    ) {

        public Declaration {
            permission = permission == null ? "" : permission;
            module     = module     == null ? "" : module;
            scope      = scope      == null ? "" : scope;
            resource   = resource   == null ? void.class : resource;
            resourceId = resourceId == null ? "" : resourceId;
        }

        /** The common case: hold this permission. */
        public static Declaration permission(String permission) {
            return new Declaration(permission, "", "", void.class, "");
        }

        /**
         * The other common case: a route reachable by anyone signed in.
         *
         * <p>⚠️ Not the same as leaving a type undeclared. This gates it on the identity axis, which
         * refuses nobody who is authenticated and refuses everybody who is not — so the refusal reads
         * <em>sign in</em> rather than <em>no permission</em>. An undeclared type is gated on nothing.
         */
        public static Declaration authenticated() {
            return new Declaration("", "", "", void.class, "");
        }

        public Declaration atScope(String scope) {
            return new Declaration(permission, module, scope, resource, resourceId);
        }

        public Declaration inModule(String module) {
            return new Declaration(permission, module, scope, resource, resourceId);
        }

        public Declaration about(Class<?> resource, String resourceId) {
            return new Declaration(permission, module, scope, resource, resourceId);
        }
    }

    /** What this method needs where nothing on it is annotated, or empty where these rules say nothing. */
    Optional<Declaration> forMethod(Method method, Class<?> targetClass);

    /**
     * Whether these rules say anything about this type at all.
     *
     * <p>Separate from {@link #forMethod} because the advice's pointcut has to decide whether to wrap a
     * type before it has a call to ask about — and a pointcut is consulted once per bean while
     * {@code forMethod} is consulted once per call.
     */
    boolean covers(Class<?> targetClass);

    /** A product that declares nothing about anybody else's types. */
    static ExternalAccessRules none() {
        return new ExternalAccessRules() {

            @Override
            public Optional<Declaration> forMethod(Method method, Class<?> targetClass) {
                return Optional.empty();
            }

            @Override
            public boolean covers(Class<?> targetClass) {
                return false;
            }
        };
    }

    /**
     * Every contributed set as one.
     *
     * <p>⚠️ Contributing is many and consuming is one: declarations naturally arrive per library adopted,
     * so a product with two of them must not have to merge them by hand — and must not be told at startup
     * that a bean is ambiguous rather than that something is un-gated.
     *
     * <p>⚠️ The <em>first</em> set that speaks about a method wins, and nothing silently merges two
     * answers about one type. Two contributors disagreeing is a mistake worth finding rather than a
     * precedence rule worth learning.
     */
    static ExternalAccessRules all(Collection<ExternalAccessRules> contributed) {
        List<ExternalAccessRules> rules = List.copyOf(contributed);

        if (rules.isEmpty()) {
            return none();
        }

        if (rules.size() == 1) {
            return rules.getFirst();
        }

        return new ExternalAccessRules() {

            @Override
            public Optional<Declaration> forMethod(Method method, Class<?> targetClass) {
                return rules.stream()
                        .map(candidate -> candidate.forMethod(method, targetClass))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst();
            }

            @Override
            public boolean covers(Class<?> targetClass) {
                return rules.stream().anyMatch(candidate -> candidate.covers(targetClass));
            }
        };
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * States rules by type, and by method where one route on a type needs more than the rest.
     *
     * <p>⚠️ A subtype is covered by a rule written about its supertype, because a library's controller
     * is exactly the kind of thing a product subclasses to change one route — and a rule that stopped at
     * the exact class would un-gate the subclass silently.
     */
    final class Builder {

        private final Map<Class<?>, Declaration>              byType   = new HashMap<>();
        private final Map<Class<?>, Map<String, Declaration>> byMethod = new HashMap<>();

        private Builder() {
        }

        /** What every route on this type needs. */
        public Builder type(Class<?> type, Declaration declaration) {
            byType.put(type, declaration);

            return this;
        }

        /**
         * What one route on this type needs, over and above the type's own rule.
         *
         * <p>By name rather than by {@link Method}, because the point of these rules is to describe a
         * type whose methods the product cannot reference without depending on its signatures. An
         * overloaded name states the rule for every overload of it, which is the only reading that is
         * safe when the alternative is gating some of them.
         */
        public Builder method(Class<?> type, String methodName, Declaration declaration) {
            byMethod.computeIfAbsent(type, declaring -> new HashMap<>()).put(methodName, declaration);

            return this;
        }

        public ExternalAccessRules build() {
            Map<Class<?>, Declaration>              types   = Map.copyOf(byType);
            Map<Class<?>, Map<String, Declaration>> methods = Map.copyOf(byMethod);
            Set<Class<?>>                           covered = coveredTypes(types, methods);

            return new ExternalAccessRules() {

                @Override
                public Optional<Declaration> forMethod(Method method, Class<?> targetClass) {
                    for (Class<?> declaring : assignableFrom(targetClass, covered)) {
                        Declaration named = methods.getOrDefault(declaring, Map.of()).get(method.getName());

                        if (named != null) {
                            return Optional.of(named);
                        }

                        Declaration whole = types.get(declaring);

                        if (whole != null) {
                            return Optional.of(whole);
                        }
                    }

                    return Optional.empty();
                }

                @Override
                public boolean covers(Class<?> targetClass) {
                    return !assignableFrom(targetClass, covered).isEmpty();
                }
            };
        }

        private static Set<Class<?>> coveredTypes(
                Map<Class<?>, Declaration> types, Map<Class<?>, Map<String, Declaration>> methods) {

            Set<Class<?>> covered = new LinkedHashSet<>(types.keySet());
            covered.addAll(methods.keySet());

            return Set.copyOf(covered);
        }

        /** The declared types this one is, nearest first — so a subclass reads its parent's rule. */
        private static List<Class<?>> assignableFrom(Class<?> targetClass, Set<Class<?>> covered) {
            List<Class<?>> matching = new ArrayList<>();

            for (Class<?> current = targetClass; current != null && current != Object.class;
                 current = current.getSuperclass()) {

                if (covered.contains(current)) {
                    matching.add(current);
                }
            }

            covered.stream()
                    .filter(Class::isInterface)
                    .filter(declared -> declared.isAssignableFrom(targetClass))
                    .forEach(matching::add);

            return matching;
        }
    }
}
