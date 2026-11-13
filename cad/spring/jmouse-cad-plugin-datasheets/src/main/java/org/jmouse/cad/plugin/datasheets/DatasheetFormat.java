package org.jmouse.cad.plugin.datasheets;

import org.jmouse.settings.Label;
import org.jmouse.cad.store.problem.CadProblem;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * What counts as a datasheet: the plugin's data ({@code settings/plugin/cad-datasheets/format}), and the two refusals that follow from it.
 *
 * @param extensions what a datasheet's file name ends in
 * @param mediaType  what it is served as
 * @param signature  what its first bytes say: a file only called a PDF is refused
 * @param linkLabel  what a document's {@code links} call its datasheet on the web
 */
public record DatasheetFormat(@Label("Extensions") List<String> extensions,
                              @Label("Media type") String mediaType,
                              @Label("Signature") String signature,
                              @Label("Link label") String linkLabel) {

    /** What the store keeps of a name. */
    private static final int LONGEST_NAME = 255;

    /** Whether a file a document holds is one of its datasheets; its 3D model's files are not. */
    public boolean names(String name) {
        String lowered = name.toLowerCase(Locale.ROOT);
        return extensions.stream().anyMatch(extension -> lowered.endsWith("." + extension));
    }

    /** A file name alone, of the kinds a datasheet is in: never a path. */
    public String requireName(String name) {
        if (name == null || name.isBlank() || name.length() > LONGEST_NAME || !name.equals(name.strip())) {
            throw CadProblem.invalid("A datasheet needs a file name of at most " + LONGEST_NAME + " characters.");
        }
        if (name.chars().anyMatch(character -> character == '/' || character == '\\' || Character.isISOControl(character))) {
            throw CadProblem.invalid("A datasheet's name is a file's name alone, not a path: " + name);
        }
        if (!names(name)) {
            throw CadProblem.invalid("A datasheet is a file of " + String.join(", ", extensions) + ": " + name);
        }
        return name;
    }

    /** Its first bytes say what its name does. */
    public void requireContent(String name, byte[] content) {
        byte[] expected = signature.getBytes(StandardCharsets.US_ASCII);
        if (content.length < expected.length || !Arrays.equals(content, 0, expected.length, expected, 0, expected.length)) {
            throw CadProblem.invalid(name + " is not a PDF: it does not begin as one.");
        }
    }
}
