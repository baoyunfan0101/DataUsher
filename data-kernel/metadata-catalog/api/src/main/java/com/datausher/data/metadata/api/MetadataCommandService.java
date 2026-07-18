package com.datausher.data.metadata.api;

public interface MetadataCommandService {
    MetadataSyncResult synchronize(SynchronizeMetadataRequest request);
}
