package org.jmouse.settings;

import java.util.List;
import java.util.Map;

/**
 * 📜 Where sections are declared: by owner, each owner's in its order — an application's plugins, each under its id; an
 * application's own parts. Read when a value is first asked for, so a source may stand on what is made after it.
 */
@FunctionalInterface
public interface SectionDeclarations {

    /** Every owner's sections, by the owner's word ({@code cad-engine}, {@code cad-gerber}). */
    Map<String, List<Section>> sections();
}
