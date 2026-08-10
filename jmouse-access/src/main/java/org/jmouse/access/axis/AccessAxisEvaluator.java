package org.jmouse.access.axis;

import org.jmouse.access.AccessDecision;
import org.jmouse.access.AccessTarget;
import org.jmouse.access.AxisKind;
import org.jmouse.access.Subject;

/**
 * One of the five questions, as a bean of its own.
 *
 * <p>The engine holds the ordering and nothing else. Every axis is a separate implementation of this
 * interface for the reason stated as spec risk 2: a decision point that answers five questions inside
 * one class grows a sixth the first time somebody has a sixth, and by then there is nowhere to put it
 * except in the middle of the other five.
 *
 * <p>The order comes from the {@link AxisKind}'s own declaration rather than from {@code @Order}, so
 * that where an axis runs is decided once, where the axis is declared, and a new bean cannot quietly
 * land in the wrong slot.
 */
public interface AccessAxisEvaluator {

    /** Which question this bean answers. Also where it runs — the axis's order is the engine's. */
    AxisKind axis();

    /**
     * Answers for one subject, one permission and one target.
     *
     * <p>An axis that has nothing to say returns {@link AccessDecision#allowed()}. That is the common
     * case and it is not an oversight: an unscoped request has no workspace for axes 2 to 4 to be
     * about, and silence rather than refusal is the correct answer to a question that was not asked.
     */
    AccessDecision evaluate(Subject subject, String permission, AccessTarget target);
}
