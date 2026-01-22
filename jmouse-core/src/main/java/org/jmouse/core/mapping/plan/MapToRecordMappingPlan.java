package org.jmouse.core.mapping.plan;

import org.jmouse.core.Verify;
import org.jmouse.core.mapping.plan.support.MappingFailures;
import org.jmouse.core.mapping.records.ConstructorArguments;
import org.jmouse.core.mapping.records.RecordFactory;
import org.jmouse.core.mapping.runtime.MappingContext;

import java.util.List;

public final class MapToRecordMappingPlan<T extends Record> implements MappingPlan<T> {

    public interface RecordComponentOperation {
        void apply(Object source, ConstructorArguments arguments, MappingContext context);
    }

    private final List<String>                   requiredComponents;
    private final RecordFactory<T>               factory;
    private final List<RecordComponentOperation> operations;

    public MapToRecordMappingPlan(
            RecordFactory<T> factory, List<RecordComponentOperation> operations, List<String> requiredComponents
    ) {
        this.requiredComponents = Verify.nonNull(requiredComponents, "requiredComponents");
        this.factory = Verify.nonNull(factory, "factory");
        this.operations = Verify.nonNull(operations, "operations");
    }

    @Override
    public T execute(Object source, MappingContext context) {
        ConstructorArguments arguments = new ConstructorArguments();

        for (RecordComponentOperation operation : operations) {
            operation.apply(source, arguments, context);
        }

        // fail-fast required check (records are typically strict)
        for (String required : requiredComponents) {
            if (!arguments.contains(required)) {
                throw MappingFailures.fail(
                        "missing_record_component",
                        "Missing required record component: '" + required + "'",
                        null
                );
            }
        }

        return factory.create(arguments);
    }
}
