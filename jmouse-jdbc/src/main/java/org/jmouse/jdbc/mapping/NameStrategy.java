package org.jmouse.jdbc.mapping;

@FunctionalInterface
public interface NameStrategy {

    static NameStrategy asIs() {
        return c -> c;
    }

    static NameStrategy toCamel() {
        return column -> {
            StringBuilder builder = new StringBuilder(column.length());
            boolean       upper   = false;
            for (int index = 0; index < column.length(); index++) {
                char character = column.charAt(index);
                if (character == '_' || character == '-' || character == ' ') {
                    upper = true;
                    continue;
                }
                builder.append(upper ? Character.toUpperCase(character) : character);
                upper = false;
            }
            return builder.toString();
        };
    }

    static NameStrategy toSnake() {
        return column -> {
            StringBuilder builder = new StringBuilder(column.length());
            boolean       upper   = false;
            for (int index = 0; index < column.length(); index++) {
                char c = column.charAt(index);
                char u = (index > 0 && index < column.length() - 1 ? '_' : 0);
                if (Character.isUpperCase(c)) {
                    upper = true;
                    continue;
                }
                builder.append(upper ? u + Character.toLowerCase(c) : c);
                upper = false;
            }
            return builder.toString();
        };
    }

    String toName(String label);
}
