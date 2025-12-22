package org.jmouse.jdbc.database;

public record DatabaseInformation(
        String productName,
        String productVersion,
        int majorVersion,
        int minorVersion
) { }