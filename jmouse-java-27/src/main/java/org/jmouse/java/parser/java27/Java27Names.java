package org.jmouse.java.parser.java27;

import org.jmouse.java.model.version.JavaFeatureId;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Java27Names {
    private static final Properties VALUES = read();
    public static final String EXTENSION = require("extension.identity");
    public static final String REVISION = require("extension.revision");
    public static final String SPECIFICATION = require("specification.identity");
    public static final int RELEASE = Integer.parseInt(require("source.release"));
    public static final int CLASSFILE_MAJOR = Integer.parseInt(require("classfile.major"));
    public static final JavaFeatureId FEATURE = new JavaFeatureId(require("feature.identity"));
    public static final String INSPECT = require("keyword.inspect");
    public static final String BUNDLE = require("keyword.bundle");
    public static final String VALUE_PREFIX = require("token.value.spelling");
    public static final int VALUE_TOKEN_ID = Integer.parseInt(require("token.value.identity"));
    public static final String INSPECT_ROLE = require("role.inspect");
    public static final String VALUE_ROLE = require("role.value");
    public static final String IMPORT_ROLE = require("role.import");
    public static final String INSPECT_NODE = require("node.inspect");
    public static final String VALUE_NODE = require("node.value");
    public static final String IMPORT_NODE = require("node.import");
    public static final String STATEMENT_SELECTION = require("selection.statement");
    public static final String PRIMARY_SELECTION = require("selection.primary");
    public static final String IMPORT_SELECTION = require("selection.import");

    private Java27Names() {}

    private static String require(String key) {
        String value = VALUES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing Java 27 example metadata: " + key);
        }
        return value;
    }

    private static Properties read() {
        Properties properties = new Properties();
        try (InputStream input =
                Java27Names.class.getResourceAsStream("java27-example.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing Java 27 example metadata");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read example metadata", exception);
        }
        return properties;
    }
}
