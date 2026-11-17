package org.jmouse.j2js.teavm;

import org.teavm.extension.spi.substitution.SimpleSubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;

/**
 * 🌐 The JDK classes TeaVM's class library lacks, written here the way TeaVM writes its own: {@code java.util.StringJoiner} is
 * {@code org.jmouse.j2js.teavm.classlib.java.util.TStringJoiner}. A JDK class with no class here is TeaVM's, as before. Found by
 * TeaVM on the class path it compiles ({@code META-INF/services/org.teavm.extension.spi.substitution.SubstitutionPolicy}).
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ClassLibrarySubstitution extends SimpleSubstitutionPolicy {

    /** Where the substitutes are, by the JDK package they stand in. */
    public static final String CLASS_LIBRARY_PACKAGE = "org.jmouse.j2js.teavm.classlib.";

    /** What a substitute's name opens with, as TeaVM's own do. */
    public static final String SUBSTITUTE_PREFIX = "T";

    /**
     * The JDK class a substitute stands in — {@code java.util.StringJoiner} for
     * {@code org.jmouse.j2js.teavm.classlib.java.util.TStringJoiner}, {@code java.util.concurrent.ConcurrentHashMap$KeySetView}
     * for {@code …TConcurrentHashMap$KeySetView}; any other class is its own.
     */
    public static String originalName(String className) {
        if (!className.startsWith(CLASS_LIBRARY_PACKAGE)) {
            return className;
        }

        String inLibrary = className.substring(CLASS_LIBRARY_PACKAGE.length());
        int    dot       = inLibrary.lastIndexOf('.');
        String simple    = inLibrary.substring(dot + 1);

        if (!simple.startsWith(SUBSTITUTE_PREFIX)) {
            return className;
        }

        return inLibrary.substring(0, dot + 1) + simple.substring(SUBSTITUTE_PREFIX.length());
    }

    @Override
    public void contribute(SubstitutionSink sink) {
        sink.selectClasses(inPackage("java", true))
                .packagePrefix(CLASS_LIBRARY_PACKAGE)
                .simpleNamePrefix(SUBSTITUTE_PREFIX);
    }
}
