package svit.converter;

import svit.reflection.ReflectType;
import svit.reflection.Reflections;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Conversion implements ConverterFactory {

    private Map<ClassPair<?, ?>, Converter<?, ?>> converters = new ConcurrentHashMap<>();

    @Override
    public void registerConverter(Converter<?, ?> converter) {
        ReflectType    reflectType = ReflectType.forType(converter.getClass()).navigate(Converter.class);
        List<Class<?>> types       = Reflections.getParameterizedTypes(reflectType.getType());

        if (types.size() != 2) {
            throw new IllegalArgumentException("Failed to register converter '%s'. Wrong size of types '%s'"
                    .formatted(Reflections.getShortName(converter), types));
        }

        ClassPair<?, ?> classPair = new ClassPair<>(types.getFirst(), types.getLast());

        converters.put(classPair, converter);
    }

    @Override
    public <S, T> Converter<S, T> getConverter(Class<S> sourceType, Class<T> targetType) {
        return (Converter<S, T>) converters.get(new ClassPair<>(sourceType, targetType));
    }

}
