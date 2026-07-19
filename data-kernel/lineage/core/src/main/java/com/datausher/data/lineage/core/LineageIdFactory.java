package com.datausher.data.lineage.core;

import com.datausher.data.lineage.api.LineageEdgeId;
import com.datausher.data.lineage.api.LineageEdgeInput;
import com.datausher.data.lineage.api.LineageNodeId;
import com.datausher.data.lineage.api.LineageNodeRef;
import com.datausher.data.lineage.api.LineageSourceRef;

public interface LineageIdFactory {
    LineageNodeId nodeId(LineageNodeRef reference);

    LineageEdgeId edgeId(LineageSourceRef source, LineageEdgeInput edge);
}
