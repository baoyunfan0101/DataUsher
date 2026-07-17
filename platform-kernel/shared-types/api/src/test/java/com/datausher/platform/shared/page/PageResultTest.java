package com.datausher.platform.shared.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageResultTest {
    @Test
    void calculatesTotalPagesWithoutLongOverflow() {
        PageResult<String> result = new PageResult<>(List.of("item"), Long.MAX_VALUE, 1, 2);

        assertEquals(4_611_686_018_427_387_904L, result.totalPages());
    }

    @Test
    void rejectsInconsistentPageMetadata() {
        assertThrows(NullPointerException.class, () -> new PageResult<>(null, 0, 1, 10));
        assertThrows(IllegalArgumentException.class, () ->
                new PageResult<>(List.of("one", "two"), 1, 1, 10)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new PageResult<>(List.of("one", "two"), 2, 1, 1)
        );
    }
}
