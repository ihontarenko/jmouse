package org.jmouse.meterializer.hooks;

/**
 * Pipeline stages that can be intercepted.
 */
public enum RenderingStage {
    BEFORE_TEMPLATE_RESOLVE,
    AFTER_TEMPLATE_RESOLVE,
    BEFORE_TRANSFORM,
    AFTER_TRANSFORM,
    BEFORE_MATERIALIZE,
    AFTER_MATERIALIZE
}
