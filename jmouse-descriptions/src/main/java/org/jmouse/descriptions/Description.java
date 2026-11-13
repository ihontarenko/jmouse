package org.jmouse.descriptions;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 📝 What a type's Javadoc says, in words: its own description, and each of its members' — a record's components by their
 * {@code @param}, an enum's constants by their own comments. Inline tags are already words (a code tag its text, a
 * link its label or the name it links to), markup gone and white space folded.
 *
 * @param summary what the type is; blank where its Javadoc says nothing
 * @param members each member's words by its name; a member its Javadoc does not describe is absent
 */
public record Description(String summary, Map<String, String> members) {

    public Description {
        Objects.requireNonNull(summary, "a description has words, if none");
        members = Map.copyOf(members);
    }

    /** A member's words; empty where the Javadoc does not describe it. */
    public Optional<String> member(String name) {
        return Optional.ofNullable(members.get(name));
    }
}
