package org.jmouse.cad.plugin.step;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbLayers;
import org.jmouse.cad.plugin.mesh.ModelDocuments;
import org.jmouse.core.process.PipedProgram;
import org.jmouse.plugin.PluginSetting;
import org.jmouse.plugin.PluginSettingValues;
import org.jmouse.plugin.PluginSettings;
import org.jmouse.strokefont.StrokeFont;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.List;

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
     *                output, word by word — {@code docker run --rm -i jmcad-step} in a container, its tolerances after
     *                it —; empty for a server without it, which then reads no STEP
     * @param timeout how long one model may take before its process is killed
     * @param exportCommand the program that reads a pcb's description as JSON from its standard input and prints the
     *                      pcb as a STEP assembly — {@code docker run --rm -i --entrypoint python jmcad-step
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
    public StepPlugin cadStepPlugin(StepProperties properties, PluginSettings settings, ObjectProvider<ModelDocuments> models) {
        PluginSettingValues values = settings.valuesOf(StepPlugin.ID);
        PipedProgram reading = new PipedProgram("OpenCascade", () -> values.command(READING_COMMAND),
                () -> values.duration(READING_TIMEOUT), LARGEST_ANSWER);
        PipedProgram writing = new PipedProgram("OpenCascade", () -> values.command(WRITING_COMMAND),
                () -> values.duration(WRITING_TIMEOUT), LARGEST_PCB);
        return new StepPlugin(new StepMeshReader(reading), exports(writing, models), settingsOf(properties));
    }

    private static List<PluginSetting> settingsOf(StepProperties properties) {
        return List.of(
                PluginSetting.command(READING_COMMAND, "Program", "How OpenCascade is started on the server, one word to a line. "
                        + "It is handed a STEP file on its standard input and prints its triangles as VRML. Empty reads no STEP: "
                        + "a model with nothing but its STEP file is shown as its footprint's body.", properties.command()).inGroup(READING),
                PluginSetting.duration(READING_TIMEOUT, "Longest run", "How long one model may take before it is stopped.",
                        properties.timeout(), LEAST_TIME, MOST_TIME).inGroup(READING),
                PluginSetting.command(WRITING_COMMAND, "Program", "How OpenCascade is started on the server, one word to a line. "
                        + "It is handed a PCB's description as JSON on its standard input and prints the PCB as a STEP assembly. "
                        + "Empty writes no PCB as STEP.", properties.exportCommand()).inGroup(WRITING),
                PluginSetting.duration(WRITING_TIMEOUT, "Longest run", "How long one PCB may take before it is stopped.",
                        properties.exportTimeout(), LEAST_TIME, MOST_TIME).inGroup(WRITING));
    }

    /** The pcb's exports the plugin's data names, each by the same program, off while it has no command. */
    private static List<DocumentExport> exports(PipedProgram program, ObjectProvider<ModelDocuments> models) {
        PcbStepExport.Settings settings = CadSettings.read(StepPlugin.EXPORT);
        PcbArtwork.Settings artwork = CadSettings.read(PcbArtwork.SECTION);
        PcbLayers.Settings layers = CadSettings.read(PcbLayers.SECTION);
        StrokeFont font = StrokeFont.newStroke();
        return settings.exports().stream().<DocumentExport>map(variant -> new PcbStepExport(program, models::getObject, settings,
                variant, artwork, layers, font)).toList();
    }
}
