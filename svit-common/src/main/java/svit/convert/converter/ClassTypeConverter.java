package svit.convert.converter;

import svit.convert.ClassPair;
import svit.convert.GenericConverter;

import java.util.Set;

public class ClassTypeConverter implements GenericConverter<Class<?>, Class<?>> {

    @Override
    public Class<?> convert(Class<?> source, Class<Class<?>> sourceType, Class<Class<?>> targetType) {
        Class<?> type = source;

        if (source.isEnum()) {
            type = Enum.class;
        }

        return type;
    }

    @Override
    public Set<ClassPair<? extends Class<?>, ? extends Class<?>>> getSupportedTypes() {
        return Set.of(new ClassPair<>((Class<Class<?>>)(Class<?>)Class.class, (Class<Class<?>>)(Class<?>)Class.class));
    }

}
