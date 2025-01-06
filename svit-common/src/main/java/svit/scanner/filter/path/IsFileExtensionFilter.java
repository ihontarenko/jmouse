package svit.scanner.filter.path;

import java.nio.file.Path;

public class IsFileExtensionFilter extends AbstractPathFilter {

    private final String extension;

    public IsFileExtensionFilter(String extension, boolean invert) {
        super(invert);
        this.extension = extension;
    }

    public IsFileExtensionFilter(String extension) {
        this(extension, false);
    }

    @Override
    public boolean accept(Path object) {
        String filename = object.toFile().toString();
        return filename.substring(filename.lastIndexOf('.') + 1).equals(extension) != invert;
    }

}
