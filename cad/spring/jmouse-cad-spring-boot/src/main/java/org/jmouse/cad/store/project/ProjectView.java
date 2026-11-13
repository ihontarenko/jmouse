package org.jmouse.cad.store.project;

import org.jmouse.cad.simulation.ProjectPurpose;
import org.jmouse.cad.store.catalogue.CatalogueViews.DocumentSummary;

import java.time.Instant;

/**
 * A project as a list and its page show it: its own document as every list shows a document — its address, title, summary,
 * status, where it is held and by whom —, and what is the project's alone.
 *
 * @param document  the project's document
 * @param purpose   a design, or its keeper's drafts
 * @param writable  whether the reader may call, describe or archive it, and put work in it
 * @param version   the version of its document a change is checked against
 * @param changedAt when anything of it last changed: its document, or any it holds
 */
public record ProjectView(DocumentSummary document, ProjectPurpose purpose, boolean writable, long version, Instant changedAt) {
}
