package org.jmouse.jdbc.bind;

import java.util.List;

public record NamedSQL(
        String normal,
        String parsed,
        List<String> parameters
) { }