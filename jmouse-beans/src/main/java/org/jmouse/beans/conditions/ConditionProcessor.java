package org.jmouse.beans.conditions;

import java.lang.reflect.AnnotatedElement;

public interface ConditionProcessor {

    ConditionalMetadata process(AnnotatedElement element);

    boolean supports(AnnotatedElement element);

}
