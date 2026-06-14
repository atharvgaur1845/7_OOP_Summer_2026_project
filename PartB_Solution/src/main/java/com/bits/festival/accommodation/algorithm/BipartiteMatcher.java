package com.bits.festival.accommodation.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Maximum-cardinality bipartite matching via Kuhn's augmenting-path algorithm (O(V·E)).
 *
 * <p>Used as a <em>feasibility fallback</em> after the cost-minimising Hungarian pass: any
 * participant the optimal assignment left unseated (matched to a dummy bed or a forbidden
 * cell) is re-matched against the beds that ended up empty, considering only feasible edges.
 * This maximises the number of placed participants even when minimising cost alone would have
 * stranded someone who could still fit somewhere.
 *
 * <p>Operates on a pure adjacency list of left-vertices→right-vertices, keeping it
 * domain-agnostic and unit-testable.
 */
public final class BipartiteMatcher {

    private BipartiteMatcher() {
    }

    /**
     * @param adjacency {@code adjacency.get(l)} = right-vertex indices reachable from left
     *                  vertex {@code l} (a feasible edge)
     * @param rightSize number of right vertices
     * @return {@code matchLeft} of length {@code adjacency.size()} where {@code matchLeft[l]}
     *         is the right vertex matched to left {@code l}, or {@code -1} if unmatched
     */
    public static int[] match(List<List<Integer>> adjacency, int rightSize) {
        final int leftSize = adjacency.size();
        final int[] matchRight = new int[rightSize]; // right -> left
        final int[] matchLeft = new int[leftSize];   // left -> right
        Arrays.fill(matchRight, -1);
        Arrays.fill(matchLeft, -1);

        for (int l = 0; l < leftSize; l++) {
            boolean[] visited = new boolean[rightSize];
            tryKuhn(l, adjacency, visited, matchRight);
        }
        for (int r = 0; r < rightSize; r++) {
            if (matchRight[r] != -1) {
                matchLeft[matchRight[r]] = r;
            }
        }
        return matchLeft;
    }

    private static boolean tryKuhn(int l, List<List<Integer>> adjacency,
                                   boolean[] visited, int[] matchRight) {
        for (int r : adjacency.get(l)) {
            if (visited[r]) {
                continue;
            }
            visited[r] = true;
            if (matchRight[r] == -1 || tryKuhn(matchRight[r], adjacency, visited, matchRight)) {
                matchRight[r] = l;
                return true;
            }
        }
        return false;
    }

    /** Convenience: build an empty adjacency list of the given size. */
    public static List<List<Integer>> newAdjacency(int leftSize) {
        List<List<Integer>> adj = new ArrayList<>(leftSize);
        for (int i = 0; i < leftSize; i++) {
            adj.add(new ArrayList<>());
        }
        return adj;
    }
}
