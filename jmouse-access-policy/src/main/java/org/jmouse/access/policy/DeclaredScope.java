package org.jmouse.access.policy;

import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeNature;

import java.util.Optional;

/**
 * A scope that came from a file rather than from an enum.
 *
 * <p>{@link ScopeKind} is an interface, so nothing in the engine requires the vocabulary to be
 * compiled in. A product that wants its floors in configuration — because it has no JPA columns
 * mapping them and no annotations naming them — declares them in a policy file and gets these.
 *
 * <p>⚠️ <strong>A product that <em>does</em> map them should not use this.</strong> An enum is what
 * makes {@code @Enumerated(STRING)} and a compile-time annotation constant possible, and no file can
 * give those back. For such a product the file is a cross-check against the enum, never a replacement
 * — see {@link PolicyVocabulary}.
 *
 * @param rank      the position in the {@code scopes} block, which is the width order
 * @param parameter the request parameter a route names an instance with, or null. A record component
 *                  cannot be the {@link ScopeKind#requestParameter()} accessor directly — that one
 *                  returns an {@link Optional}, and a record's accessor must return the component's
 *                  own type
 */
public record DeclaredScope(String name, int rank, ScopeNature nature, String parameter)
        implements ScopeKind {

    @Override
    public Optional<String> requestParameter() {
        return Optional.ofNullable(parameter);
    }

    @Override
    public String toString() {
        return name;
    }
}
