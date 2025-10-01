package org.jmouse.security.core.password;

public final class PasswordEncoderFactory {

    public PasswordEncoder getEncoder(EncoderAlgorithm algorithm) {
        return switch (algorithm) {
            case PBKDF2_SHA256 -> new Pbkdf2Sha256PasswordEncoder(120_000, 16, 32);  // 120k iters, 16B salt, 32B dk
            default -> throw new IllegalStateException("UNEXPECTED ALGORITHM: " + algorithm);
        };
    }

}
