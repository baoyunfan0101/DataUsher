package com.datausher.governance.access.core;

import com.datausher.governance.access.api.IdentityQueryService;
import com.datausher.governance.access.api.Subject;
import com.datausher.governance.access.api.SubjectQuery;
import com.datausher.governance.access.api.SubjectRef;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Objects;
import java.util.Optional;

public final class DefaultIdentityQueryService implements IdentityQueryService {
    private final SubjectStore store;

    public DefaultIdentityQueryService(SubjectStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public Optional<Subject> find(SubjectRef ref) {
        return store.find(Objects.requireNonNull(ref, "ref must not be null"));
    }

    @Override
    public PageResult<Subject> search(SubjectQuery query, PageRequest pageRequest) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        return store.search(query, pageRequest);
    }
}
