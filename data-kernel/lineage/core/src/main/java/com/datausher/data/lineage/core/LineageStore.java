package com.datausher.data.lineage.core;

import com.datausher.data.lineage.api.LineageDirection;
import com.datausher.data.lineage.api.LineageEdge;
import com.datausher.data.lineage.api.LineageEdgeQuery;
import com.datausher.data.lineage.api.LineageEdgeType;
import com.datausher.data.lineage.api.LineageNode;
import com.datausher.data.lineage.api.LineageNodeId;
import com.datausher.data.lineage.api.LineageNodeQuery;
import com.datausher.data.lineage.api.LineageNodeRef;
import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LineageStore {
    boolean apply(LineageMutation mutation);

    Optional<LineageNode> findNode(LineageNodeId nodeId);

    Optional<LineageNode> findNode(LineageNodeRef reference);

    PageResult<LineageNode> searchNodes(LineageNodeQuery query, PageRequest pageRequest);

    PageResult<LineageEdge> searchEdges(LineageEdgeQuery query, PageRequest pageRequest);

    List<LineageEdge> adjacentEdges(
            LineageNodeId nodeId,
            LineageDirection direction,
            Set<LineageEdgeType> edgeTypes
    );
}
