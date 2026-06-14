package com.bits.festival.accommodation.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the maximum-cardinality bipartite matching fallback. */
class BipartiteMatcherTest {

    @Test
    void findsAPerfectMatchingWhenOneExists() {
        // 3 left, 3 right, identity edges -> perfect matching of size 3.
        List<List<Integer>> adj = BipartiteMatcher.newAdjacency(3);
        adj.get(0).add(0);
        adj.get(1).add(1);
        adj.get(2).add(2);
        int[] m = BipartiteMatcher.match(adj, 3);
        assertEquals(0, m[0]);
        assertEquals(1, m[1]);
        assertEquals(2, m[2]);
    }

    @Test
    void maximisesMatchesViaAugmentingPaths() {
        // L0->{0}, L1->{0,1}: greedy could stall but augmenting must place both.
        List<List<Integer>> adj = BipartiteMatcher.newAdjacency(2);
        adj.get(0).add(0);
        adj.get(1).add(0);
        adj.get(1).add(1);
        int[] m = BipartiteMatcher.match(adj, 2);
        assertEquals(2, matched(m));
    }

    @Test
    void leavesUnmatchableLeftVertexUnmatched() {
        List<List<Integer>> adj = BipartiteMatcher.newAdjacency(2);
        adj.get(0).add(0);
        // L1 has no feasible edge.
        int[] m = BipartiteMatcher.match(adj, 1);
        assertEquals(0, m[0]);
        assertEquals(-1, m[1]);
    }

    private static int matched(int[] m) {
        int c = 0;
        for (int v : m) {
            if (v >= 0) {
                c++;
            }
        }
        assertTrue(c >= 0);
        return c;
    }
}
