package org.jmouse.jdbc.dialect.meta;

public record DatabaseInfo(
        String productName,
        int majorVersion,
        int minorVersion
) { }
