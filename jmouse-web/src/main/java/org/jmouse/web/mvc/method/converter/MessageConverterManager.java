package org.jmouse.web.mvc.method.converter;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.core.MediaType;
import org.jmouse.core.Sorter;
import org.jmouse.web.context.WebBeanContext;

import java.util.ArrayList;
import java.util.List;

/**
 * ⚙️ Manages a collection of {@link HttpMessageConverter} instances.
 *
 * <p>Supports registering converters and selecting an appropriate converter
 * for writing a given value with a specified content type.</p>
 *
 * <p>Automatically loads and sorts converters from the {@link WebBeanContext}
 * during initialization.</p>
 *
 * @author Ivan Hontarenko
 */
public class MessageConverterManager implements InitializingBean {

    private final List<HttpMessageConverter<?>> converters = new ArrayList<>();

    /**
     * ➕ Registers a new {@link HttpMessageConverter}.
     *
     * @param converter converter instance to register
     */
    public void register(HttpMessageConverter<?> converter) {
        converters.add(converter);
    }

    /**
     * 🔍 Finds a suitable {@link HttpMessageConverter} to write the given value
     * with the specified content type.
     *
     * @param <T>         the value type
     * @param value       the object to write
     * @param contentType the media type as a string
     * @return a compatible {@link HttpMessageConverter} for writing
     * @throws UnsuitableException if no suitable converter is found
     */
    @SuppressWarnings("unchecked")
    public <T> HttpMessageConverter<T> getMessageConverter(T value, String contentType) {
        HttpMessageConverter<T> messageConverter = null;

        for (HttpMessageConverter<?> converter : converters) {
            Class<?> valueType = value != null ? value.getClass() : null;
            if (converter.isWritable(valueType, MediaType.forString(contentType))) {
                messageConverter = (HttpMessageConverter<T>) converter;
                break;
            }
        }

        return messageConverter;
    }

    /**
     * 📋 Returns the list of supported media types.
     *
     * @return supported {@link MediaType}s
     */
    public List<MediaType> getSupportedMediaTypes() {
        List<MediaType> mediaTypes = new ArrayList<>();

        for (HttpMessageConverter<?> messageConverter : converters) {
            mediaTypes.addAll(messageConverter.getSupportedMediaTypes());
        }

        if (!mediaTypes.isEmpty()) {
            mediaTypes = mediaTypes.stream().distinct().toList();
        }

        return mediaTypes;
    }

    /**
     * 🔄 Loads all {@link HttpMessageConverter} beans from the context,
     * sorts them by priority, and registers them internally.
     *
     * @param context the current bean context
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void afterCompletion(BeanContext context) {
        List<HttpMessageConverter>    converters        = WebBeanContext.getLocalBeans(
                HttpMessageConverter.class, (WebBeanContext) context);
        List<HttpMessageConverter<?>> messageConverters = (List<HttpMessageConverter<?>>) (List<?>) converters;

        messageConverters = new ArrayList<>(messageConverters);
        Sorter.sort(messageConverters);

        this.converters.addAll(messageConverters);
    }
}
