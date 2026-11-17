package org.jmouse.cad.circuit;

import org.jspecify.annotations.Nullable;
import org.jmouse.cad.Formula;

/**
 * A view a circuit is shown in — {@code transient_response}, {@code frequency_response from 1 to 100000 points 200}, a
 * sweep of one of its parameters.
 *
 * @param name      what it is: {@code transient_response}, {@code frequency_response} …
 * @param primary   whether it is the view the circuit is first shown in
 * @param parameter the parameter it sweeps, or null
 * @param from      where the sweep starts, or null
 * @param to        where the sweep ends, or null
 * @param points    how many points the sweep is taken at, or null
 */
public record View(String name, boolean primary, @Nullable String parameter, @Nullable Formula from, @Nullable Formula to, @Nullable Formula points) {
}
