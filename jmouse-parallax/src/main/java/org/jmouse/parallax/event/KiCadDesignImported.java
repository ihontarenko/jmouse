package org.jmouse.parallax.event;

import org.jmouse.parallax.owner.ParallaxOwner;

import java.util.List;
import java.util.Map;

/**
 * A KiCad design was imported into an owner's place; published inside the transaction that stored it, so a host files
 * what was made — in its own categories — in the same commit.
 *
 * @param owner     whose place it went into
 * @param root      the slug of the design's root circuit
 * @param libraries the KiCad library of every symbol and component made from KiCad, by slug, made now or already
 *                  there — {@code Device} for {@code kicad-device-r-shunt}
 * @param circuits  the slugs of the design's circuits, the root's first
 */
public record KiCadDesignImported(ParallaxOwner owner, String root, Map<String, String> libraries, List<String> circuits) {
}
