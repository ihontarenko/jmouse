package org.jmouse.parallax.footprint;

import org.jmouse.sheet.Sheet;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A kind of package whose footprints are worked out from a standard's table rather than drawn one by one: a package
 * table document says which it is by its {@code form}, and the family emits a footprint for every member it lists.
 */
public sealed interface FootprintFamily permits ChipFootprints, SmallOutlineFootprints, DualInlineFootprints,
        HeaderFootprints, AxialFootprints, RadialFootprints, InlineFootprints, HoleFootprints {

    /** Every family, by the {@code form} a package table names it with. */
    Map<String, FootprintFamily> BY_FORM = Map.of(
            "chip", new ChipFootprints(),
            "gull-wing", new SmallOutlineFootprints(),
            "dual-inline", new DualInlineFootprints(),
            "header", new HeaderFootprints(),
            "axial", new AxialFootprints(),
            "radial", new RadialFootprints(),
            "inline", new InlineFootprints(),
            "hole", new HoleFootprints());

    static Optional<FootprintFamily> of(String form) {
        return Optional.ofNullable(BY_FORM.get(form));
    }

    /** The footprints a package table lists, worked out by the rules. */
    List<StandardFootprint> footprints(Sheet table, LandPatternRules rules);
}
