package org.jmouse.mapper.binding;

import org.jmouse.core.Verify;
import org.jmouse.mapper.MappingContext;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Declarative mapping rule for a single target property. 🧩
 *
 * <p>{@code PropertyMapping} describes <em>how</em> a target property (identified by {@link #targetName()})
 * should be populated. Mappings can be:</p>
 * <ul>
 *   <li><b>terminal</b>: {@link Ignore}, {@link Constant}, {@link Reference}, {@link Provider}, {@link Compute}</li>
 *   <li><b>decorators</b>: {@link DefaultValue}, {@link Transform}</li>
 *   <li><b>composites</b>: {@link When}, {@link Coalesce}, {@link Required}</li>
 * </ul>
 *
 * <p>Mappings are evaluated by a runtime component (typically via a {@link PropertyMappingVisitor})
 * which interprets each mapping node and produces a value for the target slot.</p>
 *
 * <p>The interface is sealed to keep the mapping algebra closed and visitor-friendly.</p>
 *
 * @see PropertyMappingVisitor
 */
public sealed interface PropertyMapping
        permits PropertyMapping.Ignore,
                PropertyMapping.Constant,
                PropertyMapping.Reference,
                PropertyMapping.Provider,
                PropertyMapping.Compute,
                PropertyMapping.DefaultValue,
                PropertyMapping.Transform,
                PropertyMapping.When,
                PropertyMapping.Guarded,
                PropertyMapping.Expression,
                PropertyMapping.Coalesce,
                PropertyMapping.Required {

    /**
     * Create an ignore mapping.
     *
     * @param targetName target property name
     * @return ignore mapping
     */
    static PropertyMapping ignore(String targetName) {
        return new Ignore(targetName);
    }

    /**
     * Create a constant mapping.
     *
     * @param targetName target property name
     * @param value constant value (may be {@code null})
     * @return constant mapping
     */
    static PropertyMapping constant(String targetName, Object value) {
        return new Constant(targetName, value);
    }

    /**
     * Create a reference mapping that reads a value from the source accessor using {@code sourceReference}.
     *
     * @param targetName target property name
     * @param sourceReference source reference/path (must not be blank)
     * @return reference mapping
     */
    static PropertyMapping reference(String targetName, String sourceReference) {
        return new Reference(targetName, sourceReference);
    }

    /**
     * Create a provider mapping that computes a value from the current source object.
     *
     * @param targetName target property name
     * @param provider value provider (never {@code null})
     * @return provider mapping
     */
    static PropertyMapping provider(String targetName, ValueProvider<Object> provider) {
        return new Provider(targetName, provider);
    }

    /**
     * Create a compute mapping that computes a value from (source, context).
     *
     * @param targetName target property name
     * @param fn compute function (never {@code null})
     * @return compute mapping
     */
    static PropertyMapping compute(String targetName, ComputeFunction<Object> fn) {
        return new Compute(targetName, fn);
    }

    /**
     * Decorate {@code delegate} with a default value supplier.
     *
     * @param targetName target property name
     * @param delegate delegate mapping (never {@code null})
     * @param s default supplier (never {@code null})
     * @return default-value mapping
     */
    static PropertyMapping defaultValue(String targetName, PropertyMapping delegate, Supplier<?> s) {
        return new DefaultValue(targetName, delegate, s);
    }

    /**
     * Decorate {@code delegate} with a transformer applied to the delegate result.
     *
     * @param targetName target property name
     * @param delegate delegate mapping (never {@code null})
     * @param t transformer (never {@code null})
     * @return transform mapping
     */
    static PropertyMapping transform(String targetName, PropertyMapping delegate, ValueTransformer t) {
        return new Transform(targetName, delegate, t);
    }

    // ===== compositional mappings =====

    /**
     * Guard {@code delegate} with a condition.
     *
     * <p>Condition is evaluated against (source, context). If the condition fails, the runtime
     * typically treats the mapping as producing "no value" (implementation-defined, often {@code null}).</p>
     *
     * @param targetName target property name
     * @param cond condition predicate (never {@code null})
     * @param delegate delegate mapping (never {@code null})
     * @return conditional mapping
     */
    static PropertyMapping when(String targetName, BiPredicate<Object, MappingContext> cond, PropertyMapping delegate) {
        return new When(targetName, cond, delegate);
    }

    /**
     * Create a guarded mapping: when the condition is false, nothing is written and the target keeps
     * what it held.
     *
     * <p>⚠️ Not the same as {@link #when}, which yields {@code null} and lets the null policy decide.
     * See {@link Guarded}.</p>
     *
     * @param targetName target property name
     * @param cond       evaluated against the source; true means write
     * @param delegate   what produces the value
     * @return guarded mapping
     */
    static PropertyMapping guarded(String targetName, BiPredicate<Object, MappingContext> cond, PropertyMapping delegate) {
        return new Guarded(targetName, cond, delegate);
    }

    /**
     * Create a computed mapping that remembers the expression it was written as.
     *
     * @param targetName target property name
     * @param source     the expression exactly as written
     * @param fn         what evaluates it
     * @return expression mapping
     */
    static PropertyMapping expression(String targetName, String source, ComputeFunction<Object> fn) {
        return new Expression(targetName, source, fn);
    }

    /**
     * Create a coalesce mapping that tries candidates in order and returns the first successful result.
     *
     * @param targetName target property name
     * @param candidates ordered list of candidates (must not be empty)
     * @return coalesce mapping
     */
    static PropertyMapping coalesce(String targetName, List<PropertyMapping> candidates) {
        return new Coalesce(targetName, candidates);
    }

    /**
     * Decorate {@code delegate} as required.
     *
     * <p>If the delegate produces no value (typically {@code null}), the runtime should raise an error
     * using {@code code} and {@code msg}.</p>
     *
     * @param targetName target property name
     * @param delegate delegate mapping (never {@code null})
     * @param code stable error code (must not be blank)
     * @param msg human-readable message (must not be blank)
     * @return required mapping
     */
    static PropertyMapping required(String targetName, PropertyMapping delegate, String code, String msg) {
        return new Required(targetName, delegate, code, msg);
    }

    // ===== core API =====

    /**
     * Target property name this mapping applies to.
     *
     * @return target property name
     */
    String targetName();

    /**
     * Accept a visitor to interpret this mapping node.
     *
     * @param visitor mapping visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    <R> R accept(PropertyMappingVisitor<R> visitor);

    /**
     * Mapping that ignores the target property.
     */
    record Ignore(String targetName) implements PropertyMapping {
        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Mapping that assigns a constant value to the target property.
     */
    record Constant(String targetName, Object value) implements PropertyMapping {
        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Mapping that reads a value from the source accessor using {@link #sourceReference()}.
     */
    record Reference(String targetName, String sourceReference) implements PropertyMapping {
        public Reference {
            Verify.notBlank(sourceReference, "sourceReference");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Mapping that obtains a value from a {@link ValueProvider}.
     */
    record Provider(String targetName, ValueProvider<Object> valueProvider) implements PropertyMapping {
        public Provider {
            Verify.nonNull(valueProvider, "valueProvider");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Mapping that computes a value using a {@link ComputeFunction}.
     */
    record Compute(String targetName, ComputeFunction<Object> function) implements PropertyMapping {
        public Compute {
            Verify.nonNull(function, "function");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Decorator mapping that supplies a default value when the delegate produces no value.
     */
    record DefaultValue(String targetName, PropertyMapping delegate, Supplier<?> defaultSupplier)
            implements PropertyMapping {
        public DefaultValue {
            Verify.nonNull(delegate, "delegate");
            Verify.nonNull(defaultSupplier, "defaultSupplier");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Decorator mapping that transforms the delegate result.
     */
    record Transform(String targetName, PropertyMapping delegate, ValueTransformer transformer)
            implements PropertyMapping {
        public Transform {
            Verify.nonNull(delegate, "delegate");
            Verify.nonNull(transformer, "transformer");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Conditional mapping that applies {@code delegate} only when {@code condition} matches.
     */
    record When(String targetName, BiPredicate<Object, MappingContext> condition, PropertyMapping delegate)
            implements PropertyMapping {
        public When {
            Verify.nonNull(condition, "condition");
            Verify.nonNull(delegate, "delegate");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Conditional mapping that writes <strong>nothing at all</strong> when {@code condition} is false.
     *
     * <h2>⚠️ How this differs from {@link When}, and why both exist</h2>
     *
     * <p>{@code When} yields {@code null} when its condition is false, and the null then falls to
     * {@link org.jmouse.mapper.config.NullHandlingPolicy} — so what happens is whatever the application
     * configured for the whole mapping: clear the property, skip it, or write an empty value.</p>
     *
     * <p>{@code Guarded} does not produce a value at all. The target property keeps whatever it already
     * held, whatever the policy says, and that is an outcome no conditional expression can express —
     * a ternary must yield something, and inventing a false branch is exactly what a caller reaches for
     * this to avoid.</p>
     *
     * <p>⚠️ They are not two names for one idea and neither replaces the other. {@code When} is the
     * right shape when the false case has a value; this is the right shape when it does not. Changing
     * {@code When} to behave like this would silently alter every mapping already written against it —
     * from writing a null to leaving a property untouched, with nothing raised either way.</p>
     *
     * @param targetName target property name
     * @param condition  evaluated against the source; true means write
     * @param delegate   what produces the value when the condition holds
     */
    record Guarded(String targetName, BiPredicate<Object, MappingContext> condition, PropertyMapping delegate)
            implements PropertyMapping {
        public Guarded {
            Verify.nonNull(condition, "condition");
            Verify.nonNull(delegate, "delegate");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * A computed mapping that also remembers <strong>how it was written</strong>.
     *
     * <h2>⚠️ Why the text is carried beside the function</h2>
     *
     * <p>{@link Compute} holds a function and nothing else, which is everything the engine needs and
     * nothing anybody else can read. A mapping that came from a declaration — a text file, a screen, a
     * stored document — has a source form, and once it is compiled into a lambda that form is gone:
     * rendering the mapping back, showing it to a person, or diffing two of them all become impossible,
     * and the only remaining spelling is "a function".</p>
     *
     * <p>So the expression travels with its own text. Nothing in the engine reads it — evaluation goes
     * through {@code function} exactly as {@code Compute} does — and everything outside the engine that
     * needs to show or re-render the mapping reads {@code source} instead of guessing.</p>
     *
     * <p>⚠️ Deliberately a {@link String} and a function rather than a parsed tree: the engine must not
     * acquire a dependency on an expression language, and a mapping declared in one language should not
     * be unreadable to a product using another.</p>
     *
     * @param targetName target property name
     * @param source     the expression exactly as it was written
     * @param function   what evaluates it
     */
    record Expression(String targetName, String source, ComputeFunction<Object> function)
            implements PropertyMapping {
        public Expression {
            Verify.nonNull(source, "source");
            Verify.nonNull(function, "function");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Composite mapping that tries multiple candidates in order.
     */
    record Coalesce(String targetName, List<PropertyMapping> candidates) implements PropertyMapping {
        public Coalesce {
            Verify.state(!candidates.isEmpty(), "candidates");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Decorator mapping that enforces presence of a value.
     */
    record Required(String targetName, PropertyMapping delegate, String errorCode, String message)
            implements PropertyMapping {
        public Required {
            Verify.nonNull(delegate, "delegate");
            Verify.notBlank(errorCode, "errorCode");
            Verify.notBlank(message, "message");
        }

        @Override
        public <R> R accept(PropertyMappingVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }
}
