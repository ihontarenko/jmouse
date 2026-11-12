package org.jmouse.cad.plugin.drawings;

import java.util.List;

/**
 * What a document's cover says of it.
 *
 * @param title    what it is called
 * @param author   who it is by; blank for not said
 * @param revision which revision it is of
 * @param date     when it was made, as a reader writes it
 * @param year     the year of that date, large on the cover
 * @param facts    what can be told of the design at a glance
 */
record SheetFacts(String title, String author, String revision, String date, String year, List<Fact> facts) {

    SheetFacts {
        facts = List.copyOf(facts);
    }

    /** One line of what is told. */
    record Fact(String label, String value) {
    }
}
