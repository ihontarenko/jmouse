package org.jmouse.gerber;

import java.util.List;
import java.util.Optional;

/**
 * What a Gerber layer is, as its file attributes say it.
 *
 * @param function the {@code %TF.FileFunction}: {@code Copper,L1,Top}, {@code Soldermask,Top}, {@code Profile,NP} …
 * @param positive whether dark is material ({@code Positive}) or taken away ({@code Negative})
 * @param software the program that wrote it, as {@code %TF.GenerationSoftware}: vendor, name, version; empty for none
 * @param project  the project the file belongs to, as {@code %TF.ProjectId}; empty for none
 */
public record GerberFile(String function, boolean positive, List<String> software, Optional<String> project) {

    public GerberFile {
        software = List.copyOf(software);
        if (!software.isEmpty() && software.size() != 3) {
            throw new IllegalArgumentException("The generation software is a vendor, a name and a version: " + software);
        }
    }

    /** A positive layer of this function, with no software or project said. */
    public static GerberFile of(String function) {
        return new GerberFile(function, true, List.of(), Optional.empty());
    }
}
