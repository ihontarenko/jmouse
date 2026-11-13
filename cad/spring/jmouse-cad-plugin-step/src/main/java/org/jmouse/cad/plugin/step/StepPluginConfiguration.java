package org.jmouse.cad.plugin.step;

import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.settings.Parameter;
import org.jmouse.settings.Section;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbLayers;
import org.jmouse.cad.plugin.mesh.ModelDocuments;
import org.jmouse.core.process.PipedProgram;
import org.jmouse.settings.ParameterValues;
import org.jmouse.settings.Settings;
import org.jmouse.strokefont.StrokeFont;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/** 🧩 The STEP plugin, on the classpath: its plugin bean, OpenCascade as the server names it, once for each program. */
@AutoConfiguration(afterName = "org.jmouse.cad.plugin.mesh.MeshPluginConfiguration")
@EnableConfigurationProperties(StepPluginConfiguration.StepProperties.class)
public class StepPluginConfiguration {

    private static final String READING_COMMAND = "reading-command";
    private static final String READING_TIMEOUT = "reading-timeout";
    private static final String WRITING_COMMAND = "writing-command";
    private static final String WRITING_TIMEOUT = "writing-timeout";

    /** What a reader finds each program's settings under. */
    private static final String READING = "Reading a model";
    private static final String WRITING = "Writing a PCB";
    /** Its sections of programs. */
    private static final String READING_SECTION = "reading";
    private static final String WRITING_SECTION = "writing";

    /** A model or a pcb may be given from a second up to an hour. */
    private static final Duration LEAST_TIME = Duration.ofSeconds(1);
    private static final Duration MOST_TIME  = Duration.ofHours(1);

    /** A model's triangles as VRML are at most this long: a part's body, not an assembly. */
    private static final int LARGEST_ANSWER = 128 * 1024 * 1024;

    /** A pcb's STEP is at most this long: the pcb and one copy of each of its models. */
    private static final int LARGEST_PCB = 256 * 1024 * 1024;

    /**
     * {@code jmouse.cad.step.*}: how this server reaches OpenCascade.
     *
     * @param command the program that reads a STEP file from its standard input and prints its triangles as VRML on its
     *                output, word by word — {@code docker run --rm -i jm_cad-step} in a container, its tolerances after
     *                it —; empty for a server without it, which then reads no STEP
     * @param timeout how long one model may take before its process is killed
     * @param exportCommand the program that reads a pcb's description as JSON from its standard input and prints the
     *                      pcb as a STEP assembly — {@code docker run --rm -i --entrypoint python jm_cad-step
     *                      /usr/local/bin/board-to-step.py} —; empty for a server that writes no pcb as STEP
     * @param exportTimeout how long one pcb may take before its process is killed
     */
    @ConfigurationProperties("jmouse.cad.step")
    public record StepProperties(List<String> command, @DefaultValue("PT120S") Duration timeout, List<String> exportCommand,
                                 @DefaultValue("PT300S") Duration exportTimeout) {

        public StepProperties {
            command = command == null ? List.of() : List.copyOf(command);
            exportCommand = exportCommand == null ? List.of() : List.copyOf(exportCommand);
        }
    }

    @Bean
    public StepPlugin cadStepPlugin(StepProperties properties, Settings settings, ObjectProvider<ModelDocuments> models,
                                    CadSettings sections, CadStandards standards, TableSource tables) {
        ParameterValues readingValues = settings.parameters(StepPlugin.ID, READING_SECTION);
        ParameterValues writingValues = settings.parameters(StepPlugin.ID, WRITING_SECTION);
        PipedProgram reading = new PipedProgram("OpenCascade", () -> readingValues.command(READING_COMMAND),
                () -> readingValues.duration(READING_TIMEOUT), LARGEST_ANSWER);
        PipedProgram writing = new PipedProgram("OpenCascade", () -> writingValues.command(WRITING_COMMAND),
                () -> writingValues.duration(WRITING_TIMEOUT), LARGEST_PCB);
        return new StepPlugin(new StepMeshReader(reading), exports(writing, models, sections, standards, tables), sections, settingsOf(properties));
    }

    private static List<Section> settingsOf(StepProperties properties) {
        return List.of(Section.fixed(READING_SECTION, READING, null, List.of(
                Parameter.command(READING_COMMAND, "Program", "How OpenCascade is started on the server, one word to a line. "
                        + "It is handed a STEP file on its standard input and prints its triangles as VRML. Empty reads no STEP: "
                        + "a model with nothing but its STEP file is shown as its footprint's body.", properties.command()),
                Parameter.duration(READING_TIMEOUT, "Longest run", "How long one model may take before it is stopped.",
                        properties.timeout(), LEAST_TIME, MOST_TIME))),
                Section.fixed(WRITING_SECTION, WRITING, null, List.of(
                Parameter.command(WRITING_COMMAND, "Program", "How OpenCascade is started on the server, one word to a line. "
                        + "It is handed a PCB's description as JSON on its standard input and prints the PCB as a STEP assembly. "
                        + "Empty writes no PCB as STEP.", properties.exportCommand()),
                Parameter.duration(WRITING_TIMEOUT, "Longest run", "How long one PCB may take before it is stopped.",
                        properties.exportTimeout(), LEAST_TIME, MOST_TIME))));
    }

    /** The pcb's export of a variant its data names, each by the same program, off while it has no command. */
    private static Function<PcbStepExport.Variant, DocumentExport> exports(PipedProgram program, ObjectProvider<ModelDocuments> models,
                                                                           CadSettings sections, CadStandards standards,
                                                                           TableSource tables) {
        StrokeFont font = StrokeFont.newStroke();
        return variant -> new PcbStepExport(program, models::getObject, () -> sections.read(StepPlugin.EXPORT), variant,
                () -> sections.read(PcbArtwork.SECTION), () -> sections.read(PcbLayers.SECTION), font, standards, tables);
    }
}
