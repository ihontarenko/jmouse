package org.jmouse.dom.blueprint.hooks;

/**
 * Pipeline stages that can be intercepted.
 */
public enum RenderingStage {
    BEFORE_BLUEPRINT_RESOLVE,
    AFTER_BLUEPRINT_RESOLVE,
    BEFORE_TRANSFORM,
    AFTER_TRANSFORM,
    BEFORE_MATERIALIZE,
    AFTER_MATERIALIZE
}
