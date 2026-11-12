package org.jmouse.eagle;

import java.util.Optional;

/**
 * An Eagle file as it was read: the version that wrote it, and its schematic or its board — a {@code .sch} holds the
 * one, a {@code .brd} the other.
 */
public record EagleDesign(String version, Optional<EagleSchematic> schematic, Optional<EagleBoard> board) {
}
