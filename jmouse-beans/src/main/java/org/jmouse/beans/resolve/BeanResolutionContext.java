package org.jmouse.beans.resolve;

import org.jmouse.beans.BeanContext;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import static org.jmouse.core.Verify.nonNull;

/**
 * Immutable bean resolution input. 📦
 *
 * <p>Can be created either from a reflective {@link Parameter}
 * or from explicit type information.</p>
 *
 * @param beanContext bean context
 * @param parameter   reflective parameter, if available
 * @param type        declared type to resolve
 * @param required    whether resolution is mandatory
 */
public record BeanResolutionContext(
        BeanContext beanContext,
        Parameter parameter,
        Type type,
        boolean required
) {

    public BeanResolutionContext {
        nonNull(beanContext, "beanContext");
        nonNull(type, "type");
    }

    /**
     * Create context from a reflective method or constructor parameter. 🔎
     *
     * @param beanContext bean context
     * @param parameter   reflective parameter
     * @param required    whether resolution is mandatory
     *
     * @return resolution context
     */
    public static BeanResolutionContext forParameter(BeanContext beanContext, Parameter parameter, boolean required) {
        return new BeanResolutionContext(beanContext, parameter, parameter.getParameterizedType(), required);
    }

    /**
     * Create context from an explicit type. 🧾
     *
     * @param beanContext bean context
     * @param type        declared type
     * @param required    whether resolution is mandatory
     *
     * @return resolution context
     */
    public static BeanResolutionContext forType(BeanContext beanContext, Type type, boolean required) {
        return new BeanResolutionContext(beanContext, null, type, required);
    }

}