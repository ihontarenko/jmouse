package org.jmouse.el.evaluation;

import org.jmouse.core.bind.DefaultPropertyValueResolver;
import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.VirtualPropertyResolver;

public class ExpressionLanguageValuesResolver extends DefaultPropertyValueResolver {

    public ExpressionLanguageValuesResolver(ObjectAccessor accessor, VirtualPropertyResolver resolver) {
        super(accessor, resolver);
    }

    public ExpressionLanguageValuesResolver(ObjectAccessor accessor) {
        super(accessor, new VirtualPropertyResolver.Default());
    }

}
