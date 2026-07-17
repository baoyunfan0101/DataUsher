package com.datausher.integration.runtime.api;

import java.util.List;
import java.util.Optional;

public interface CredentialBindingService {
    CredentialBinding bind(CredentialBinding binding);

    Optional<CredentialBinding> unbind(String bindingId);

    Optional<CredentialBinding> find(String bindingId);

    List<CredentialBinding> listByAdapter(String adapterId);
}
