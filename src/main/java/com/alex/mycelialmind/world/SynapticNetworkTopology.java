package com.alex.mycelialmind.world;

import net.minecraft.core.BlockPos;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.BiconnectivityInspector;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Pure JGraphT topology analyzer for the Mycelial Synaptic Network.
 * Computes connected clusters, articulation points (critical relay nodes), and weighted shortest paths.
 */
public final class SynapticNetworkTopology {

    public record TopologySummary(
            int nodeCount,
            int edgeCount,
            int clusterCount,
            Set<BlockPos> criticalNodes
    ) {
        public static final TopologySummary EMPTY = new TopologySummary(0, 0, 0, Set.of());

        public TopologySummary {
            criticalNodes = criticalNodes == null ? Set.of() : Set.copyOf(criticalNodes);
        }
    }

    private SynapticNetworkTopology() {
    }

    public static SimpleWeightedGraph<BlockPos, DefaultWeightedEdge> buildGraph(
            Collection<BlockPos> nodes,
            BiPredicate<BlockPos, BlockPos> edgePredicate
    ) {
        SimpleWeightedGraph<BlockPos, DefaultWeightedEdge> graph =
                new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        if (nodes == null || nodes.isEmpty()) {
            return graph;
        }
        List<BlockPos> ordered = new ArrayList<>(nodes.size());
        for (BlockPos pos : nodes) {
            if (pos != null) {
                BlockPos immutable = pos.immutable();
                if (graph.addVertex(immutable)) {
                    ordered.add(immutable);
                }
            }
        }
        if (edgePredicate == null) {
            return graph;
        }
        for (int i = 0; i < ordered.size(); i++) {
            BlockPos a = ordered.get(i);
            for (int j = i + 1; j < ordered.size(); j++) {
                BlockPos b = ordered.get(j);
                if (edgePredicate.test(a, b)) {
                    DefaultWeightedEdge edge = graph.addEdge(a, b);
                    if (edge != null) {
                        graph.setEdgeWeight(edge, Math.max(1.0D, Math.sqrt(a.distSqr(b))));
                    }
                }
            }
        }
        return graph;
    }

    public static TopologySummary analyze(SimpleWeightedGraph<BlockPos, DefaultWeightedEdge> graph) {
        if (graph == null || graph.vertexSet().isEmpty()) {
            return TopologySummary.EMPTY;
        }
        BiconnectivityInspector<BlockPos, DefaultWeightedEdge> inspector =
                new BiconnectivityInspector<>(graph);
        int clusters = inspector.getConnectedComponents().size();
        Set<BlockPos> cutpoints = inspector.getCutpoints();
        return new TopologySummary(
                graph.vertexSet().size(),
                graph.edgeSet().size(),
                clusters,
                cutpoints
        );
    }

    public static List<BlockPos> findShortestPath(
            SimpleWeightedGraph<BlockPos, DefaultWeightedEdge> graph,
            BlockPos start,
            BlockPos goal
    ) {
        if (graph == null || start == null || goal == null) {
            return List.of();
        }
        if (!graph.containsVertex(start) || !graph.containsVertex(goal)) {
            return List.of();
        }
        GraphPath<BlockPos, DefaultWeightedEdge> path = DijkstraShortestPath.findPathBetween(graph, start, goal);
        if (path == null) {
            return List.of();
        }
        return Collections.unmodifiableList(path.getVertexList());
    }
}
