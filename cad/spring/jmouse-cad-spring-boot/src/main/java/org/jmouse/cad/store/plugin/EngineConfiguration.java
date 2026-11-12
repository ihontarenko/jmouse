package org.jmouse.cad.store.plugin;

import org.jmouse.cad.store.CadProperties;
import org.jmouse.plugin.PluginSetting;
import org.jmouse.plugin.PluginSettingValues;
import org.jmouse.plugin.PluginSettings;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * 🎚️ What the engine itself may be set to while it runs, as the first plugin's settings: read each time it is used, so a
 * change applies at once. {@link CadProperties} says what they start from, and forgetting a change brings it back. What
 * has to be known before the engine stands — its database, the key that seals these settings, how many threads it starts
 * — stays in the server's configuration.
 */
public final class EngineConfiguration {

    private static final String PART_LIMIT         = "part-limit";
    private static final String DOCUMENT_LENGTH    = "document-length";
    private static final String REVISION_ZONE      = "revision-zone";
    private static final String DEFAULT_CATEGORY   = "default-category";
    private static final String INLINE_SAMPLES     = "inline-samples";
    private static final String MOST_SAMPLES       = "most-samples";
    private static final String ENGINE_SAMPLES     = "engine-samples";
    private static final String JOB_RETENTION      = "job-retention";

    /** What a reader finds each setting under. */
    private static final String DOCUMENTS  = "Documents";
    private static final String PARTS      = "Part search";
    private static final String SIMULATION = "Simulation";

    private final PluginSettingValues values;

    public EngineConfiguration(PluginSettings settings) {
        this.values = settings.valuesOf(CadEnginePlugin.ID);
    }

    /** The settings the engine's plugin declares, each starting from what the server is configured with. */
    static List<PluginSetting> settingsFrom(CadProperties properties) {
        CadProperties.Simulation simulation = properties.simulation();
        return List.of(
                PluginSetting.wholeNumber(DOCUMENT_LENGTH, "Longest document", "The most text one document may be.",
                        properties.documents().maximumLength(), 1_000, 100_000_000, "characters").inGroup(DOCUMENTS),
                PluginSetting.text(DEFAULT_CATEGORY, "Category of the unfiled", "The category a new document that names none is filed in; none when empty.",
                        properties.documents().defaultCategory()).inGroup(DOCUMENTS),
                PluginSetting.choice(REVISION_ZONE, "Revision day", "The zone a revision's day is written in, as lm358@20261023_001 is.",
                        properties.revisions().zone().getId(), zonesWith(properties.revisions().zone())).inGroup(DOCUMENTS),
                PluginSetting.wholeNumber(PART_LIMIT, "Results from each place", "How many parts each place parts are looked for in answers with at most.",
                        properties.parts().limit(), 1, 500, "parts").inGroup(PARTS),
                PluginSetting.wholeNumber(INLINE_SAMPLES, "Answered at once", "The most samples a simulation is answered with at once; more run as a job.",
                        simulation.inlineSamples(), 100, 10_000_000, "samples").inGroup(SIMULATION),
                PluginSetting.wholeNumber(MOST_SAMPLES, "Most samples", "The most samples a simulation may ask for at all.",
                        simulation.maximumSamples(), 100, 100_000_000, "samples").inGroup(SIMULATION),
                PluginSetting.wholeNumber(ENGINE_SAMPLES, "Another engine's run", "How many even steps another engine's run is printed in when nobody says.",
                        simulation.engineSamples(), 2, 10_000_000, "samples").inGroup(SIMULATION),
                PluginSetting.duration(JOB_RETENTION, "Finished jobs kept", "How long a finished job and its result wait for whoever started it.",
                        simulation.jobRetention(), Duration.ofMinutes(1), Duration.ofDays(7)).inGroup(SIMULATION));
    }

    /** Every zone the platform knows by name, and the one configured even when it is none of them: {@code +02:00}. */
    private static List<PluginSetting.Choice> zonesWith(ZoneId configured) {
        TreeSet<String> zones = new TreeSet<>(ZoneId.getAvailableZoneIds());
        zones.add(configured.getId());
        List<PluginSetting.Choice> choices = new ArrayList<>(zones.size());
        zones.forEach(zone -> choices.add(new PluginSetting.Choice(zone, zone)));
        return choices;
    }

    public int partLimit() {
        return (int) values.wholeNumber(PART_LIMIT);
    }

    public int maximumDocumentLength() {
        return (int) values.wholeNumber(DOCUMENT_LENGTH);
    }

    /** The category a new document naming none is filed in; empty when the installation leaves such a document unfiled. */
    public Optional<String> defaultCategory() {
        return Optional.ofNullable(values.text(DEFAULT_CATEGORY)).map(String::strip).filter(code -> !code.isEmpty());
    }

    public ZoneId revisionZone() {
        return ZoneId.of(values.text(REVISION_ZONE));
    }

    public int inlineSamples() {
        return (int) values.wholeNumber(INLINE_SAMPLES);
    }

    public int maximumSamples() {
        return (int) values.wholeNumber(MOST_SAMPLES);
    }

    public int engineSamples() {
        return (int) values.wholeNumber(ENGINE_SAMPLES);
    }

    public Duration jobRetention() {
        return values.duration(JOB_RETENTION);
    }
}
