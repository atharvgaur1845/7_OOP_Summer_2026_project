package com.bits.festival.accommodation.algorithm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the Kuhn–Munkres core. */
class HungarianAlgorithmTest {

    @Test
    void solvesClassicThreeByThreeToKnownOptimum() {
        double[][] cost = {
                {4, 1, 3},
                {2, 0, 5},
                {3, 2, 2}
        };
        int[] assignment = HungarianAlgorithm.solve(cost);
        assertEquals(5.0, HungarianAlgorithm.totalCost(cost, assignment), 1e-9);
        assertValidPermutation(assignment, 3);
    }

    @Test
    void everyEqualCostMatrixStillYieldsAValidPermutation() {
        double[][] cost = {
                {10, 10, 10},
                {10, 10, 10},
                {10, 10, 10}
        };
        int[] assignment = HungarianAlgorithm.solve(cost);
        assertEquals(30.0, HungarianAlgorithm.totalCost(cost, assignment), 1e-9);
        assertValidPermutation(assignment, 3);
    }

    @Test
    void picksTheCheapDiagonalWhenItIsOptimal() {
        double[][] cost = {
                {1, 9, 9},
                {9, 1, 9},
                {9, 9, 1}
        };
        int[] assignment = HungarianAlgorithm.solve(cost);
        assertArrayEquals(new int[]{0, 1, 2}, assignment);
        assertEquals(3.0, HungarianAlgorithm.totalCost(cost, assignment), 1e-9);
    }

    @Test
    void avoidsLargeFinitePenaltiesWhenAFeasibleAssignmentExists() {
        double BIG = 1_000_000.0;
        // Forcing row 0 away from col 0 (penalised) and row 1 away from col 1.
        double[][] cost = {
                {BIG, 1, 5},
                {2, BIG, 5},
                {5, 5, 1}
        };
        int[] assignment = HungarianAlgorithm.solve(cost);
        assertTrue(HungarianAlgorithm.totalCost(cost, assignment) < BIG,
                "solver should avoid forbidden cells when a feasible matching exists");
    }

    @Test
    void singleElementMatrix() {
        int[] assignment = HungarianAlgorithm.solve(new double[][]{{42}});
        assertArrayEquals(new int[]{0}, assignment);
    }

    @Test
    void rejectsNonSquareMatrix() {
        double[][] bad = {{1, 2, 3}, {4, 5, 6}};
        assertThrows(IllegalArgumentException.class, () -> HungarianAlgorithm.solve(bad));
    }

    @Test
    void rejectsNonFiniteEntries() {
        double[][] bad = {{1, Double.POSITIVE_INFINITY}, {2, 3}};
        assertThrows(IllegalArgumentException.class, () -> HungarianAlgorithm.solve(bad));
    }

    @Test
    void rejectsEmptyMatrix() {
        assertThrows(IllegalArgumentException.class, () -> HungarianAlgorithm.solve(new double[0][0]));
    }

    private static void assertValidPermutation(int[] assignment, int n) {
        boolean[] seen = new boolean[n];
        for (int col : assignment) {
            assertTrue(col >= 0 && col < n, "column index in range");
            assertTrue(!seen[col], "each column used once");
            seen[col] = true;
        }
    }
}
