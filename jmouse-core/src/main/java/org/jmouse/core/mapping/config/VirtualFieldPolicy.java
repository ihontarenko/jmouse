package org.jmouse.core.mapping.config;

public enum VirtualFieldPolicy {
    USE_VIRTUAL_IF_SOURCE_MISSING,
    USE_VIRTUAL_ALWAYS,
    DISABLE,
    FAIL_IF_REQUIRED_AND_NO_SOURCE_NO_VIRTUAL
}
