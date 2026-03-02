package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

/**
 * Module-level wiring helper for EL-based constraint definitions. 🔌
 *
 * <p>
 * {@code ConstraintELModule} centralizes integration between:
 * </p>
 * <ul>
 *     <li>{@link ExpressionLanguage} (EL engine),</li>
 *     <li>{@link ConstraintTypeRegistry} (constraint type lookup),</li>
 *     <li>{@link Mapper} (definition → constraint mapping).</li>
 * </ul>
 *
 * <p>
 * It registers the {@link ValidatorDefinitionParser} into the EL engine
 * and constructs a ready-to-use {@link ConstraintExpressionSupport} facade.
 * </p>
 *
 * <h3>What this method does</h3>
 * <ol>
 *     <li>Registers EL parser for validation definitions.</li>
 *     <li>Creates {@link ConstraintExpressionAdapter} using registry + mapper.</li>
 *     <li>Returns {@link ConstraintExpressionSupport} facade.</li>
 * </ol>
 *
 * <h3>Typical usage</h3>
 *
 * <pre>{@code
 * ConstraintExpressionSupport support =
 *     ConstraintELModule.create(expressionLanguage, registry, mapper);
 *
 * ValidationDefinition definition =
 *     support.parse("@min(min=18, message='Should be min 18')");
 * }</pre>
 *
 * <p>
 * This method should be called once during application bootstrap.
 * </p>
 */
public final class ConstraintELModule {

    private ConstraintELModule() {}

    /**
     * Performs EL module wiring.
     *
     * <p>
     * Responsibilities:
     * </p>
     * <ul>
     *     <li>Registers validation definition parser into EL engine.</li>
     *     <li>Creates adapter for constraint instantiation.</li>
     *     <li>Returns support facade.</li>
     * </ul>
     *
     * @param expressionLanguage EL engine instance
     * @param registry           constraint type registry
     * @param mapper             object mapper for definition binding
     * @return configured {@link ConstraintExpressionSupport}
     */
    public static ConstraintExpressionSupport create(
            ExpressionLanguage expressionLanguage,
            ConstraintTypeRegistry registry,
            Mapper mapper
    ) {
        expressionLanguage.getExtensions().addParser(new ValidatorDefinitionParser());

        ConstraintExpressionAdapter adapter =
                new ConstraintExpressionAdapter(registry, mapper);

        return new ConstraintExpressionSupport(expressionLanguage, adapter);
    }
}