package svit.scanner.filter.path;

import java.nio.file.Path;

public class IsDirectoryPathFilter extends AbstractPathFilter {

    public IsDirectoryPathFilter() {
        super(false);
    }

    public IsDirectoryPathFilter(boolean invert) {
        super(invert);
    }

    @Override
    public boolean accept(Path object) {
        return object.toFile().isDirectory() != invert;
    }

}
