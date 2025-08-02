package org.jmouse.mvc.routing;

/**
 * Composite route condition (path + method + media type etc).
 */
public interface MappingCondition extends MappingMatcher, Comparable<MappingCondition> {

    /**
     * Combine this condition with another.
     * Useful during route registration merge.
     */
    MappingCondition combine(MappingCondition that);

}
