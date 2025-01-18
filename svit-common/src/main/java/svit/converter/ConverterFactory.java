package svit.converter;

public interface ConverterFactory {

    void registerConverter(Converter<?, ?> converter);

    <S, T> Converter<S, T> getConverter(Class<S> sourceType, Class<T> targetType);

}
