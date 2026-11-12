package org.jmouse.odb;

import java.util.Collection;

/** What a job's records may be named: one word of printable ASCII, no blank, no semicolon. */
final class OdbNames {

    private OdbNames() {
    }

    /** A name as one word a record can hold: whatever it may not hold becomes {@code _}. */
    static String word(String name) {
        String word = name.replaceAll("[^\\x21-\\x7E]|;", "_");
        return word.isEmpty() ? "_" : word;
    }

    /** A name as a word no other of the taken ones is: two names that read alike once written stay two. */
    static String unique(String name, Collection<String> taken) {
        String word = word(name);
        String candidate = word;
        for (int count = 2; taken.contains(candidate); count++) {
            candidate = word + "_" + count;
        }
        return candidate;
    }
}
