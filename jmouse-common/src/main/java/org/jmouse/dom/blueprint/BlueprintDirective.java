package org.jmouse.dom.blueprint;

import java.util.Map;

public sealed interface BlueprintDirective
        permits BlueprintDirective.SetAttributeIf,
                BlueprintDirective.RemoveAttributeIf,
                BlueprintDirective.AddClassIf,
                BlueprintDirective.WrapIf,
                BlueprintDirective.OmitIf {

    record SetAttributeIf(BlueprintPredicate predicate, String name, BlueprintValue value)
            implements BlueprintDirective {
    }

    record RemoveAttributeIf(BlueprintPredicate predicate, String attributeName)
            implements BlueprintDirective {
    }

    record AddClassIf(BlueprintPredicate predicate, BlueprintValue classValue)
            implements BlueprintDirective {
    }

    record WrapIf(BlueprintPredicate predicate, String wrapperTagName, Map<String, BlueprintValue> wrapperAttributes)
            implements BlueprintDirective {
    }

    record OmitIf(BlueprintPredicate predicate)
            implements BlueprintDirective {
    }

}