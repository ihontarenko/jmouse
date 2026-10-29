package org.jmouse.security.password;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class Pbkdf2Sha256PasswordEncoderTest {
    private final PasswordEncoder encoder = new Pbkdf2Sha256PasswordEncoder(1000, 16, 32);

    @Test
    void saltedRoundTripPreservesWhitespaceAndInput() {
        char[] password = " secret ".toCharArray();
        String first = encoder.encode(password);
        String second = encoder.encode(password);
        assertNotEquals(first, second);
        assertTrue(encoder.verify(password, first));
        assertFalse(encoder.verify("secret".toCharArray(), first));
        assertArrayEquals(" secret ".toCharArray(), password);
    }

    @Test
    void malformedRecordsAreRejected() {
        for (String record : new String[]{"", "pbkdf2", "pbkdf2$sha256$0$YQ==$Yg==",
                "pbkdf2$sha256$1$$", "pbkdf2$sha256$1$?$?"}) {
            assertFalse(encoder.verify("secret".toCharArray(), record));
        }
        assertFalse(encoder.verify((char[]) null, null));
    }

    @Test
    void untrustedWorkFactorIsBoundedBeforeDeriving() {
        assertTimeout(Duration.ofSeconds(1), () ->
                assertFalse(encoder.verify("secret".toCharArray(),
                        "pbkdf2$sha256$2147483647$YQ==$Yg==")));
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Pbkdf2Sha256PasswordEncoder(0, 16, 32));
        assertThrows(IllegalArgumentException.class, () -> new Pbkdf2Sha256PasswordEncoder(1000, 0, 32));
        assertThrows(IllegalArgumentException.class, () -> new Pbkdf2Sha256PasswordEncoder(1000, 16, 0));
    }

    @Test
    void strongerHistoricalRecordsCanUseAnExplicitVerificationLimit() {
        String record = new Pbkdf2Sha256PasswordEncoder(2000, 16, 32).encode("value".toCharArray());
        assertTrue(new Pbkdf2Sha256PasswordEncoder(1000, 16, 32, 3000).verify("value".toCharArray(), record));
    }
}
