package org.jmouse.cad.pcb.drawing;

import java.util.List;

/**
 * What this pcb is released as: a set of formats written together.
 *
 * @param name    one word it is known by
 * @param formats the formats, by the word each is written under: {@code gerber}, {@code step}
 * @param title   how a reader is shown it
 */
public record Output(String name, List<String> formats, String title) {

    public Output {
        formats = List.copyOf(formats);
    }
}
