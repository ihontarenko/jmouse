package org.jmouse.beans.events;

final class DeduplicateKey {

    private DeduplicateKey() {
    }

    static String of(Object... parts) {
        StringBuilder builder = new StringBuilder(128);

        for (Object part : parts) {
            if (!builder.isEmpty()) {
                builder.append('|');
            }
            builder.append(token(part));
        }

        return builder.toString();
    }

    private static String token(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return "-";
        }
        return text.replace('|', '_');
    }
}
