package org.jmouse.cad.circuit;

import org.jmouse.cad.Formula;

/**
 * A view a circuit is shown in — {@code transient_response}, {@code frequency_response from 1 to 100000 points 200}, a
 * sweep of one of its parameters.
 *
 * @param parameter the parameter it sweeps, or null
 * @param from      where the sweep starts, or null
 */
public record View(String name, boolean primary, String parameter, Formula from, Formula to, Formula points) {
}
