package org.jmouse.meterializer;

import java.util.Map;

public sealed interface NodeDirective
        permits NodeDirective.SetAttributeIf,
                NodeDirective.RemoveAttributeIf,
                NodeDirective.AddClassIf,
                NodeDirective.WrapIf,
                NodeDirective.OmitIf,
                NodeDirective.ApplyAttributes {

    record SetAttributeIf(TemplatePredicate predicate, String name, ValueExpression value)
            implements NodeDirective {
    }

    record RemoveAttributeIf(TemplatePredicate predicate, String attributeName)
            implements NodeDirective {
    }

    record AddClassIf(TemplatePredicate predicate, ValueExpression classValue)
            implements NodeDirective {
    }

    record WrapIf(TemplatePredicate predicate, String wrapperTagName, Map<String, ValueExpression> wrapperAttributes)
            implements NodeDirective {
    }

    record OmitIf(TemplatePredicate predicate)
            implements NodeDirective {
    }

    record ApplyAttributes(ValueExpression source, String keyValue, String valueKey)
            implements NodeDirective {
    }

}