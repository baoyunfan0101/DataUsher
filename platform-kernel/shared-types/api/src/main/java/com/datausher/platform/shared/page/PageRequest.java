package com.datausher.platform.shared.page;

import java.util.List;

public record PageRequest(int page, int size, List<SortSpec> sort) {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 100;

    public PageRequest {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than or equal to 1");
        }
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public static PageRequest firstPage() {
        return new PageRequest(DEFAULT_PAGE, DEFAULT_SIZE, List.of());
    }

    public long offset() {
        return (long) (page - 1) * size;
    }
}
