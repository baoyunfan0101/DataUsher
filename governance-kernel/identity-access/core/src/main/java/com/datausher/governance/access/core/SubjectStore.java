package com.datausher.governance.access.core;

import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectQuery;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface SubjectStore {
    void save(Subject subject);

    Optional<Subject> find(SubjectRef ref);

    PageResult<Subject> search(SubjectQuery query, PageRequest pageRequest);
}
