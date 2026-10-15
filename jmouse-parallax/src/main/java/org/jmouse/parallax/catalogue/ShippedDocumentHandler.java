package org.jmouse.parallax.catalogue;

import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Kind;

import java.util.List;

/**
 * The host's part of the catalogue import: a catalogue document of a kind Parallax reads but does not keep — a
 * {@code course}, which the host's learning keeps — is handed to the handler that takes its kind. A shipped document
 * no handler takes fails the import, as a document the catalogue cannot read does.
 */
public interface ShippedDocumentHandler {

    boolean handles(Kind kind);

    /**
     * Adds what is missing among the documents, and never rewrites what exists — an editor may have changed it since.
     *
     * @param documents every shipped document of a kind this handler takes, in the order of their files; maybe none
     */
    Applied apply(List<ExperienceDocument> documents);

    /**
     * What a handler wrote.
     *
     * @param written how many things were added
     * @param summary the same in words, as the import's outcome lists it: {@code 3 lesson(s)}
     */
    record Applied(int written, String summary) {
    }
}
