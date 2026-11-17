package org.jmouse.cad.store.plugin;

import org.jmouse.cad.store.CadProperties;
import org.jmouse.settings.Parameter;
import org.jmouse.settings.ParameterValues;
import org.jmouse.settings.Section;
import org.jmouse.settings.Settings;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 🎚️ What the engine itself may be set to while it runs, as sections of the engine plugin's settings (ADR 0007): read each time it
 * is used, so a change applies at once. {@link CadProperties} says what they start from. What has to be known before the engine
 * stands — its database, the key that seals its secrets, how many threads it starts — stays in the server's configuration.
 */
public final class EngineConfiguration {

    /** Its sections, by path. */
    private static final String DOCUMENTS   = "documents";
    private static final String PART_SEARCH = "part-search";
    private static final String SIMULATION  = "simulation";
    private static final String READERS     = "readers";
    /** The section of where the engine keeps its library: what a reader of it works in. */
    public static final String LIBRARY      = "library";

    private static final String PART_LIMIT       = "part-limit";
    private static final String DOCUMENT_LENGTH  = "document-length";
    private static final String DEFAULT_CATEGORY = "default-category";
    private static final String INLINE_SAMPLES   = "inline-samples";
    private static final String MOST_SAMPLES     = "most-samples";
    private static final String ENGINE_SAMPLES   = "engine-samples";
    private static final String JOB_RETENTION    = "job-retention";
    private static final String MOST_VALUES      = "most-values";
    private static final String WORKING_FOLDER   = "working-folder";

    private final Settings settings;

    public EngineConfiguration(Settings settings) {
        this.settings = settings;
    }

    /** The engine's own sections of parameters, each starting from what the server is configured with. */
    static List<Section> sectionsFrom(CadProperties properties) {
        CadProperties.Simulation simulation = properties.simulation();
        return List.of(
                Section.fixed(LIBRARY, "Library", "Where the engine keeps its projects, its documents and their files.", List.of(
                        Parameter.folder(WORKING_FOLDER, "Working folder", "The folder the engine works in: its projects, its documents and "
                                + "every file they hold. Another folder is another library — the engine's own shipped documents stay.",
                                properties.library().folder()))),
                Section.fixed(DOCUMENTS, "Documents", "How long a document may be, where an unfiled one goes, and the day a revision is named by.", List.of(
                        Parameter.wholeNumber(DOCUMENT_LENGTH, "Longest document", "The most text one document may be.",
                                properties.documents().maximumLength(), 1_000, 100_000_000, "characters"),
                        Parameter.text(DEFAULT_CATEGORY, "Category of the unfiled", "The category a new document that names none is filed in; none when empty.",
                                properties.documents().defaultCategory()))),
                Section.fixed(PART_SEARCH, "Part search", null, List.of(
                        Parameter.wholeNumber(PART_LIMIT, "Results from each place", "How many parts each place parts are looked for in answers with at most.",
                                properties.parts().limit(), 1, 500, "parts"))),
                Section.fixed(SIMULATION, "Simulation", "How much one simulation may ask for, and how long its result waits.", List.of(
                        Parameter.wholeNumber(INLINE_SAMPLES, "Answered at once", "The most samples a simulation is answered with at once; more run as a job.",
                                simulation.inlineSamples(), 100, 10_000_000, "samples"),
                        Parameter.wholeNumber(MOST_SAMPLES, "Most samples", "The most samples a simulation may ask for at all.",
                                simulation.maximumSamples(), 100, 100_000_000, "samples"),
                        Parameter.wholeNumber(ENGINE_SAMPLES, "Another engine's run", "How many even steps another engine's run is printed in when nobody says.",
                                simulation.engineSamples(), 2, 10_000_000, "samples"),
                        Parameter.duration(JOB_RETENTION, "Finished jobs kept", "How long a finished job and its result wait for whoever started it.",
                                simulation.jobRetention(), Duration.ofMinutes(1), Duration.ofDays(7)))),
                Section.fixed(READERS, "Readers", null, List.of(
                        Parameter.wholeNumber(MOST_VALUES, "Values a reader keeps", "The most values of the interface's — a column's width, a look — one reader keeps; an interface naming more names something it should not.",
                                512, 16, 100_000, "values"))));
    }

    private ParameterValues section(String path) {
        return settings.parameters(CadEnginePlugin.ID, path);
    }

    /** The folder the engine works in now. */
    public Path workingFolder() {
        return section(LIBRARY).folder(WORKING_FOLDER);
    }

    public int partLimit() {
        return (int) section(PART_SEARCH).wholeNumber(PART_LIMIT);
    }

    public int maximumDocumentLength() {
        return (int) section(DOCUMENTS).wholeNumber(DOCUMENT_LENGTH);
    }

    /** The category a new document naming none is filed in; empty when the installation leaves such a document unfiled. */
    public Optional<String> defaultCategory() {
        return Optional.ofNullable(section(DOCUMENTS).text(DEFAULT_CATEGORY)).map(String::strip).filter(code -> !code.isEmpty());
    }

    public int inlineSamples() {
        return (int) section(SIMULATION).wholeNumber(INLINE_SAMPLES);
    }

    public int maximumSamples() {
        return (int) section(SIMULATION).wholeNumber(MOST_SAMPLES);
    }

    public int engineSamples() {
        return (int) section(SIMULATION).wholeNumber(ENGINE_SAMPLES);
    }

    public Duration jobRetention() {
        return section(SIMULATION).duration(JOB_RETENTION);
    }

    /** The most values of the interface's one reader keeps. */
    public int mostValues() {
        return (int) section(READERS).wholeNumber(MOST_VALUES);
    }
}
