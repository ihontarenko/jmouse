package org.jmouse.core.mapping.binding;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.MappingContext;

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
        PropertyMapping.Coalesce,
        PropertyMapping.Required {

    // ===== static factories (ergonomic) =====

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

    // ===== mapping node types =====

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
