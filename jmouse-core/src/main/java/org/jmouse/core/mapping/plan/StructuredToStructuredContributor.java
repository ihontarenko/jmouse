package org.jmouse.core.mapping.plan.contrib;

import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.model.SourceModel;
import org.jmouse.core.mapping.model.TargetModel;
import org.jmouse.core.mapping.plan.MappingPlan;
import org.jmouse.core.mapping.plan.StructuredMappingPlan;
import org.jmouse.core.mapping.plan.spi.MappingPlanContributor;
import org.jmouse.core.mapping.plan.spi.PlanBuildContext;
import org.jmouse.core.mapping.runtime.MappingContext;
import org.jmouse.core.mapping.values.ValueKind;
import org.jmouse.core.reflection.TypeClassifier;

import java.util.ArrayList;
import java.util.List;

public final class StructuredToStructuredContributor implements MappingPlanContributor {

    @Override
    public boolean supports(SourceModel source, TargetModel target) {
        return target.kind() == ValueKind.JAVA_BEAN || target.kind() == ValueKind.VALUE_OBJECT;
    }

    @Override
    public MappingPlan<?> build(SourceModel source, TargetModel target, PlanBuildContext buildContext) {
        List<StructuredMappingPlan.Step> steps = new ArrayList<>();

        for (PropertyDescriptor<?> p : target.descriptor().getProperties().values()) {
            String name = p.getName();
            Class<?> targetFieldType = p.getType().getJavaType().getRawType();

            steps.add((src, session, ctx) -> {
                if (!src.has(name)) {
                    // virtual fields hook stays in 3.1 (wired via ctx.policy().virtualFieldPolicy() + ctx.virtualResolver in 3.2/3.3)
                    return;
                }

                Object raw = src.read(name);
                Object value = adapt(raw, targetFieldType, ctx, buildContext.types());

                if (session.instance() != null) {
                    if (p.isWritable()) {
                        session.write(p, value);
                    }
                } else {
                    session.putConstructorArgument(name, value);
                }
            });
        }

        return new StructuredMappingPlan(target, steps);
    }

    private Object adapt(Object raw, Class<?> targetType, MappingContext ctx, TypeClassifier types) {
        if (raw == null) return null;
        if (targetType.isInstance(raw)) return raw;

        ValueKind sourceKind = types.kindOfValue(raw);
        ValueKind targetKind = types.kindOfType(targetType, null);

        if (types.isStructured(sourceKind) && types.isStructured(targetKind)) {
            // deep mapping: map any structured source (Map/Bean/Record) to structured target
            return ctx.mapper().map(raw, targetType);
        }

        try {
            return ctx.conversion().convert(raw, targetType);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Conversion failed: " + raw.getClass().getName() + " -> " + targetType.getName(), ex);
        }
    }
}
