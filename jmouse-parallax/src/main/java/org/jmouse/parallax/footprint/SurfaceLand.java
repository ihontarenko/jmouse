package org.jmouse.parallax.footprint;

/**
 * The land of a row of surface leads by IPC-7351, tolerances included: the outer edge of the pads {@code Z}, their
 * inner edge {@code G} and their width {@code X}, from the leads' span, their terminal length and their width — each a
 * range — and the fillets wanted at toe, heel and side.
 *
 * <pre>
 * Z = L(min) + 2 Jt + √(C(L)² + F² + P²)
 * G = S(max) − 2 Jh − √(C(S)² + F² + P²)     S = L − 2 T, its tolerance taken as the root sum of squares
 * X = W(min) + 2 Js + √(C(W)² + F² + P²)
 * </pre>
 *
 * F is the board's fabrication tolerance, P the placement's. A range's tolerance is its width.
 */
record SurfaceLand(double outer, double inner, double width) {

    static SurfaceLand of(PackageTable.Span span, PackageTable.Span terminal, PackageTable.Span lead,
                          LandPatternRules.Fillets fillets, LandPatternRules.Tolerances tolerances) {
        double process = tolerances.fabrication() * tolerances.fabrication() + tolerances.placement() * tolerances.placement();
        double spanTolerance = span.maximum() - span.minimum();
        double terminalTolerance = terminal.maximum() - terminal.minimum();
        double leadTolerance = lead.maximum() - lead.minimum();

        double outer = span.minimum() + 2 * fillets.toe() + Math.sqrt(spanTolerance * spanTolerance + process);

        double innerMinimum = span.minimum() - 2 * terminal.maximum();
        double innerMaximum = span.maximum() - 2 * terminal.minimum();
        double innerTolerance = innerMaximum - innerMinimum;
        double innerTolerancePooled = Math.sqrt(spanTolerance * spanTolerance + 2 * terminalTolerance * terminalTolerance);
        double innerMaximumPooled = innerMaximum - (innerTolerance - innerTolerancePooled) / 2;
        double inner = innerMaximumPooled - 2 * fillets.heel()
                - Math.sqrt(innerTolerancePooled * innerTolerancePooled + process);

        double width = lead.minimum() + 2 * fillets.side() + Math.sqrt(leadTolerance * leadTolerance + process);
        return new SurfaceLand(outer, inner, width);
    }

    /** The pad's length along the lead, to the grid. */
    double padLength(LandPatternRules.Drawing drawing) {
        return drawing.nearest((outer - inner) / 2);
    }

    /** Where the pad's middle stands from the package's, to the grid. */
    double centre(LandPatternRules.Drawing drawing) {
        return drawing.nearest((outer + inner) / 4);
    }

    /** The pad's width across the lead, up to the grid — narrowed, as IPC-7351 does, where it would leave neighbours on the pitch closer than the pad gap. */
    double padWidth(LandPatternRules.Drawing drawing, LandPatternRules.Tolerances tolerances, double pitch) {
        double widest = Math.floor((pitch - tolerances.padGap()) / drawing.grid() + 1e-9) * drawing.grid();
        return Math.min(drawing.up(width), widest);
    }

    /** The pad's width across a lead with no neighbour on a pitch: a two-terminal part's. */
    double padWidth(LandPatternRules.Drawing drawing) {
        return drawing.up(width);
    }
}
