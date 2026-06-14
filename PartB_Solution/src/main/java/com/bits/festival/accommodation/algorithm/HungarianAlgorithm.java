package com.bits.festival.accommodation.algorithm;

import java.util.Arrays;

/**
 * Kuhn–Munkres (Hungarian) algorithm for the <strong>minimum-cost perfect assignment</strong>
 * problem on a square cost matrix, running in O(n³) time.
 *
 * <p>This is the mathematical core of the component. Given an {@code n×n} matrix where
 * {@code cost[i][j]} is the cost of assigning row {@code i} to column {@code j}, it finds a
 * one-to-one assignment of rows to columns that minimises the total cost.
 *
 * <p>The implementation uses the dual/potentials formulation with successive shortest
 * augmenting paths (the classic O(n³) variant). It works purely on {@code double[][]} and has
 * no dependency on the accommodation domain, so it is independently unit-testable and
 * reusable (e.g. it could equally drive judge-to-event or volunteer-to-task assignment).
 */
public final class HungarianAlgorithm {

    private static final double INF = Double.POSITIVE_INFINITY;

    private HungarianAlgorithm() {
    }

    /**
     * Solve the assignment problem.
     *
     * @param cost a square {@code n×n} matrix of finite, non-negative-friendly costs. Values
     *             may be any finite double; callers represent "forbidden" pairings with a
     *             large finite sentinel rather than {@link Double#POSITIVE_INFINITY}.
     * @return an array {@code assignment} of length {@code n} where {@code assignment[i]} is
     *         the column assigned to row {@code i}. Every column appears exactly once.
     * @throws IllegalArgumentException if the matrix is null, empty, non-square, or contains
     *                                  a non-finite entry.
     */
    public static int[] solve(double[][] cost) {
        validateSquareFinite(cost);
        final int n = cost.length;

        // 1-indexed potentials/state as in the canonical formulation.
        final double[] u = new double[n + 1]; // potential for rows
        final double[] v = new double[n + 1]; // potential for columns
        final int[] p = new int[n + 1];       // p[j] = row matched to column j (0 = none)
        final int[] way = new int[n + 1];     // back-pointers to reconstruct the augmenting path

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0; // current column on the augmenting path (0 = virtual start)
            final double[] minv = new double[n + 1];
            final boolean[] used = new boolean[n + 1];
            Arrays.fill(minv, INF);

            // Dijkstra-like search for the shortest augmenting path from row i.
            do {
                used[j0] = true;
                final int i0 = p[j0];
                double delta = INF;
                int j1 = -1;

                for (int j = 1; j <= n; j++) {
                    if (used[j]) {
                        continue;
                    }
                    double cur = cost[i0 - 1][j - 1] - u[i0] - v[j];
                    if (cur < minv[j]) {
                        minv[j] = cur;
                        way[j] = j0;
                    }
                    if (minv[j] < delta) {
                        delta = minv[j];
                        j1 = j;
                    }
                }

                // Update potentials along the path.
                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }
                j0 = j1;
            } while (p[j0] != 0);

            // Augment: walk the back-pointers, shifting matches by one.
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        // Convert column->row matching into row->column assignment.
        final int[] assignment = new int[n];
        for (int j = 1; j <= n; j++) {
            assignment[p[j] - 1] = j - 1;
        }
        return assignment;
    }

    /**
     * Total cost of an assignment returned by {@link #solve(double[][])}.
     */
    public static double totalCost(double[][] cost, int[] assignment) {
        double sum = 0.0;
        for (int i = 0; i < assignment.length; i++) {
            sum += cost[i][assignment[i]];
        }
        return sum;
    }

    private static void validateSquareFinite(double[][] cost) {
        if (cost == null || cost.length == 0) {
            throw new IllegalArgumentException("cost matrix must be non-empty");
        }
        final int n = cost.length;
        for (int i = 0; i < n; i++) {
            if (cost[i] == null || cost[i].length != n) {
                throw new IllegalArgumentException("cost matrix must be square (n x n)");
            }
            for (int j = 0; j < n; j++) {
                if (!Double.isFinite(cost[i][j])) {
                    throw new IllegalArgumentException(
                            "cost[" + i + "][" + j + "] is not finite; "
                                    + "represent forbidden pairings with a large finite value");
                }
            }
        }
    }
}
