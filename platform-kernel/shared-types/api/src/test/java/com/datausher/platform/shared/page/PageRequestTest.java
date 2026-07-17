package com.datausher.platform.shared.page;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageRequestTest {
    @Test
    void calculatesOffsetWithoutIntegerOverflow() {
        PageRequest request = new PageRequest(Integer.MAX_VALUE, Integer.MAX_VALUE, List.of());

        assertEquals(4_611_686_011_984_936_962L, request.offset());
    }

    @Test
    void leavesQuerySpecificSizeLimitsToTheConsumer() {
        assertEquals(10_000, new PageRequest(1, 10_000, List.of()).size());
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(1, 0, List.of()));
    }

    @Test
    void snapshotsSortSpecifications() {
        List<SortSpec> sort = new ArrayList<>();
        sort.add(new SortSpec("name", SortDirection.ASC));

        PageRequest request = new PageRequest(1, 20, sort);
        sort.clear();

        assertEquals(1, request.sort().size());
        assertThrows(UnsupportedOperationException.class, () -> request.sort().clear());
    }
}
