package org.jmouse.cad.pcb.geometry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

/**
 * Areas grown and shrunk so a gap is kept exactly: what a pour clears round other copper by, and where a via may stand. A round
 * offset is followed in chords, which stand inside the true curve; they are made fine enough to stand off it by no more than
 * the arc error, and a gap is taken that much further, as KiCad keeps a zone's clearance.
 *
 * @param arcError how far a round offset followed in chords may stand off the true curve
 */
public record AreaOffsets(double arcError) {

    /**
     * An area grown — or, by a negative distance, shrunk — by at least the distance everywhere: what a gap is kept by. Grown by
     * the bare distance, a pour came 2 µm under a 0.2 mm clearance — copper made nearer than the rule allows.
     */
    public Geometry grown(Geometry area, double distance) {
        return BufferOp.bufferOp(area, distance + Math.signum(distance) * arcError, offsets(Math.abs(distance)));
    }

    /** An area grown by at most the distance: what takes back what a shrinking took, and never more. */
    public Geometry grownBack(Geometry area, double distance) {
        return BufferOp.bufferOp(area, distance, offsets(distance));
    }

    /**
     * How a round offset of a radius is followed: in as many pieces a quarter as keep each chord within the arc error of the
     * curve, and with what is offset never simplified first — JTS simplifies by a hundredth of the distance by default, which
     * cost a further 0.7 µm of a 0.2 mm gap.
     */
    private BufferParameters offsets(double radius) {
        int quadrantSegments = radius <= arcError ? 1
                : (int) Math.ceil((Math.PI / 2) / (2 * Math.acos(1 - arcError / radius)));
        BufferParameters parameters = new BufferParameters(Math.max(1, quadrantSegments));
        parameters.setSimplifyFactor(0);
        return parameters;
    }
}
