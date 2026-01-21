package org.jmouse.core;

public class PrefixedIdGenerator extends AbstractStringIdGenerator {

    private static final IdGenerator<String, String> DEFAULT_ID_GENERATOR = new SecureRandomStringGenerator(16);
    private static final String                      DEFAULT_PREFIX       = "jMouse_";

    private final IdGenerator<String, String> idGenerator;
    private final String                      prefix;

    public PrefixedIdGenerator(IdGenerator<String, String> idGenerator, String prefix) {
        this.idGenerator = idGenerator;
        this.prefix = prefix;
    }

    public static IdGenerator<String, String> defaultGenerator() {
        return prefixedGenerator(DEFAULT_PREFIX);
    }

    public static IdGenerator<String, String> prefixedGenerator(String prefix) {
        return new PrefixedIdGenerator(DEFAULT_ID_GENERATOR, prefix);
    }

    @Override
    public String generate() {
        return prefix + idGenerator.generate();
    }
}
