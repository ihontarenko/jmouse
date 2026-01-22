package org.jmouse.core.mapping.records;

import org.jmouse.core.bind.ValueObject;

import static org.jmouse.core.Verify.nonNull;

/**
 * Record instantiation backed by the existing {@link ValueObject} infrastructure.
 */
public final class RecordFactory<T extends Record> {

    private final ValueObject<T> valueObject;

    public RecordFactory(Class<T> recordType) {
        this.valueObject = ValueObject.of(nonNull(recordType, "recordType"));
    }

    public T create(ConstructorArguments arguments) {
        ValueObject.Values values = valueObject.getRecordValues();

        for (var entry : arguments.asMap().entrySet()) {
            values.set(entry.getKey(), entry.getValue());
        }

        return valueObject.getInstance(values).create();
    }
}
