package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.ValueObject;
import org.jmouse.core.bind.descriptor.structured.DescriptorResolver;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.factory.NonArgumentConstructorObjectFactory;
import org.jmouse.core.mapping.values.ValueKind;
import org.jmouse.core.reflection.TypeInformation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TargetModelFactory {

    public TargetModel forType(Class<?> targetType) {
        Objects.requireNonNull(targetType, "targetType");

        TypeInformation information = TypeInformation.forClass(targetType);
        ValueKind kind = ValueKind.ofClassifier(information);

        if (kind == ValueKind.VALUE_OBJECT) {
            return new RecordTargetModel(targetType);
        }
        if (kind == ValueKind.JAVA_BEAN) {
            return new BeanTargetModel(targetType);
        }

        throw new MappingException("unsupported_target_type", "Unsupported target type: " + targetType.getName());
    }

    private static final class BeanTargetModel implements TargetModel {

        private final Class<?> type;
        private final ObjectDescriptor<?> descriptor;

        BeanTargetModel(Class<?> type) {
            this.type = type;
            this.descriptor = DescriptorResolver.ofBeanType(type);
        }

        @Override public Class<?> targetType() { return type; }

        @Override public ValueKind kind() { return ValueKind.JAVA_BEAN; }

        @Override public ObjectDescriptor<?> descriptor() { return descriptor; }

        @Override
        public TargetSession newSession() {

            Object instance = NonArgumentConstructorObjectFactory.forClass(type).create();

            return new TargetSession() {

                @Override
                public boolean isRecord() {
                    return false;
                }

                @Override
                public Object instance() {
                    return instance;
                }

                @Override
                public void write(PropertyDescriptor<?> property, Object value) {
                    if (property.isWritable()) {
                        property.getAccessor().writeValue(instance, value);
                    }
                }

                @Override
                public void putConstructorArgument(String name, Object value) {
                    throw new UnsupportedOperationException("Bean target does not support constructor arguments");
                }

                @Override
                public Object build() {
                    return instance;
                }
            };
        }
    }

    private static final class RecordTargetModel implements TargetModel {

        private final Class<? extends Record>       type;
        private final ValueObject<? extends Record> valueObject;
        private final ObjectDescriptor<?>           descriptor;

        @SuppressWarnings("unchecked")
        RecordTargetModel(Class<?> type) {
            this.type = (Class<? extends Record>) type;
            this.valueObject = ValueObject.of(this.type);
            this.descriptor = DescriptorResolver.ofRecordType(this.type);
        }

        @Override public Class<?> targetType() { return type; }

        @Override public ValueKind kind() { return ValueKind.VALUE_OBJECT; }

        @Override public ObjectDescriptor<?> descriptor() { return descriptor; }

        @Override
        public TargetSession newSession() {

            Map<String, Object> arguments = new LinkedHashMap<>();

            return new TargetSession() {

                @Override
                public boolean isRecord() {
                    return true;
                }

                @Override
                public Object instance() {
                    return null;
                }

                @Override
                public void write(PropertyDescriptor<?> property, Object value) {
                    throw new UnsupportedOperationException("Record target does not support property writes");
                }

                @Override
                public void putConstructorArgument(String name, Object value) {
                    arguments.put(name, value);
                }

                @Override
                public Object build() {
                    var values = valueObject.getRecordValues();
                    arguments.forEach(values::set);
                    return valueObject.getInstance(values).create();
                }
            };
        }
    }
}
