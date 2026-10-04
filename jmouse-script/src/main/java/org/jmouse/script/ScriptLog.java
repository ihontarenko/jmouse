package org.jmouse.script;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;

/**
 * What rules wrote with {@code @log}, kept for an administration screen.
 *
 * <h2>⚠️ THE LAST N LINES, IN MEMORY — and that is a choice, not a shortcut</h2>
 *
 * <p>A rule at a per-item stage runs once per item in a walk of thousands; a database row per
 * {@code @log} would put a write inside the innermost loop of the product. What a person reads here is
 * "what did my rule say just now", which the last few thousand lines answer. A product that needs more
 * keeps everything in its own server log.</p>
 *
 * <h2>⚠️ THE CAPACITY IS A SUPPLIER, NOT A NUMBER</h2>
 *
 * <p>It is read on every write on purpose: a product that keeps this in editable settings can raise it
 * without a restart, and a product that does not simply passes {@code () -> 2_000}. Taking an {@code
 * int} here would quietly make the first case impossible, and a library is exactly the wrong place to
 * decide that.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptLog {

    private final IntSupplier capacity;
    private final Deque<Line> lines = new ArrayDeque<>();

    public ScriptLog(IntSupplier capacity) {
        this.capacity = capacity;
    }

    /**
     * One line a rule wrote.
     *
     * @param at      when
     * @param level   how loud — the rule author's own word
     * @param rule    the rule's name
     * @param stage   the moment it ran at
     * @param message what it said
     */
    public record Line(Instant at, String level, String rule, String stage, String message) {
    }

    public synchronized void record(String level, String rule, String stage, String message) {
        lines.addFirst(new Line(Instant.now(), level, rule, stage, message));

        while (lines.size() > capacity.getAsInt()) {
            lines.removeLast();
        }
    }

    /**
     * Newest first.
     *
     * @param rule  only this rule's lines, or {@code null} for every rule
     * @param level only this level, or {@code null} for every level
     * @param limit how many at most
     * @return the lines
     */
    public synchronized List<Line> read(String rule, String level, int limit) {
        List<Line> found = new ArrayList<>();

        for (Line line : lines) {
            boolean thisRule  = rule == null || rule.isBlank() || rule.equals(line.rule());
            boolean thisLevel = level == null || level.isBlank()
                    || level.toUpperCase(Locale.ROOT).equals(line.level());

            if (thisRule && thisLevel) {
                found.add(line);

                if (found.size() >= limit) {
                    break;
                }
            }
        }

        return found;
    }

    public synchronized void clear() {
        lines.clear();
    }

}
