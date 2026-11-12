package org.jmouse.mesh;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A turned body is round and closed: its corners on the profile's radii, its round shaded smooth, its ends discs. */
class MeshBuilderTest {

    private static final MeshMaterial SLEEVE = new MeshMaterial("sleeve", 0.1, 0.2, 0.5, 1, 0, 0.5);
    private static final int STEPS = 16;

    @Test
    void aTurnedSleeveIsRoundSmoothAndClosed() {
        // A can 5 mm across and 7 mm tall, its top rolled in: a gentle bend at the top's edge.
        List<double[]> profile = List.of(new double[] {2.5, 0}, new double[] {2.5, 6.5}, new double[] {2.4, 6.85}, new double[] {2.2, 7});
        MeshPart part = new MeshBuilder().revolved(SLEEVE, Placement.IDENTITY, profile, 0, 360, STEPS).build().parts().getFirst();
        float[] positions = part.positions();
        float[] normals = part.normals();
        double highest = 0;
        for (int corner = 0; corner < positions.length; corner += 3) {
            double radius = Math.hypot(positions[corner], positions[corner + 1]);
            assertTrue(radius <= 2.5 + 1e-5, "no corner stands outside the profile");
            highest = Math.max(highest, positions[corner + 2]);
            assertEquals(1, Math.sqrt(normals[corner] * normals[corner] + normals[corner + 1] * normals[corner + 1] + normals[corner + 2] * normals[corner + 2]), 1e-4);
        }
        assertEquals(7, highest, 1e-5);
        // The side's normals point straight out: a corner on the straight sleeve at angle a has the normal (cos a, sin a, 0).
        boolean sideSeen = false;
        for (int corner = 0; corner < positions.length; corner += 3) {
            if (Math.abs(positions[corner + 2] - 3.0) > 3.4 || Math.abs(normals[corner + 2]) > 1e-6) {
                continue;
            }
            double radius = Math.hypot(positions[corner], positions[corner + 1]);
            assertEquals(positions[corner] / radius, normals[corner], 1e-4);
            sideSeen = true;
        }
        assertTrue(sideSeen);
    }

    @Test
    void aPartTurnIsAStripeWithNoDiscs() {
        List<double[]> band = List.of(new double[] {2.55, 0.5}, new double[] {2.55, 6});
        MeshPart part = new MeshBuilder().revolved(SLEEVE, Placement.IDENTITY, band, -30, 30, STEPS).build().parts().getFirst();
        float[] positions = part.positions();
        for (int corner = 0; corner < positions.length; corner += 3) {
            double angle = Math.toDegrees(Math.atan2(positions[corner + 1], positions[corner]));
            assertTrue(angle >= -30 - 1e-4 && angle <= 30 + 1e-4, "a corner stands within the stripe's turn");
        }
        // A sixth of a turn at 16 sides a turn is 3 sides, two triangles each.
        assertEquals(3 * 2 * 3 * 3, positions.length);
    }
}
