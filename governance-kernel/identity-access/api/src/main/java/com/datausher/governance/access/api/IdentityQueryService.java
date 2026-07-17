package com.datausher.governance.access.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface IdentityQueryService {
    Optional<Subject> find(SubjectRef ref);

    PageResult<Subject> search(SubjectQuery query, PageRequest pageRequest);
}
