package org.jmouse.cad.packages.footprint;

import org.jmouse.sheet.Sheet;

import java.util.List;

/**
 * A kind of package whose footprints are worked out from a standard's table rather than drawn one by one: a package
 * table says which it is by its {@code form}, and the family emits a footprint for every member it lists.
 */
public sealed interface FootprintFamily permits ChipFootprints, SmallOutlineFootprints, DualInlineFootprints,
        HeaderFootprints, AxialFootprints, RadialFootprints, InlineFootprints, HoleFootprints, QuadFootprints {

    /** The {@code form} a package table names this family with: {@code gull-wing}. */
    String form();

    /** The footprints a package table lists, worked out by the rules. */
    List<StandardFootprint> footprints(Sheet table, LandPatternRules rules);
}
