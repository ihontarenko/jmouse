package org.jmouse.ipc2581;

/** A point of the file's frame: millimetres, X to the right and Y upwards, as IPC-2581 draws a board seen from its top. */
public record Ipc2581Point(double x, double y) {
}
