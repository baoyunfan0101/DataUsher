package com.datausher.governance.access.core;

import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectQuery;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.governance.access.api.SubjectStatus;
import com.datausher.governance.access.api.SubjectType;
import com.datausher.platform.shared.page.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemorySubjectStoreTest {
    @Test
    void appliesFilteringAndPaginationInsideTheStore() {
        InMemorySubjectStore store = new InMemorySubjectStore();
        store.save(subject("user-2", "Beta"));
        store.save(subject("user-1", "Alpha"));

        var result = store.search(
                SubjectQuery.all(),
                new PageRequest(2, 1, List.of())
        );

        assertEquals(2, result.total());
        assertEquals("user-2", result.items().get(0).ref().subjectId());
    }

    private static Subject subject(String subjectId, String displayName) {
        return new Subject(
                new SubjectRef(SubjectType.USER, subjectId),
                displayName,
                SubjectStatus.ACTIVE,
                Map.of()
        );
    }
}
