package org.jmouse.gerber;

/**
 * A point of a Gerber or an Excellon file, in millimetres. Which way Y runs is the caller's to decide.
 *
 * @param x across
 * @param y up, as Gerber's image runs
 */
public record GerberPoint(double x, double y) {
}
