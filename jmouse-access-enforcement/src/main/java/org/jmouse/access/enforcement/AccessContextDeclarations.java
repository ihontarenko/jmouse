package org.jmouse.access.enforcement;

import org.jmouse.access.PlaceholderResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads {@link AccessContext} off a method and its class, and states what that call publishes.
 *
 * <p>The counterpart of {@link AccessRequirements}, and it merges by the same rule with one
 * exception: <strong>values</strong> merge with the method winning on a name collision, so a
 * controller states the tenant once and each method adds its own; <strong>action</strong> is
 * method-only, because a controller does several things and an action on a class would be a lie.
 *
 * <h2>What is decided once, and what is decided per call</h2>
 *
 * <p>Everything that can be is decided once. Which parameter a name refers to, whether exactly one of
 * {@code from} and {@code is} was written, what a {@code ${…}} fills to — none of that changes while
 * the application runs, and re-deciding it on the security path of every request is the sort of cost
 * that only ever surfaces as a product that feels slow.
 *
 * <p>What is left for the call is reading the arguments, which is what a call <em>is</em>.
 *
 * <h2>⚠️ A malformed declaration fails at startup, not at call time</h2>
 *
 * <p>{@link #of(Method, Class)} throws on a value that writes both {@code from} and {@code is}, or
 * neither, or a placeholder nothing can fill. Called from a startup scan over every route — which is
 * what a product should do — that is a boot failure naming the route. Reached first by a request it
 * is a 500, which is worse but still not a silent pass: the one outcome this must never have is a
 * declaration that quietly publishes nothing and a rule that quietly never fires.
 */
public class AccessContextDeclarations {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessContextDeclarations.class);

    private final ParameterNaming      naming;
    private final PlaceholderResolver  placeholders;
    private final AmbientAccessValues  ambient;

    /**
     * ⚠️ Keyed by the method <em>and</em> the class it was called on, never by the method alone.
     *
     * <p>An adapter hands over the method it was given, which under a JDK proxy is the
     * <strong>interface's</strong>. Two implementations of one interface would then share an entry,
     * and the class-level values of whichever arrived first would be published for both — a route
     * publishing another route's tenant, decided by startup order.
     */
    private final Map<Declaration, Optional<Declared>> byDeclaration = new ConcurrentHashMap<>();

    /**
     * @param ambient what is true of every call, published under whatever a route says about itself
     */
    public AccessContextDeclarations(
            ParameterNaming naming, PlaceholderResolver placeholders, AmbientAccessValues ambient) {

        this.naming       = naming       == null ? ParameterNaming.declared()   : naming;
        this.placeholders = placeholders == null ? PlaceholderResolver.none()   : placeholders;
        this.ambient      = ambient      == null ? AmbientAccessValues.none()   : ambient;
    }

    /** The wiring for an installation with nothing to publish beyond what its routes declare. */
    public AccessContextDeclarations(ParameterNaming naming, PlaceholderResolver placeholders) {
        this(naming, placeholders, null);
    }

    /** What this call publishes, or nothing where it declares nothing. */
    public Optional<Declared> of(Method method, Class<?> targetClass) {
        return byDeclaration.computeIfAbsent(
                new Declaration(method, targetClass),
                cached -> read(cached.method(), cached.targetClass()));
    }

    /** One method as called on one class — see {@link #byDeclaration} for why both are the key. */
    private record Declaration(Method method, Class<?> targetClass) {
    }

    private Optional<Declared> read(Method method, Class<?> targetClass) {
        AccessContext onMethod = findOnMethod(method, targetClass);
        AccessContext onClass  = findOnType(targetClass);

        if (onMethod == null && onClass == null) {
            return Optional.empty();
        }

        String action = onMethod == null ? "" : onMethod.action();

        return Optional.of(new Declared(
                action.isBlank() ? null : action,
                merged(onMethod, onClass, method, targetClass),
                MethodArguments.of(method, naming),
                describe(method, targetClass),
                ambient));
    }

    /**
     * The values of both declarations, method first.
     *
     * <p>⚠️ A name written in both places is the method's. A class-level value is a default — "every
     * route here is about this tenant" — and a method restating it is a method that means it.
     */
    private List<Value> merged(
            AccessContext onMethod, AccessContext onClass, Method method, Class<?> targetClass) {

        Map<String, Value> values = new LinkedHashMap<>();

        collect(onClass,  values, method, targetClass);
        collect(onMethod, values, method, targetClass);

        return List.copyOf(values.values());
    }

    private void collect(
            AccessContext declared, Map<String, Value> into, Method method, Class<?> targetClass) {

        if (declared == null) {
            return;
        }

        for (AccessValue value : declared.values()) {
            into.put(value.name(), read(value, method, targetClass));
        }
    }

    /**
     * One declared value, with everything constant about it already worked out.
     *
     * @throws IllegalStateException where the declaration cannot mean anything — see the class comment
     */
    private Value read(AccessValue declared, Method method, Class<?> targetClass) {
        boolean fromAnArgument = !declared.from().isBlank();
        boolean fromALiteral   = !declared.is().isBlank();
        String  where          = describe(method, targetClass);

        if (fromAnArgument == fromALiteral) {
            throw new IllegalStateException(
                    where + " publishes '" + declared.name() + "' with "
                    + (fromAnArgument ? "both from and is" : "neither from nor is")
                    + ". Exactly one of them says where a value comes from: 'from' names the method "
                    + "parameter to read it out of, 'is' states a constant this route always carries.");
        }

        if (declared.name().isBlank()) {
            throw new IllegalStateException(
                    where + " publishes a value with no name. A rule reads a value by name, so an "
                    + "unnamed one is a value no rule can ever mention.");
        }

        if (fromALiteral) {
            return new Value(declared.name(), null, fill(declared.is(), declared.name(), where), false);
        }

        return new Value(declared.name(), declared.from(), null, declared.optional());
    }

    /**
     * ⚠️ Filled here, at read time, and never per call.
     *
     * <p>A placeholder is configuration: fixed before the first request and identical for every one
     * of them. Filling it per call would make an authorization rule's meaning depend on runtime
     * state, which is the one thing a rule written in a file must not do.
     */
    private String fill(String literal, String name, String where) {
        try {
            return PlaceholderResolver.fill(literal, placeholders);
        } catch (RuntimeException unresolved) {
            throw new IllegalStateException(
                    where + " publishes '" + name + "' as " + literal + ", and that cannot be filled: "
                    + unresolved.getMessage() + " A placeholder nothing can fill would be compared "
                    + "literally forever, so a rule about it would never hold and nothing would say so.",
                    unresolved);
        }
    }

    private static String describe(Method method, Class<?> targetClass) {
        return targetClass.getSimpleName() + "." + method.getName();
    }

    /**
     * The declaration on this method, or on the same method where a supertype declares it.
     *
     * <p>The concrete class is searched first, for {@link AccessRequirements}'s reason: an override
     * that states its own action means it.
     */
    private static AccessContext findOnMethod(Method method, Class<?> targetClass) {
        AccessContext direct = method.getAnnotation(AccessContext.class);

        if (direct != null) {
            return direct;
        }

        for (Class<?> type : hierarchyOf(targetClass)) {
            try {
                AccessContext inherited = type
                        .getDeclaredMethod(method.getName(), method.getParameterTypes())
                        .getAnnotation(AccessContext.class);

                if (inherited != null) {
                    return inherited;
                }
            } catch (NoSuchMethodException absentHere) {
                // This type simply does not declare the method — an ordinary answer, not a fault.
            }
        }

        return null;
    }

    private static AccessContext findOnType(Class<?> targetClass) {
        for (AnnotatedElement type : hierarchyOf(targetClass)) {
            AccessContext declared = type.getAnnotation(AccessContext.class);

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

    /**
     * What one route publishes — the declaration, read and checked, ready to be given arguments.
     *
     * @param action     what is being done, or null where the route only publishes values
     * @param values     what it carries, class-level defaults already merged under the method's
     * @param parameters what this method's arguments are called, resolved once
     * @param where      the route, for a message somebody has to act on
     * @param ambient    what is true of every call, asked per call because that is what makes it
     *                   ambient — a value memoised here would be the first request's answer forever
     */
    public record Declared(
            String              action,
            List<Value>         values,
            MethodArguments     parameters,
            String              where,
            AmbientAccessValues ambient) {

        /** Every value name this route publishes — what a catalogue is built out of. */
        public Set<String> publishedNames() {
            Set<String> names = new TreeSet<>();

            values.forEach(value -> names.add(value.name()));

            return names;
        }

        /**
         * The values for one call, or a complaint naming the promise that was broken.
         *
         * <p>⚠️ <strong>An absent required value refuses.</strong> The route declared it, which is a
         * promise; a null quietly entering the bag would make a conditional deny stop applying, and a
         * deny that stops applying is a call that goes through.
         */
        public Published publish(Object[] arguments) {
            // ⚠️ Ambient first, so a route that declares the same name overwrites it. Ambient is a
            // default about the surrounding request; a route that publishes it from a parameter has
            // said something more specific and means it.
            Map<String, Object> published = new LinkedHashMap<>(ambientValues());

            for (Value value : values) {
                if (value.isLiteral()) {
                    published.put(value.name(), value.literal());
                    continue;
                }

                Optional<String> passed = parameters.valueOf(value.from(), arguments);

                if (passed.isEmpty() && !value.optional()) {
                    return new Published(null, missing(value));
                }

                passed.ifPresent(text -> published.put(value.name(), text));
            }

            return new Published(published, null);
        }

        /**
         * ⚠️ <strong>An ambient publisher that throws publishes nothing, and says so in the log.</strong>
         *
         * <p>{@link AmbientAccessValues} is documented as not throwing, and documentation is not a
         * mechanism. What one of these reaches for is by nature outside the invocation — a repository,
         * a request attribute, a workspace deleted between two calls — so the ways it can fail are not
         * ones the caller of a route can do anything about.
         *
         * <p>Letting it out turns every guarded route into a 500. Swallowing it costs a rule that
         * reads the value: it does not hold, which is safe for a conditional allow and — like every
         * absent value — the open direction for a conditional deny. That is the same trade
         * {@code ExpressionConditionCompiler} makes when a condition blows up, and it is made here for
         * the same reason: an authorization mechanism must not be able to take a product down.
         *
         * <p>⚠️ This now catches a failure to <em>register</em>, which is rare and is a defect. A
         * failure to <em>work a value out</em> never reaches here at all: it happens later, on first
         * read, and {@link org.jmouse.access.spi.DeferredValue} answers it as an absent value.
         */
        private Map<String, Object> ambientValues() {
            try {
                return AmbientValues.publishedBy(ambient);
            } catch (RuntimeException failed) {
                LOGGER.warn("The ambient access values could not be read for {}, so no rule about one "
                            + "will hold for this call: {}", where, failed.toString());

                return Map.of();
            }
        }

        private String missing(Value value) {
            return where + " publishes '" + value.name() + "' from the parameter '" + value.from()
                   + "', and this call passed nothing for it. The route promised the value, and a rule "
                   + "may be written against it — proceeding would silently mean that rule does not "
                   + "apply. Pass it, or declare the value optional and write the rule to tolerate null.";
        }
    }

    /**
     * One value's resolved declaration.
     *
     * @param name     what a rule calls it
     * @param from     the parameter to read it out of, or null for a literal
     * @param literal  the constant, placeholders already filled, or null for an argument
     * @param optional whether the call may proceed without it
     */
    public record Value(String name, String from, String literal, boolean optional) {

        public boolean isLiteral() {
            return from == null;
        }
    }

    /**
     * The outcome of publishing one call's values.
     *
     * @param values  what to publish, or null where a promise was broken
     * @param refusal why the call may not proceed, or null where it may
     */
    public record Published(Map<String, Object> values, String refusal) {

        public boolean isRefused() {
            return refusal != null;
        }
    }
}
