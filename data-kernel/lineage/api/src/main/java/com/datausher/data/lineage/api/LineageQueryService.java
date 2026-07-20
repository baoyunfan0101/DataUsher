package com.datausher.data.lineage.api;

import com.datausher.platform.shared.page.PageRequest;
import com.datausher.platform.shared.page.PageResult;

import java.util.Optional;

public interface LineageQueryService {
    Optional<LineageNode> findNode(LineageNodeId nodeId);

    Optional<LineageNode> findNode(LineageNodeRef reference);

    PageResult<LineageNode> searchNodes(LineageNodeQuery query, PageRequest pageRequest);

    PageResult<LineageEdge> searchEdges(LineageEdgeQuery query, PageRequest pageRequest);

    LineageTraversalResult traverse(LineageTraversalQuery query);
}
