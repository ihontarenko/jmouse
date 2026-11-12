package org.jmouse.suppliers;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpCallException;
import org.jmouse.http.client.HttpReply;
import org.jmouse.http.client.HttpTransport;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/**
 * One call to a distributor, as every driver makes it: what it answers read as JSON, and every way it can fail turned
 * into a {@link SupplierFailure} whose words carry no credential.
 *
 * <p>The cause is left off the failure on purpose: nothing it holds is needed by a reader, and a failure with no cause
 * cannot print an address that carries a key.</p>
 */
final class SupplierExchange {

    /** How much of a refusal's body is repeated: a gateway may answer with a whole page. */
    private static final int MOST_WORDS = 300;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpTransport transport;

    SupplierExchange(HttpTransport transport) {
        this.transport = transport;
    }

    /** The answer's JSON. */
    JsonNode answerTo(HttpCall call, SupplierAccess access) {
        HttpReply reply = replyTo(call, access);
        if (!reply.successful()) {
            throw refusal(reply, access);
        }
        try {
            return MAPPER.readTree(reply.text());
        } catch (JacksonException unreadable) {
            throw new SupplierFailure(SupplierFailure.Reason.UNREADABLE, access.label() + " answered something that is not JSON.");
        }
    }

    /** A request's body as JSON. */
    static String json(Object body) {
        return MAPPER.writeValueAsString(body);
    }

    /** A field's text; null where it is absent, null or blank — a distributor's way of not saying. */
    static String textOf(JsonNode node) {
        if (node.isMissingNode() || node.isNull() || node.isContainer()) {
            return null;
        }
        String text = node.asString().strip();
        return text.isEmpty() ? null : text;
    }

    /** A numeric field; null where it holds no number: an absent quantity is not zero. */
    static Long wholeNumberOf(JsonNode node) {
        return node.isNumber() ? Long.valueOf(node.longValue()) : digitsOf(textOf(node));
    }

    /** A numeric field's exact value; null where it holds no number. */
    static BigDecimal decimalOf(JsonNode node) {
        return node.isNumber() ? node.decimalValue() : null;
    }

    /** The number written in a text — {@code 1,234 In Stock} — or null where it has no digit. */
    static Long digitsOf(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder();
        text.chars().filter(Character::isDigit).forEach(digits::appendCodePoint);
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(digits.toString());
        } catch (NumberFormatException tooLong) {
            return null;
        }
    }

    private HttpReply replyTo(HttpCall call, SupplierAccess access) {
        try {
            return transport.exchange(call);
        } catch (HttpCallException noAnswer) {
            SupplierFailure.Reason reason = noAnswer.failure() == HttpCallException.Failure.INTERRUPTED
                    ? SupplierFailure.Reason.INTERRUPTED : SupplierFailure.Reason.UNREACHABLE;
            throw new SupplierFailure(reason, access.label() + " did not answer: "
                    + access.credentials().withoutSecrets(noAnswer.getMessage()));
        }
    }

    private static SupplierFailure refusal(HttpReply reply, SupplierAccess access) {
        SupplierFailure.Reason reason = switch (reply.status()) {
            case 401, 403 -> SupplierFailure.Reason.CREDENTIALS_REFUSED;
            case 429 -> SupplierFailure.Reason.RATE_LIMITED;
            default -> SupplierFailure.Reason.REFUSED;
        };
        String words = access.credentials().withoutSecrets(reply.text()).strip();
        if (words.length() > MOST_WORDS) {
            words = words.substring(0, MOST_WORDS) + "…";
        }
        return new SupplierFailure(reason, access.label() + " answered HTTP " + reply.status()
                + (words.isEmpty() ? "." : ": " + words));
    }
}
