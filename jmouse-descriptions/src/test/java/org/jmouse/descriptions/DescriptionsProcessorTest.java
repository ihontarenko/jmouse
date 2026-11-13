package org.jmouse.descriptions;

import org.jmouse.descriptions.processing.DescriptionsProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A record and an enum compiled with the processor, their Javadoc read back as words. */
class DescriptionsProcessorTest {

    private static final String SOURCE = """
            package sample;

            /**
             * How a track is <em>drawn</em>: {@code width} wide, as {@link java.util.List the list} says, see {@link #SECTION}.
             *
             * @param width how wide, in {@code mm} &amp; never less than {@code {@code 0}}, as {@code <name>.json} says
             * @param style its {@link Style}
             */
            public record Track(double width, Style style) {

                public static final String SECTION = "pcb/track";

                /** The two ways it is drawn. */
                public enum Style {
                    /** As one line. */
                    SOLID,
                    DASHED
                }

                /** Says nothing of its component. */
                record Quiet(int value) {
                }
            }
            """;

    @Test
    void keepsWhatTheJavadocSays(@TempDir Path folder) throws IOException, ClassNotFoundException {
        Path source = folder.resolve("sample/Track.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, SOURCE);
        Path classes = Files.createDirectories(folder.resolve("classes"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        var files = compiler.getStandardFileManager(null, null, null);
        var task = compiler.getTask(null, files, null, List.of("-d", classes.toString(), "-encoding", "UTF-8"), null,
                files.getJavaFileObjects(source));
        task.setProcessors(List.of(new DescriptionsProcessor()));
        assertTrue(task.call(), "the sample compiles");

        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, null)) {
            Description track = Descriptions.of(loader.loadClass("sample.Track")).orElseThrow();
            assertEquals("How a track is drawn: width wide, as the list says, see SECTION.", track.summary());
            assertEquals(Map.of("width", "how wide, in mm & never less than {@code 0}, as <name>.json says", "style", "its Style"), track.members());

            Description style = Descriptions.of(loader.loadClass("sample.Track$Style")).orElseThrow();
            assertEquals("The two ways it is drawn.", style.summary());
            assertEquals(Map.of("SOLID", "As one line."), style.members());

            assertEquals(Optional.of(Map.of()), Descriptions.of(loader.loadClass("sample.Track$Quiet")).map(Description::members));
        }
    }
}
