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
     * 🔍 Find a suitable {@link HttpMessageConverter} for the given value.
     *
     * <p>Delegates to {@link #getMessageConverter(Class, String)} by inferring
     * the value type from the actual runtime class of the object.</p>
     *
     * <h3>Resolution steps:</h3>
     * <ol>
     *   <li>Detects the value type (or {@code null} if value is {@code null}).</li>
     *   <li>Iterates over registered converters in order.</li>
     *   <li>Checks each converter with {@link HttpMessageConverter#isWritable(Class, org.jmouse.core.MediaType)}.</li>
     *   <li>Returns the first compatible converter found.</li>
     * </ol>
     *
     * <p>⚠️ If no converter is found, {@code null} is returned. In such cases,
     * callers are expected to handle this gracefully or raise an
     * {@link UnsuitableException}.</p>
     *
     * @param <T>         the value type
     * @param value       the object to be written (may be {@code null})
     * @param contentType the desired media type as a string
     * @return a matching {@link HttpMessageConverter}, or {@code null} if none found
     */
    @SuppressWarnings("unchecked")
    public <T> HttpMessageConverter<T> getMessageConverter(T value, String contentType) {
        return getMessageConverter(value != null ? value.getClass() : null, contentType);
    }

    /**
     * 🎯 Find a suitable {@link HttpMessageConverter} for the given type and media type.
     *
     * <p>Iterates through all configured converters and returns the first one
     * that reports itself as writable for the given combination.</p>
     *
     * @param <T>        the expected value type
     * @param valueType  the runtime class of the value (may be {@code null})
     * @param contentType the target media type as a string (never {@code null})
     * @return a matching {@link HttpMessageConverter}, or {@code null} if none found
     */
    @SuppressWarnings("unchecked")
    public <T> HttpMessageConverter<T> getMessageConverter(Class<?> valueType, String contentType) {
        HttpMessageConverter<T> messageConverter = null;

        for (HttpMessageConverter<?> converter : converters) {
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
