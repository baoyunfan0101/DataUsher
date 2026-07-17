package com.datausher.platform.shared.page;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(List<T> items, long total, int page, int size) {
    public PageResult {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        if (total < 0) {
            throw new IllegalArgumentException("total must be greater than or equal to 0");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than or equal to 1");
        }
        if (items.size() > size) {
            throw new IllegalArgumentException("items size must not exceed page size");
        }
        if (items.size() > total) {
            throw new IllegalArgumentException("items size must not exceed total");
        }
    }

    public static <T> PageResult<T> empty(PageRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return new PageResult<>(List.of(), 0, request.page(), request.size());
    }

    public long totalPages() {
        return total == 0 ? 0 : 1 + (total - 1) / size;
    }
}
