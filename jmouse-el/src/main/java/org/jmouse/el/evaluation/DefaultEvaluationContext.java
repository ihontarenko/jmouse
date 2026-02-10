package org.jmouse.el.evaluation;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.VirtualPropertyResolver;
import org.jmouse.core.access.AttributeResolver;
import org.jmouse.core.convert.Conversion;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.renderable.Inheritance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultEvaluationContext implements EvaluationContext {

    private final Conversion              conversion;
    private final Map<Object, Object>     objects;
    private final Inheritance             stack;
    private       ExtensionContainer      extensions;
    private       ScopedChain             chain;
    private       VirtualPropertyResolver resolver;
    private       ObjectAccessor          accessor;

    public DefaultEvaluationContext(ScopedChain chain, ExtensionContainer extensions, Conversion conversion) {
        this.chain = chain;
        this.extensions = extensions;
        this.conversion = conversion;
        this.resolver = new VirtualPropertyResolver.Default();
        this.objects = new HashMap<>();
        this.stack = Inheritance.empty();
    }

    public DefaultEvaluationContext(ExtensionContainer extensions) {
        this(new BasicValuesChain(), extensions, new ExpressionLanguageConversion());
    }

    public DefaultEvaluationContext() {
        this(new BasicValuesChain(), new StandardExtensionContainer(), new ExpressionLanguageConversion());
    }

    @Override
    public Inheritance getInheritance() {
        return stack;
    }

    @Override
    public Object getObject(Object key) {
        return objects.get(key);
    }

    @Override
    public void setObject(Object key, Object object) {
        objects.put(key, object);
    }

    @Override
    public ScopedChain getScopedChain() {
        return chain;
    }

    @Override
    public void setScopedChain(ScopedChain chain) {
        this.chain = chain;
        this.accessor = null;
    }

    @Override
    public ObjectAccessor getValueAccessor() {
        if (accessor == null) {
            accessor = new ScopedChainValuesAccessor(getScopedChain());
            ((AccessorWrapper.Aware) accessor).setWrapper(WRAPPER);
        }
        return accessor;
    }

    @Override
    public ExtensionContainer getExtensions() {
        return extensions;
    }

    @Override
    public void setExtensions(ExtensionContainer extensions) {
        this.extensions = extensions;
    }

    @Override
    public Conversion getConversion() {
        return conversion;
    }

    @Override
    public List<AttributeResolver> getAttributeResolvers() {
        return extensions.getAttributeResolvers();
    }

    /**
     * Returns the currently set VirtualPropertyResolver.
     *
     * @return the VirtualPropertyResolver instance
     */
    @Override
    public VirtualPropertyResolver getVirtualProperties() {
        return resolver;
    }

    /**
     * Sets the VirtualPropertyResolver.
     *
     * @param resolver the VirtualPropertyResolver instance to set
     */
    @Override
    public void setVirtualProperties(VirtualPropertyResolver resolver) {
        this.resolver = resolver;
    }
}
