package org.jmouse.java.parser.extension.fixture;

import org.jmouse.java.model.version.JavaFeatureId;

final class FixtureNames {
    static final String EXTENSION = "fixture.source.extension";
    static final String REVISION = "test-1";
    static final String TRACE_KEYWORD = "trace";
    static final String BUNDLE_KEYWORD = "bundle";
    static final String TRACE_PREFIX = "!>";
    static final int TRACE_TOKEN_ID = 40000;
    static final String TRACE_ROLE = "fixture.trace-statement";
    static final String TRACE_KIND = "fixture.trace-statement";
    static final String BUNDLE_KIND = "fixture.bundle-import";
    static final String TRACE_BINDING = "fixture.trace-parser";
    static final String TRACE_SELECTION = "fixture.trace-selection";
    static final String IMPORT_SELECTION = "fixture.import-selection";
    static final String EXPRESSION_CHILD = "expression";
    static final String NAME_CHILD = "name";
    static final String CATALOG = "fixture.java25";
    static final int SOURCE_RELEASE = 25;
    static final int CLASSFILE_MAJOR = 69;
    static final JavaFeatureId TRACE_FEATURE = new JavaFeatureId("fixture.trace");

    private FixtureNames() {
    }
}
