package org.jmouse.access.policy.model;

/**
 * Another file this one composes with.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * include 'bootstrap.jmp'
 * include 'roles/workspace.jmp'
 * </pre>
 *
 * <p>produces
 *
 * <pre>
 *   new PolicyInclude("bootstrap.jmp",        new SourceSpan(1, 1))
 *   new PolicyInclude("roles/workspace.jmp",  new SourceSpan(2, 1))
 * </pre>
 *
 * <p>⚠️ The parser records the path and does <strong>not</strong> read it. "Relative to what" is a
 * loader's question, and a cycle has to be detected across a whole load rather than inside one parse.
 */
public record PolicyInclude(String path, SourceSpan at) {
}
