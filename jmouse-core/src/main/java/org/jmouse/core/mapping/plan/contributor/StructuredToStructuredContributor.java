package org.jmouse.core.mapping.plan.contrib;

import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.bindings.*;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.model.SourceModel;
import org.jmouse.core.mapping.model.SourceModelFactory;
import org.jmouse.core.mapping.model.TargetModel;
import org.jmouse.core.mapping.model.TargetSession;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.StructuredMappingPlan;
import org.jmouse.core.mapping.plan.build.MappingPlanContributor;
import org.jmouse.core.mapping.plan.build.PlanBuildContext;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.mapping.values.ValueKind;
import org.jmouse.core.reflection.TypeInformation;

import java.util.ArrayList;
import java.util.List;

public final class StructuredToStructuredContributor implements MappingPlanContributor {

    @Override
    public boolean supports(SourceModel source, TargetModel target) {
        return target.kind() == ValueKind.JAVA_BEAN || target.kind() == ValueKind.VALUE_OBJECT;
    }

    @Override
    public MappingPlan<?> build(SourceModel source, TargetModel target, PlanBuildContext context) {
        List<StructuredMappingPlan.Step> steps = new ArrayList<>();

        SourceModelFactory sourceFactory = new SourceModelFactory();
        SourcePathReader   pathReader    = new SourcePathReader(sourceFactory);

        TypeMappingRules rules = context.mappingConfig().rules().find(source.sourceType(), target.targetType());

        for (PropertyDescriptor<?> targetProperty : target.descriptor().getProperties().values()) {
            String   targetName      = targetProperty.getName();
            Class<?> targetFieldType = targetProperty.getType().getClassType();

            FieldRule rule = (rules == null ? null : rules.findRule(targetName));

            steps.add((sourceModel, targetSession, mappingContext) -> {
                if (rule != null) {
                    Object value = resolveRawValue(rule, sourceModel, mappingContext, pathReader);

                    if (rule.type() == FieldRuleType.IGNORE) {
                        return;
                    }

                    Object adapted = adaptValue(value, targetFieldType, mappingContext);
                    write(targetSession, targetProperty, targetName, adapted);
                    return;
                }

                // default same-name mapping
                if (!sourceModel.has(targetName)) {
                    return;
                }

                Object value   = sourceModel.read(targetName);
                Object adapted = adaptValue(value, targetFieldType, mappingContext);

                write(targetSession, targetProperty, targetName, adapted);
            });
        }

        return new StructuredMappingPlan(target, steps, sourceFactory);
    }

    private Object resolveRawValue(FieldRule rule,
                                   SourceModel sourceModel,
                                   MappingContext context,
                                   SourcePathReader pathReader) {

        return switch (rule.type()) {
            case IGNORE -> null;

            case CONSTANT -> rule.constantValue();

            case COMPUTE -> {
                FieldComputeFunction function = rule.computeFunction();

                if (function == null) {
                    throw new MappingException("invalid_compute", "Compute rule requires function for target: " + rule.targetName());
                }

                yield function.compute(sourceModel.source(), context);
            }

            case BIND -> {
                // 1) provider binder
                if (rule.valueProvider() != null) {
                    try {
                        yield rule.valueProvider().provide(sourceModel.source());
                    } catch (Exception ex) {
                        throw new MappingException("binding_provider_failed",
                                                   "Binding provider failed for target: " + rule.targetName(), ex);
                    }
                }

                // 2) name/path binder
                String ref = rule.sourceReference();
                if (ref == null || ref.isBlank()) {
                    throw new MappingException("invalid_binding", "Binding requires sourceReference or provider for target: " + rule.targetName());
                }

                if (isReferencePath(ref)) {
                    yield pathReader.read(sourceModel.source(), ref);
                }

                if (!sourceModel.has(ref)) {
                    yield null;
                }

                yield sourceModel.read(ref);
            }
        };
    }

    private boolean isReferencePath(String reference) {
        return reference.indexOf('.') >= 0 || reference.indexOf('[') >= 0;
    }

    private void write(TargetSession session, PropertyDescriptor<?> property, String name, Object value) {
        if (session.isRecord()) {
            session.putConstructorArgument(name, value);
        } else {
            session.write(property, value);
        }
    }

    private Object adaptValue(Object rawValue, Class<?> targetType, MappingContext context) {
        if (rawValue == null) return null;
        if (targetType.isInstance(rawValue)) return rawValue;

        ValueKind sourceKind = ValueKind.ofClassifier(TypeInformation.forClass(rawValue.getClass()));
        ValueKind targetKind = ValueKind.ofClassifier(TypeInformation.forClass(targetType));

        boolean sourceStructured = sourceKind == ValueKind.MAP || sourceKind == ValueKind.JAVA_BEAN || sourceKind == ValueKind.VALUE_OBJECT;
        boolean targetStructured = targetKind == ValueKind.JAVA_BEAN || targetKind == ValueKind.VALUE_OBJECT;

        if (sourceStructured && targetStructured) {
            return context.mapper().map(rawValue, targetType);
        }

        try {
            return context.conversion().convert(rawValue, targetType);
        } catch (Exception ex) {
            throw new MappingException("conversion_failed",
                                       "Conversion failed: " + rawValue.getClass().getName() + " -> " + targetType.getName(), ex);
        }
    }
}
