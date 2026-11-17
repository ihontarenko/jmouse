package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;

/**
 * What the editor asks of the router for parts it moved, turned or put on the other side: the board before, the layout with only
 * what the reader moved moved — the copper its parts pull or push is the engine's to work out ({@link PartDragging}) —, what moving
 * does to the lines ({@link PartDragMode}), and the angle the reader lays copper at.
 */
public record PartDrag(PcbDrawing before, PcbDrawing moved, double angleStep, PartDragMode mode) {
}
