package org.jmouse.mesh;

/**
 * How a part of a body looks: its base colour and how it reflects, as glTF's metallic-roughness model says it — what a
 * viewer lights a surface with. Its colour is LINEAR, as glTF's is, the light a surface gives back; a colour as a file or a
 * person writes it, as it is shown on a screen, goes through {@link #linear(double)} first, or black is drawn grey.
 *
 * @param red       0 … 1, linear
 * @param green     0 … 1, linear
 * @param blue      0 … 1, linear
 * @param opacity   1 opaque, 0 not seen
 * @param metallic  0 a dielectric — plastic, epoxy —, 1 a bare metal
 * @param roughness 0 a mirror, 1 matte
 */
public record MeshMaterial(String name, double red, double green, double blue, double opacity, double metallic,
                           double roughness) {

    /** What a part no file colours is drawn in: a mid grey, matte. */
    public static final MeshMaterial PLAIN = new MeshMaterial("plain", 0.6, 0.6, 0.6, 1, 0, 0.8);

    /** A channel of a colour as it is shown (sRGB), from 0 to 1, as the light it stands for. */
    public static double linear(double shown) {
        return shown <= 0.04045 ? shown / 12.92 : Math.pow((shown + 0.055) / 1.055, 2.4);
    }

    public boolean transparent() {
        return opacity < 1;
    }
}
