package com.datausher.governance.access.core;

import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectQuery;
import com.datausher.governance.access.api.SubjectRef;

import java.util.List;
import java.util.Optional;

public interface SubjectStore {
    void save(Subject subject);

    Optional<Subject> find(SubjectRef ref);

    List<Subject> search(SubjectQuery query);
}
