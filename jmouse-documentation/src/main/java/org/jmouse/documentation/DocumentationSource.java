package org.jmouse.documentation;

import java.util.List;

/**
 * 📚 Where pages of documentation come from — files of Markdown, a database, a service —: whatever a page is written in, a source
 * gives it as a {@link DocumentationPage}. An application holds as many sources as it has places its pages are kept in.
 */
@FunctionalInterface
public interface DocumentationSource {

    /** Every page this source holds. */
    List<DocumentationPage> pages();
}
