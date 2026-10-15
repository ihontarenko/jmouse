package org.jmouse.notifier;

/**
 * Who a notification comes from.
 *
 * <p>⚠️ <strong>Chosen by the code that publishes, never by the text's author.</strong> A rule or a
 * message that could name its own sender could impersonate the product — and
 * <em>"System: your session has expired, sign in at…"</em> is the message that gets written the week
 * after somebody notices. So the product constructs this from what it knows to be true (the rule that
 * is running, the signed-in person), not from anything a caller typed.
 */
public sealed interface Sender {

    /**
     * How a channel names the sender to a reader.
     *
     * <p>Plain words with no markup; a channel escapes them like any other value.
     */
    String label();

    /**
     * The product itself, or one of its parts.
     *
     * @param name which part — {@code "Kitsu"}, {@code "Scanner"}
     */
    record System(String name) implements Sender {

        public System {
            requireText(name, "A system sender needs a name");
        }

        @Override
        public String label() {
            return name;
        }
    }

    /**
     * An automation rule — a jMS document, a scheduled job.
     *
     * @param name the rule as its author named it
     */
    record Rule(String name) implements Sender {

        public Rule {
            requireText(name, "A rule sender needs the rule's name");
        }

        @Override
        public String label() {
            return "Rule: " + name;
        }
    }

    /**
     * A person, writing through the product — from a screen, or from their phone through a bot.
     *
     * @param subjectId   the product's identifier for them — what an audit trail keeps
     * @param displayName what a reader sees
     */
    record Person(String subjectId, String displayName) implements Sender {

        public Person {
            requireText(subjectId, "A person sender needs the product's identifier for them");
            requireText(displayName, "A person sender needs a name to show");
        }

        @Override
        public String label() {
            return displayName;
        }
    }

    private static void requireText(String value, String complaint) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(complaint);
        }
    }
}
