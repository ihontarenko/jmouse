package org.jmouse.cad.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/**
 * {@code jmouse.cad.*}: what an installation tunes about the catalogue.
 *
 * @param documents what a document may be, and how many read documents are kept
 * @param library   the shipped library and the product's own shipped documents
 * @param revisions  the zone a revision's day is written in
 * @param simulation how much a simulation may ask, and how the longer ones are run
 * @param parts      how parts are looked for beyond the catalogue
 */
@ConfigurationProperties("jmouse.cad")
public record CadProperties(@DefaultValue Documents documents, @DefaultValue Library library,
                            @DefaultValue Revisions revisions, @DefaultValue Simulation simulation,
                            @DefaultValue Parts parts) {

    /** @param limit how many candidates each place parts are looked for in answers with at most */
    public record Parts(@DefaultValue("24") int limit) {
    }

    /**
     * @param maximumLength the longest text a document may be, in characters
     * @param cachedReadings how many read documents are kept by checksum, so a document is read once per revision
     */
    public record Documents(@DefaultValue("1000000") int maximumLength, @DefaultValue("2000") int cachedReadings) {
    }

    /**
     * @param importOnStart import the shipped documents into the catalogue when the application is ready
     * @param includeEngine whether jmouse-cad-library's own documents are among them
     * @param locations     the product's own shipped documents, as resource patterns: {@code classpath:catalogue/**}
     */
    public record Library(@DefaultValue("true") boolean importOnStart, @DefaultValue("true") boolean includeEngine,
                          @DefaultValue List<String> locations) {
    }

    /**
     * @param zone the zone a revision's day — {@code 20261027_001} — is written in; the system's when unset
     */
    public record Revisions(ZoneId zone) {

        public Revisions {
            zone = zone == null ? ZoneId.systemDefault() : zone;
        }
    }

    /**
     * @param inlineSamples  the most samples a simulation is answered with at once; more are a job
     * @param maximumSamples the most samples a simulation may ask for at all
     * @param workers        how many jobs run at once
     * @param jobRetention   how long a finished job and its result are kept for its reader to fetch
     * @param engineSamples  how many even steps another engine's run is printed in when the caller asks for none
     */
    public record Simulation(@DefaultValue("20000") int inlineSamples, @DefaultValue("100000") int maximumSamples,
                             @DefaultValue("1") int workers, @DefaultValue("PT30M") Duration jobRetention,
                             @DefaultValue("1000") int engineSamples) {
    }
}
