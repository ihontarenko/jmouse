package org.jmouse.validator.dynamic;

public enum ConstraintPolicy {
    FAIL_FAST,       // throw / mark as config error
    COLLECT_ERRORS,  // return errors as normal validation failures
    LENIENT          // ignore unknown/missing arguments, best-effort
}
