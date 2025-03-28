package org.jmouse.el.evaluation;

import org.jmouse.core.bind.VirtualPropertyResolver;
import org.jmouse.core.convert.Conversion;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.rendering.TemplateStack;

import java.util.HashMap;
import java.util.Map;

public class DefaultEvaluationContext implements EvaluationContext {

    private final ScopedChain             chain;
    private final ExtensionContainer      extensions;
    private final Conversion              conversion;
    private final Map<Object, Object>     objects;
    private final TemplateStack           stack;
    private       VirtualPropertyResolver resolver;

    public DefaultEvaluationContext(ScopedChain chain, ExtensionContainer extensions, Conversion conversion) {
        this.chain = chain;
        this.extensions = extensions;
        this.conversion = conversion;
        this.resolver = new VirtualPropertyResolver.Default();
        this.objects = new HashMap<>();
        this.stack = TemplateStack.empty();
    }

    public DefaultEvaluationContext(ExtensionContainer extensions) {
        this(new BasicValuesChain(), extensions, new ExpressionLanguageConversion());
    }

    public DefaultEvaluationContext() {
        this(new BasicValuesChain(), new StandardExtensionContainer(), new ExpressionLanguageConversion());
    }

    @Override
    public TemplateStack getInheritance() {
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
    public ExtensionContainer getExtensions() {
        return extensions;
    }

    @Override
    public Conversion getConversion() {
        return conversion;
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
