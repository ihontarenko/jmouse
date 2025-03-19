package org.jmouse.el.evaluation;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;

public class DefaultEvaluationContext implements EvaluationContext {

    private final ScopedChain chain;
    private final ExtensionContainer extensions;
    private final Conversion conversion;

    public DefaultEvaluationContext(ScopedChain chain, ExtensionContainer extensions, Conversion conversion) {
        this.chain = chain;
        this.extensions = extensions;
        this.conversion = conversion;
    }

    public DefaultEvaluationContext() {
        this(new BasicValuesChain(), new StandardExtensionContainer(), new ELConversion());
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

}
