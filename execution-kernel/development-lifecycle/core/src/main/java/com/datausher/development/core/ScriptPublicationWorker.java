package com.datausher.development.core;

import com.datausher.development.api.ScriptPublication;
import com.datausher.development.api.ScriptPublicationId;

public interface ScriptPublicationWorker {
    ScriptPublication submitApproval(ScriptPublicationId publicationId);
}
