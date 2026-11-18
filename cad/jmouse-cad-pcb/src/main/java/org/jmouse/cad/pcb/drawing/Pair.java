package org.jmouse.cad.pcb.drawing;

/**
 * A differential pair: two nets routed side by side.
 *
 * @param name     what it is called
 * @param width    the width each of its tracks is laid at, millimetres
 * @param gap      how far apart its two tracks run, millimetres
 * @param skew     how far the two lengths may differ, millimetres; 0: the two lengths are not matched
 * @param positive its positive net
 * @param negative its negative net
 */
public record Pair(String name, double width, double gap, double skew, String positive, String negative) {
}
