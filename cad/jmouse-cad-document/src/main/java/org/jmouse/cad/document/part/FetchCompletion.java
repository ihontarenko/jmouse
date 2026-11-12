package org.jmouse.cad.document.part;

import org.jmouse.plugin.Contributions;

/**
 * What a plugin adds to a part fetched from any provider, before it is stored: the 3D models its footprint names, a
 * datasheet beside it. A provider fetches what it knows; a completion brings what another plugin knows, so neither has
 * to know the other.
 */
@FunctionalInterface
public interface FetchCompletion {

    Contributions<FetchCompletion> POINT = new Contributions<>("cad.parts.completions", FetchCompletion.class);

    /**
     * The part with what this completion adds — documents before the ones that name them, their files —; the part as it
     * is when there is nothing to add.
     */
    FetchedPart completed(FetchedPart part);
}
