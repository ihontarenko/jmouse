package org.jmouse.dom.template;

import java.util.Map;

public sealed interface NodeDirective
        permits NodeDirective.SetAttributeIf,
                NodeDirective.RemoveAttributeIf,
                NodeDirective.AddClassIf,
                NodeDirective.WrapIf,
                NodeDirective.OmitIf {

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

}