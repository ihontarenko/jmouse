package org.jmouse.util;

import java.util.UUID;

public class UUIDGenerator extends AbstractStringIdGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }

}
