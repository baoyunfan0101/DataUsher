package com.datausher.data.lineage.api;

public interface LineageCommandService {
    LineageApplyResult applySnapshot(ApplyLineageSnapshotRequest request);
}
