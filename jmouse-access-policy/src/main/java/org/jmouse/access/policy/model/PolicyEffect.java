package org.jmouse.access.policy.model;

/**
 * Whether a grant gives or takes away.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 *   {@literal @}SPACE:kyiv  entry:read          -&gt;  ALLOW   (implied — the common case reads without ceremony)
 *   {@literal @}SPACE:kyiv  entry:read  allow   -&gt;  ALLOW
 *   {@literal @}SELF        form:write  deny    -&gt;  DENY
 * </pre>
 *
 * <p>Two constants and no third. There is deliberately no ordering, priority or override in this
 * language: {@link #DENY} wins over every {@link #ALLOW}, in this file and across every other grant
 * source, and it is applied last. A grammar offering precedence would teach readers a rule the engine
 * does not have.
 */
public enum PolicyEffect {

    /** Implied where nothing is written. */
    ALLOW,

    /** Always written. A denial is never accidental. */
    DENY
}
