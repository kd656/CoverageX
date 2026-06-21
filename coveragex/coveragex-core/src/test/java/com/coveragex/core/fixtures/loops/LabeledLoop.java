package com.coveragex.core.fixtures.loops;

public class LabeledLoop {
    public int find(int[][] matrix, int target) {
        int found = -1;
        OUTER:
        for (int r = 0; r < matrix.length; r++) {
            for (int c = 0; c < matrix[r].length; c++) {
                if (matrix[r][c] == target) {
                    found = r * matrix[r].length + c;
                    break OUTER;
                }
            }
        }
        return found;
    }
}
