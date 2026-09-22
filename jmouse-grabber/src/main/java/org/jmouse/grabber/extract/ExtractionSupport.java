package org.jmouse.grabber.extract;

import java.util.Map;

import org.jmouse.core.access.TypedValue;
import org.jmouse.core.binding.Bind;
import org.jmouse.core.binding.BinderConversion;
import org.jmouse.core.convert.Conversion;

/**
 * 🧰 The two library services extraction leans on, in one place.
 *
 * <p>⚠️ This class is deliberately thin, and that is the point of the design: the grabber decides
 * <b>what text is where</b>, and the library decides <b>what type it becomes</b> and <b>how it
 * becomes an object</b>. A grabber holding its own type coercion has re-implemented
 * {@link Conversion}; one instantiating the caller's record itself has re-implemented
 * {@code ValueObjectBinder}.</p>
 */
public final class ExtractionSupport {

    private static final ExtractionSupport DEFAULT = new ExtractionSupport(new BinderConversion());

    private final Conversion conversion;

    public ExtractionSupport(Conversion conversion) {
        this.conversion = conversion;
    }

    /**
     * 🧰 The standard set — {@code BinderConversion}, which is the same converter set the binder
     * itself uses, so a value converted here and a value the binder converts behave identically. It
     * already handles text to a number, to a {@code URI}, to a date and time, and to an enum.
     */
    public static ExtractionSupport standard() {
        return DEFAULT;
    }

    /**
     * 🔁 Converts a value to the requested type, leaving it alone when it is already one.
     */
    public Object convert(Object value, Class<?> targetType) {
        if (targetType.isInstance(value)) {
            return value;
        }

        return conversion.convert(value, targetType);
    }

    /**
     * 🏗️ Turns the extracted map into the caller's own type.
     *
     * <p>⚠️ A record binds by its component names, so those are what the field names must match — or
     * {@code @BindName} on the component. This is the first thing that will confuse somebody, and it
     * is a property of the binder rather than of the grabber.</p>
     */
    public <T> T bind(Map<String, Object> values, Class<T> type) {
        if (Map.class.isAssignableFrom(type)) {
            return type.cast(values);
        }

        return Bind.with(values).to(TypedValue.of(type)).getValue();
    }

    public Conversion conversion() {
        return conversion;
    }

}
