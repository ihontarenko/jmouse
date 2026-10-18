package org.jmouse.java.index;

import org.jmouse.java.parser.model.LocalBinding;

import java.util.List;

/** Explicit outcomes; unsupported inference is unresolved rather than a guessed declaration. */
public sealed interface MemberResolution {
    record Resolved(MemberSymbol target) implements MemberResolution {}

    record Ambiguous(List<MemberSymbol> candidates) implements MemberResolution {
        public Ambiguous {
            candidates = List.copyOf(candidates);
        }
    }

    record Local(LocalBinding binding) implements MemberResolution {}

    record Unresolved(String name, String reason) implements MemberResolution {}
}
