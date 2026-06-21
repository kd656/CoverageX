package com.coveragex.core.fixtures.loops;

public class NestedLoops {
    public int[][] buildMatrix(int rows, int cols) {
        int[][] matrix = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                matrix[r][c] = r * cols + c;
            }
        }
        return matrix;
    }
}
