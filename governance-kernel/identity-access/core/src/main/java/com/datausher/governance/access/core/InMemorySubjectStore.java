package com.datausher.governance.access.core;

import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectQuery;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemorySubjectStore implements SubjectStore {
    private final ConcurrentMap<String, Subject> subjects = new ConcurrentHashMap<>();

    @Override
    public void save(Subject subject) {
        subjects.put(subject.ref().canonicalValue(), subject);
    }

    @Override
    public Optional<Subject> find(SubjectRef ref) {
        return Optional.ofNullable(subjects.get(ref.canonicalValue()));
    }

    @Override
    public PageResult<Subject> search(SubjectQuery query, PageRequest pageRequest) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        List<Subject> matches = new ArrayList<>();
        for (Subject subject : subjects.values()) {
            String searchText = subject.ref().subjectId().toLowerCase() + " "
                    + subject.displayName().toLowerCase();
            if ((query.type() == null || query.type() == subject.ref().type())
                    && (query.status() == null || query.status() == subject.status())
                    && (query.text() == null || searchText.contains(query.text()))) {
                matches.add(subject);
            }
        }
        matches.sort(Comparator.comparing(subject -> subject.ref().canonicalValue()));
        int fromIndex = (int) Math.min(pageRequest.offset(), matches.size());
        int toIndex = (int) Math.min(
                (long) fromIndex + pageRequest.size(), matches.size());
        return new PageResult<>(
                matches.subList(fromIndex, toIndex),
                matches.size(),
                pageRequest.page(),
                pageRequest.size()
        );
    }
}
