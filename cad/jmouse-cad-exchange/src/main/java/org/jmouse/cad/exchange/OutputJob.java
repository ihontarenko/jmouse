package org.jmouse.cad.exchange;

import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

import java.util.List;

/**
 * A set of formats a document is released as together, ready to take: a plugin of a pcb house brings its own order. A pcb's own
 * {@code output} lines are the same thing, kept with it.
 *
 * @param id      one word it is known by
 * @param title   how a reader is shown it
 * @param formats the formats, by the word each is written under
 */
public record OutputJob(String id, String title, List<String> formats) implements Identified {

    public static final Contributions<OutputJob> POINT = new Contributions<>("cad.exchange.output-jobs", OutputJob.class);

    public OutputJob {
        if (formats.isEmpty()) {
            throw new IllegalArgumentException("The output job '" + id + "' names no format.");
        }
        formats = List.copyOf(formats);
    }
}
