package org.jmouse.jdbc.query;

import org.jmouse.core.Verify;

public record PageRequest(OffsetLimit page, Sort sort) {

    public PageRequest {
        Verify.nonNull(page, "page");
        sort = (sort != null ? sort : Sort.unsorted());
    }

    public static PageRequest of(int offset, int limit) {
        return new PageRequest(OffsetLimit.of(offset, limit), Sort.unsorted());
    }

    public static PageRequest of(int offset, int limit, Sort sort) {
        return new PageRequest(OffsetLimit.of(offset, limit), sort);
    }

}
