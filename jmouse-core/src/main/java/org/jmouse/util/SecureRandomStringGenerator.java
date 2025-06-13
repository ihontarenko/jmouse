package org.jmouse.util;

import java.security.SecureRandom;
import java.util.Random;

public class SecureRandomStringGenerator extends RandomStringGenerator {

    protected SecureRandomStringGenerator(int length) {
        super(length);
    }

    @Override
    protected Random getRandom() {
        return new SecureRandom();
    }
}
