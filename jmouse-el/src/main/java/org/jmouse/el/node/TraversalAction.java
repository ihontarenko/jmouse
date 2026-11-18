package org.jmouse.el.node;

/** Control returned on entry to a structural node. */
public enum TraversalAction {
    CONTINUE,
    SKIP_CHILDREN,
    STOP
}
