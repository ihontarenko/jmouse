package org.jmouse.cad.pcb.drawing;

/**
 * Where a pcb widens its tracks as they enter copper, and how far: a drop's measures are shares of the copper it enters.
 *
 * @param target what the tracks enter: {@code round} pads and vias, {@code surface} pads, or other {@code tracks}
 * @param length how far along the track a drop reaches, as a share of the copper it enters
 * @param width  how wide a drop's base is, as a share of the copper it enters
 * @param curved whether a drop's sides are curved rather than straight
 */
public record Teardrop(String target, double length, double width, boolean curved) {
}
