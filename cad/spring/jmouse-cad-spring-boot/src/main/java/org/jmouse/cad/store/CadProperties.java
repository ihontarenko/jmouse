package org.jmouse.cad.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;

/**
 * {@code jmouse.cad.*}: what an installation tunes about the catalogue.
 *
 * <p>Most of this is only what the engine's settings start from ({@code EngineConfiguration}): it is changed there, while
 * the engine runs.</p>
 *
 * @param documents what a document may be, and how many read documents are kept
 * @param library   the shipped library and the product's own shipped documents
 * @param revisions  the zone a revision's day is written in
 * @param simulation how much a simulation may ask, and how the longer ones are run
 * @param parts      how parts are looked for beyond the catalogue
 * @param projects   what a reader's projects start as
 */
@ConfigurationProperties("jmouse.cad")
public record CadProperties(@DefaultValue Documents documents, @DefaultValue Library library,
                            @DefaultValue Revisions revisions, @DefaultValue Simulation simulation,
                            @DefaultValue Parts parts, @DefaultValue Projects projects) {

    /** @param draftsTitle what the place a reader's loose work is kept in is called when the engine makes it */
    public record Projects(@DefaultValue("Drafts") String draftsTitle) {
    }

    /** @param limit how many candidates each place parts are looked for in answers with at most */
    public record Parts(@DefaultValue("24") int limit) {
    }

    /**
     * @param maximumLength the longest text a document may be, in characters
     * @param cachedReadings how many read documents are kept by checksum, so a document is read once per revision
     * @param defaultCategory the category a new document whose text names none is filed in — its text then says so —: the
     *                        library's own «Other» unless the installation names another; none when blank, the document left
     *                        unfiled. A category itself is never filed by it: one naming no parent stands at the top.
     * @param routingBoards how many pcbs an editor lays copper on are kept read at once, for the router to answer every move
     */
    public record Documents(@DefaultValue("1000000") int maximumLength, @DefaultValue("2000") int cachedReadings,
                            @DefaultValue("general") String defaultCategory, @DefaultValue("16") int routingBoards) {
    }

    /**
     * @param includeEngine whether jmouse-cad-library's own documents are among the shipped ones
     * @param locations     the product's own shipped documents, as resource patterns: {@code classpath:catalogue/**}
     * @param folder        the library folder: every document written here is a file of it, every revision a commit
     *                      (ADR 0002); the installation's, set by whoever runs it
     * @param identity      who a commit is by when no person is named: the engine writing, a file found in the folder
     */
    public record Library(@DefaultValue("true") boolean includeEngine, @DefaultValue List<String> locations, Path folder,
                          @DefaultValue Identity identity) {

        public Library {
            if (folder == null) {
                throw new IllegalStateException("jmouse.cad.library.folder names no library folder: the documents written here are "
                        + "its files, so the installation says where it is");
            }
        }
    }

    /**
     * @param name  the name a commit nobody named is by
     * @param email its address
     */
    public record Identity(@DefaultValue("jMouse CAD") String name, @DefaultValue("library@jmouse.invalid") String email) {
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
