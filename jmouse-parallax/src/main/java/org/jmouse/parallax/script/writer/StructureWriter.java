package org.jmouse.parallax.script.writer;

import org.jmouse.parallax.script.Block;

/**
 * Blocks only some kinds have — an iterated function system's maps, a Lindenmayer system's axiom, rules and turtle, a
 * construction's steps, a polytope's vertices and edges, a point list — and the catalogue's own: a collection's items
 * and sections, a course's lessons and steps, an instrument's object kinds, representations and the object it opens.
 */
final class StructureWriter {

    private StructureWriter() {
    }

    /** {@code 0.85 : (0.85 x + 0.04 y, -0.04 x + 0.85 y + 1.6)}. */
    static void maps(Block.Maps maps, SourceLines lines) {
        lines.block("maps", inner -> maps.maps().forEach(map ->
                inner.line(map.weight().text() + " : " + Values.tuple(map.components()))));
    }

    /** {@code "F" -> "F+F--F+F"}: a symbol in quotes, since it may be any character. */
    static void rules(Block.Rules rules, SourceLines lines) {
        lines.block("rules", inner -> rules.rules().forEach((symbol, replacement) ->
                inner.line(Values.text(symbol) + " -> " + Values.text(replacement))));
    }

    static void turtle(Block.Turtle turtle, SourceLines lines) {
        lines.block("turtle", inner -> turtle.settings().forEach((name, value) -> inner.line(name + " = " + value.text())));
    }

    /** A construction's statements, each its whole line as written. */
    static void steps(Block.Steps steps, SourceLines lines) {
        lines.block("steps", inner -> steps.steps().forEach(step -> inner.line(step.text())));
    }

    /** {@code edges where hamming(first, second) = 1}, or written out: {@code edges = [[0, 1], [1, 2]]}. */
    static String edges(Block.Edges edges) {
        return edges.condition().map(condition -> "edges where " + condition.text())
                .orElseGet(() -> "edges = " + edges.pairs().orElseThrow().text());
    }

    /** {@code points { (0, 0) … }}: a point a line. */
    static void points(Block.PointList points, SourceLines lines) {
        lines.block("points", inner -> points.points().forEach(point -> inner.line(point.size() == 1
                ? point.getFirst().text() : Values.tuple(point))));
    }

    // ─── catalogue ──────────────────────────────────────────────────────────────────────────────────

    static void section(Block.Section section, SourceLines lines) {
        lines.block("section " + Values.text(section.title()), inner -> inner.line("items " + Values.names(section.references())));
    }

    /** {@code lesson "Title" as slug { summary …;  minutes …;  object …;  step … }}. */
    static void lesson(Block.Lesson lesson, SourceLines lines) {
        lines.block("lesson " + Values.text(lesson.title()) + " as " + lesson.slug(), inner -> {
            lesson.summary().ifPresent(summary -> inner.line("summary " + Values.longText(summary, inner.indentation())));
            lesson.minutes().ifPresent(minutes -> inner.line("minutes " + minutes));
            lesson.object().ifPresent(object -> inner.line("object " + object));
            lesson.steps().forEach(step -> courseStep(step, inner));
        });
    }

    /** {@code step "Contours" { object …;  view …;  values …;  camera …;  narration …;  task … answer … }}, a field a line. */
    static void courseStep(Block.CourseStep step, SourceLines lines) {
        lines.block("step " + Values.text(step.title()), inner -> {
            step.object().ifPresent(object -> inner.line("object " + object));
            step.view().ifPresent(view -> inner.line("view " + PresentationWriter.representation(view)));
            if (!step.values().isEmpty()) {
                inner.line("values " + Values.assignments(step.values()));
            }
            step.camera().ifPresent(camera -> inner.line(PresentationWriter.camera(camera)));
            step.narration().ifPresent(narration -> inner.line("narration " + Values.longText(narration, inner.indentation())));
            step.task().ifPresent(task -> inner.line("task " + Values.text(task.question())
                    + task.answer().map(answer -> " answer " + Values.longText(answer, inner.indentation())).orElse("")));
        });
    }
}
