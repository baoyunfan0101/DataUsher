package com.datausher.governance.ownership.api;

public interface OwnershipCommandService {
    ResourceOwner assign(AssignResourceOwnerRequest request);

    void remove(RemoveResourceOwnerRequest request);
}
