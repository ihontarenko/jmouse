package org.jmouse.beans.conditions;

import java.util.Set;

public interface ConditionConfiguration {

    Set<Class<? extends BeanCondition>> getImplementations();



}
