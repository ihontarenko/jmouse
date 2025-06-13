package org.jmouse.util;

import java.util.Random;

public class SimpleRandomStringGenerator extends RandomStringGenerator {
    public SimpleRandomStringGenerator(int length) {
        super(length);
    }

    @Override
    protected Random getRandom() {
        return new Random();
    }
}
