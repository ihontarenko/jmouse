package org.jmouse.cad.exchange.imported;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Identifiers of one kind that one document gives out — the ids of a sheet's parts and marks, its nets, a symbol's
 * pins —, each made of what the product calls the thing ({@link ImportedNames#name}) and none given twice: a second
 * {@code R1} is {@code R1_2}. Two that differ only in case are one, as a reader takes them; the count fits within the
 * longest an identifier of the kind may be.
 */
public final class UniqueIdentifiers {

    /** No limit to how long an identifier may be. */
    public static final int UNLIMITED = Integer.MAX_VALUE;

    private final String opening;
    private final int longest;
    private final Set<String> taken = new HashSet<>();

    /**
     * @param opening what one opens with where the product's name opens with no letter: {@code P} makes {@code 3V3}
     *                {@code P3V3}
     * @param longest how long one is at most, its count included
     */
    public UniqueIdentifiers(String opening, int longest) {
        this.opening = opening;
        this.longest = longest;
    }

    /** An identifier for what the product calls {@code written}, taken from now on. */
    public String of(String written) {
        String name = ImportedNames.name(written, opening);
        name = name.substring(0, Math.min(name.length(), longest));
        String unique = name;
        for (int count = 2; !taken.add(unique.toLowerCase(Locale.ROOT)); count++) {
            String ending = "_" + count;
            unique = name.substring(0, Math.min(name.length(), longest - ending.length())) + ending;
        }
        return unique;
    }
}
