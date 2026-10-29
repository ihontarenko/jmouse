package org.jmouse.gerber.node;

/** How Gerber's words compare: by their letter and the number after it, leading zeros aside — {@code G1} is {@code G01}. */
public final class GerberWords {

    private GerberWords() {
    }

    /** Whether a word is this code: its letter and the same number. */
    public static boolean sameCode(String word, String code) {
        if (word.isEmpty() || word.charAt(0) != code.charAt(0)) {
            return false;
        }
        try {
            return Integer.parseInt(word.substring(1)) == Integer.parseInt(code.substring(1));
        } catch (NumberFormatException notCode) {
            return false;
        }
    }

    /** The letter a word opens with. */
    public static char letterOf(String word) {
        return word.charAt(0);
    }

    /** The number after a word's letter, as an integer: {@code X-1500} is −1500. */
    public static long numberOf(String word) {
        return Long.parseLong(word.substring(1).replace("+", ""));
    }
}
