package org.jmouse.liveblocks;

/**
 * One directive, answered — and the shape a consumer draws without understanding it.
 *
 * <h2>⚠️ Flat, and deliberately not a payload per block kind</h2>
 *
 * <p>The temptation is a field per concept: an {@code issue} object, a {@code sprint} object, a
 * {@code board} object. That contract makes the <em>consumer</em> learn every producer's vocabulary — so
 * a fourth block kind anywhere is a release of every renderer, and a product cannot add one without
 * asking permission from the products that quote it.
 *
 * <p>A title, a subtitle and a link is what a live block <em>looks like</em> on a page, and it is the
 * largest thing every producer can agree on. What an issue is stays entirely inside the product that
 * owns it.
 *
 * <p>⚠️ <strong>The payload fields are not a guess: they are exactly what the shipped consumers
 * draw.</strong> There are two of those now, and they draw different shapes:
 *
 * <ul>
 *   <li>a <strong>block</strong> — a link whose first line is the title and whose second is the
 *       subtitle. A producer with more to say — a status, a count, an estimate — packs it into the
 *       subtitle rather than into a field nothing paints.
 *   <li>a <strong>reference drawn inside a sentence</strong> — one badge, with room for one short word
 *       and nothing else. Neither a summary nor a state line is one, which is what {@link #label} is
 *       for.
 * </ul>
 *
 * <p>⚠️ So a field here is still earned by a renderer wanting it, never by a producer having it: a
 * fifth one is a change to the renderers first and to this record second.
 *
 * @param name     the directive answered, echoed back so a batched answer needs no positional matching
 * @param argument its argument, echoed for the same reason
 * @param status   ⚠️ read this before the payload — every field below is null unless it is
 *                 {@link DirectiveStatus#RESOLVED}
 * @param label    the shortest name the thing has, for a reference drawn inside a sentence — an issue's
 *                 key, a part number. ⚠️ <strong>Null</strong> where a producer has nothing shorter than
 *                 its title, and a consumer with no room for one ignores it. Null rather than blank, so
 *                 a consumer tests one thing
 * @param title    the line a reader recognises the thing by — an issue's summary, a sprint's name
 * @param subtitle the state around it, in one line: {@code TSSR-4 · In Review · 8 points}. Optional
 * @param url      where to go to see the whole of it. ⚠️ <strong>Absolute, and pointing at the producing
 *                 product</strong>, because the page it is drawn on is served by a different one
 */
public record ResolvedDirective(
        String          name,
        String          argument,
        DirectiveStatus status,
        String          label,
        String          title,
        String          subtitle,
        String          url
) {

    /**
     * Answered, with everything a block draws — and no short name.
     *
     * <p>⚠️ <strong>Kept for the producers that have nothing shorter than their title</strong>, and
     * because a resolver written before labels existed goes on saying exactly what it said. An inline
     * reference to something answered this way falls back to whatever the document called it.
     */
    public static ResolvedDirective resolved(
            Directive directive, String title, String subtitle, String url) {

        return resolved(directive, null, title, subtitle, url);
    }

    /** Answered, with everything a consumer draws — a badge inside a sentence included. */
    public static ResolvedDirective resolved(
            Directive directive, String label, String title, String subtitle, String url) {

        return new ResolvedDirective(
                directive.name(), directive.argument(), DirectiveStatus.RESOLVED,
                label, title, subtitle, url);
    }

    /**
     * Not answered, and saying why.
     *
     * <p>⚠️ Carries the name and argument like any other answer: a consumer matches a batch by them, and
     * a miss that dropped them would be an answer to a question nobody could identify.
     */
    public static ResolvedDirective miss(Directive directive, DirectiveStatus status) {
        return new ResolvedDirective(
                directive.name(), directive.argument(), status, null, null, null, null);
    }

}
