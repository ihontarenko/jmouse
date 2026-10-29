package org.jmouse.security.password;

public final class PasswordEncoderFactory {

    public PasswordEncoder getEncoder(EncoderAlgorithm algorithm) {
        return switch (algorithm) {
            case PBKDF2_SHA256 -> new Pbkdf2Sha256PasswordEncoder();
            default -> throw new IllegalStateException("UNEXPECTED ALGORITHM: " + algorithm);
        };
    }

}
