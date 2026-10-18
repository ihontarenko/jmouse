package org.jmouse.java.index;

import java.util.ArrayList;
import java.util.List;

/**
 * How "go to class" reads what somebody types: a prefix, a run of camel humps, or a fragment.
 *
 * <p>{@code BeBi}, {@code BB} and {@code BeanB} all find {@code BeanBinder}: each capital in the
 * pattern starts a hump that has to begin a hump of the name, in order, and humps of the name may
 * be skipped. A lower-case pattern also matches anywhere inside the name, ranked last.
 */
final class TypeNameMatcher {

    static final int NO_MATCH = -1;

    private static final int EXACT = 400;
    private static final int PREFIX = 300;
    private static final int HUMPS = 200;
    private static final int FRAGMENT = 100;

    private TypeNameMatcher() {}

    /**
     * Scores a name against a pattern; higher is a better match, and a shorter name wins among
     * equals.
     *
     * @return the score, or {@link #NO_MATCH}
     */
    static int score(String pattern, String name) {
        if (pattern.isEmpty()) {
            return NO_MATCH;
        }
        int lengthPenalty = Math.min(name.length(), 99);
        if (name.equalsIgnoreCase(pattern)) {
            return EXACT - lengthPenalty;
        }
        if (name.regionMatches(true, 0, pattern, 0, pattern.length())) {
            return PREFIX - lengthPenalty;
        }
        if (matchesHumps(humps(pattern), humps(name))) {
            return HUMPS - lengthPenalty;
        }
        if (pattern.equals(pattern.toLowerCase()) && name.toLowerCase().contains(pattern)) {
            return FRAGMENT - lengthPenalty;
        }
        return NO_MATCH;
    }

    private static boolean matchesHumps(List<String> patternHumps, List<String> nameHumps) {
        int nameIndex = 0;
        for (String patternHump : patternHumps) {
            while (nameIndex < nameHumps.size()
                    && !nameHumps
                            .get(nameIndex)
                            .regionMatches(true, 0, patternHump, 0, patternHump.length())) {
                nameIndex++;
            }
            if (nameIndex == nameHumps.size()) {
                return false;
            }
            nameIndex++;
        }
        return true;
    }

    /**
     * {@code BeanBinder} → {@code Bean}, {@code Binder}; {@code URLDecoder} → {@code U}, {@code R},
     * {@code L}, {@code Decoder}.
     */
    private static List<String> humps(String text) {
        List<String> humps = new ArrayList<>();
        int start = 0;
        for (int index = 1; index < text.length(); index++) {
            if (Character.isUpperCase(text.charAt(index)) || text.charAt(index) == '_') {
                humps.add(text.substring(start, index));
                start = text.charAt(index) == '_' ? index + 1 : index;
            }
        }
        if (start < text.length()) {
            humps.add(text.substring(start));
        }
        return humps;
    }
}
