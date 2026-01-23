package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.ValueObject;
import org.jmouse.core.bind.JavaBean;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.mapping.factory.ObjectFactory;
import org.jmouse.core.mapping.factory.NonArgumentConstructorObjectFactory;
import org.jmouse.core.mapping.values.ValueKind;
import org.jmouse.core.reflection.TypeInformation;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TargetModelFactory {

    public TargetModel forType(Class<?> targetType) {
        TypeInformation information = TypeInformation.forClass(targetType);

        if (information.isRecord()) {
            return new RecordTargetModel(targetType);
        }
        if (information.isBean()) {
            return new BeanTargetModel(targetType);
        }

        throw new IllegalArgumentException("Unsupported target kind in Stage 3.1: " + ValueKind.ofClassifier(information));
    }

    private static final class BeanTargetModel implements TargetModel {

        private final Class<?>            type;
        private final ObjectDescriptor<?> descriptor;
        private final ObjectFactory<?>    factory;

        BeanTargetModel(Class<?> type) {
            this.type = type;
            this.descriptor = JavaBean.of(type).getDescriptor();
            this.factory = NonArgumentConstructorObjectFactory.forClass(type);
        }

        public Class<?> targetType() {
            return type;
        }

        public ValueKind kind() {
            return ValueKind.JAVA_BEAN;
        }

        public ObjectDescriptor<?> descriptor() {
            return descriptor;
        }

        public TargetSession newSession() {
            Object instance = factory.create();
            return new TargetSession() {

                public Object instance() {
                    return instance;
                }

                public void write(PropertyDescriptor<Object> descriptor, Object value) {
                    descriptor.getAccessor().writeValue(instance, value);
                }

                public void putConstructorArgument(String name, Object value) {
                    throw new UnsupportedOperationException();
                }

                public Object build() {
                    return instance;
                }

            };
        }
    }

    private static final class RecordTargetModel implements TargetModel {

        private final Class<?>                      type;
        private final ValueObject<? extends Record> vo;
        private final ObjectDescriptor<?>           descriptor;

        RecordTargetModel(Class<?> type) {
            this.type = type;
            this.vo = ValueObject.of((Class<? extends Record>) type);
            this.descriptor = vo.getDescriptor();
        }

        public Class<?> targetType() {
            return type;
        }

        public ValueKind kind() {
            return ValueKind.VALUE_OBJECT;
        }

        public ObjectDescriptor<?> descriptor() {
            return descriptor;
        }

        public TargetSession newSession() {

            Map<String, Object> arguments = new LinkedHashMap<>();

            return new TargetSession() {

                public Object instance() {
                    return null;
                }

                public void write(PropertyDescriptor<Object> p, Object value) {
                    throw new UnsupportedOperationException();
                }

                public void putConstructorArgument(String name, Object value) {
                    arguments.put(name, value);
                }

                public Object build() {
                    var values = vo.getRecordValues();
                    arguments.forEach(values::set);
                    return vo.getInstance(values).create();
                }

            };
        }
    }
}
