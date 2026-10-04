package org.jmouse.telegram.jpa.smoke;

import org.jmouse.telegram.jpa.AesGcmCredentialCipher;
import org.jmouse.telegram.jpa.CredentialCipher;
import org.jmouse.telegram.smoke.Checks;

import java.security.SecureRandom;
import java.util.Base64;

import static org.jmouse.telegram.smoke.Checks.check;

/**
 * The credential cipher, checked without a database.
 *
 * <p>Worth its own smoke because the properties that matter here fail <em>silently</em>: a reused nonce
 * is not a weakening of GCM but a break, a short key accepted quietly means an installation believes it
 * configured encryption it did not, and a cipher that returned the sealed text on failure would send
 * ciphertext to Telegram as a token.
 *
 * <p>Run its {@code main}.
 */
public final class CipherSmoke {

    public static void main(String[] arguments) {
        sealsAndOpens();
        producesADifferentValueEachTime();
        refusesAKeyOfTheWrongLength();
        refusesAValueSealedUnderAnotherKey();
        refusesTamperedCiphertext();
        refusesSomethingThatWasNeverSealed();
        acceptsABase64Key();

        Checks.report();
    }

    private static void sealsAndOpens() {
        CredentialCipher cipher = cipher();
        String           token  = "7123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw";

        String sealed = cipher.seal(token);

        check("the sealed value is not the token", !sealed.equals(token));
        check("it does not contain the token", !sealed.contains("AAHdqTcv"));
        check("it opens back to the token", token.equals(cipher.open(sealed)));
    }

    private static void producesADifferentValueEachTime() {
        // ⚠️ The property this exists for. Reusing a nonce under one key in GCM leaks the XOR of the
        // plaintexts AND allows forgery - it is a break, not a weakening. Two identical tokens sealing
        // to the same string would be the visible symptom of that mistake.
        CredentialCipher cipher = cipher();

        String first  = cipher.seal("same-token");
        String second = cipher.seal("same-token");

        check("sealing the same token twice gives different values", !first.equals(second));
        check("both still open", "same-token".equals(cipher.open(first))
                && "same-token".equals(cipher.open(second)));
    }

    private static void refusesAKeyOfTheWrongLength() {
        // ⚠️ Not padded or hashed into shape: silently accepting a short key lets an installation
        // believe it configured AES-256 when it configured something else.
        check("a 16-byte key is refused",
                Checks.throwsIllegalArgument(() -> new AesGcmCredentialCipher(new byte[16])));

        check("a 33-byte key is refused",
                Checks.throwsIllegalArgument(() -> new AesGcmCredentialCipher(new byte[33])));

        check("a 32-byte key is accepted", new AesGcmCredentialCipher(new byte[32]) != null);
    }

    private static void refusesAValueSealedUnderAnotherKey() {
        String sealed = cipher().seal("a-token");

        check("another key cannot open it", throwsIllegalState(() -> cipher().open(sealed)));
    }

    private static void refusesTamperedCiphertext() {
        // GCM is authenticated, which is why this fails rather than decrypting to rubbish. A credential
        // that decrypted to rubbish would be sent to Telegram as a token, and the refusal would read as
        // "the credential was revoked" - sending somebody to rotate one that was fine.
        CredentialCipher cipher = cipher();
        byte[]           raw    = Base64.getDecoder().decode(cipher.seal("a-token"));

        raw[raw.length - 1] ^= 0x01;

        String tampered = Base64.getEncoder().encodeToString(raw);

        check("a tampered value is refused, not decrypted to rubbish",
                throwsIllegalState(() -> cipher.open(tampered)));
    }

    private static void refusesSomethingThatWasNeverSealed() {
        CredentialCipher cipher = cipher();

        check("plain text is refused", throwsIllegalState(() -> cipher.open("not base64 at all!!")));
        check("a too-short value is refused", throwsIllegalState(() -> cipher.open("AAAA")));
    }

    private static void acceptsABase64Key() {
        byte[] key = new byte[AesGcmCredentialCipher.KEY_LENGTH_BYTES];

        new SecureRandom().nextBytes(key);

        CredentialCipher cipher =
                AesGcmCredentialCipher.fromBase64Key(Base64.getEncoder().encodeToString(key));

        check("a base64 key works end to end", "token".equals(cipher.open(cipher.seal("token"))));

        check("a key that is not base64 is refused",
                Checks.throwsIllegalArgument(() -> AesGcmCredentialCipher.fromBase64Key("!!!")));
    }

    /** A fresh random key each call, which is what makes the cross-key checks meaningful. */
    private static CredentialCipher cipher() {
        byte[] key = new byte[AesGcmCredentialCipher.KEY_LENGTH_BYTES];

        new SecureRandom().nextBytes(key);

        return new AesGcmCredentialCipher(key);
    }

    private static boolean throwsIllegalState(Runnable call) {
        try {
            call.run();
            return false;
        } catch (IllegalStateException expected) {
            return true;
        }
    }

    private CipherSmoke() {
    }
}
