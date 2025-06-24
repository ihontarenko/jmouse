package org.jmouse.beans.conditions;

import org.jmouse.beans.BeanContext;

/**
 * Base contract for evaluating conditions before bean registration.
 */
public interface BeanCondition {
    boolean match(ConditionalMetadata metadata, BeanContext context);
}
