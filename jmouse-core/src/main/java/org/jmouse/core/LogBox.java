package org.jmouse.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Structured diagnostic log renderer (framework-level utility). 🚀
 *
 * <p>Usage:
 * <pre>
 * LogBox.box("TITLE")
 *     .formatter(Object::toString)
 *     .section("Group")
 *         .row("Key", value)
 *     .build()
 *     .render(System.out::println);
 * </pre>
 */
public class LogBox {

    private final String title;
    private final List<Section> sections;
    private final ValueFormatter formatter;

    private LogBox(String title,
                   List<Section> sections,
                   ValueFormatter formatter) {
        this.title = title;
        this.sections = sections;
        this.formatter = formatter;
    }

    public static Builder box(String title) {
        return new Builder(title);
    }

    public void render(Consumer<String> out) {
        Renderer.render(this, out);
    }

    public record Section(String name, List<Row> rows) { }

    public record Row(String key, Object value) { }

    public interface ValueFormatter {
        String format(Object value);
    }

    public static class Builder {

        private final String title;
        private final List<Section> sections = new ArrayList<>();
        private ValueFormatter formatter = String::valueOf;

        private Builder(String title) {
            this.title = title;
        }

        public Builder formatter(ValueFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        public SectionBuilder section(String name) {
            Section section = new Section(name, new ArrayList<>());
            sections.add(section);
            return new SectionBuilder(this, section);
        }

        public LogBox build() {
            return new LogBox(title, sections, formatter);
        }
    }

    public static class SectionBuilder {

        private final Builder root;
        private final Section section;

        private SectionBuilder(Builder root, Section section) {
            this.root = root;
            this.section = section;
        }

        public SectionBuilder row(String key, Object value) {
            section.rows().add(new Row(key, value));
            return this;
        }

        public SectionBuilder section(String name) {
            return root.section(name);
        }

        public LogBox build() {
            return root.build();
        }
    }

    static final class Renderer {

        private static final String H_LINE =
                "────────────────────────────────────────────";

        static void render(LogBox box, Consumer<String> out) {

            out.accept("┌" + H_LINE);
            out.accept("│ 🚀 " + box.title);
            out.accept("├" + H_LINE);

            for (Section section : box.sections) {

                out.accept("│");
                out.accept("│ ▶ " + section.name());

                int pad = calculatePadding(section);

                for (Row row : section.rows()) {
                    out.accept("│   " + format(row, pad, box.formatter));
                }
            }

            out.accept("└" + H_LINE);
        }

        private static int calculatePadding(Section section) {
            return section.rows().stream()
                    .mapToInt(r -> r.key().length())
                    .max()
                    .orElse(10) + 2;
        }

        private static String format(Row row, int pad, ValueFormatter formatter) {
            String key = String.format("%-" + pad + "s", row.key());
            return key + ": " + formatter.format(row.value());
        }
    }
}