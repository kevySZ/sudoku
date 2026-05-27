package com.kevy.sudoku.game;

public final class SudokuSolver {
    private static final int SIZE = 9;
    private static final int CELL_COUNT = 81;
    private static final int ALL_DIGITS_MASK = 0x3FE;

    public int countSolutions(int[] puzzle, int limit) {
        if (puzzle == null || puzzle.length != CELL_COUNT || limit <= 0) {
            return 0;
        }
        int[] board = puzzle.clone();
        int[] rowMasks = new int[SIZE];
        int[] columnMasks = new int[SIZE];
        int[] boxMasks = new int[SIZE];
        for (int index = 0; index < CELL_COUNT; index++) {
            int value = board[index];
            if (value == 0) {
                continue;
            }
            if (value < 1 || value > 9) {
                return 0;
            }
            int row = index / SIZE;
            int column = index % SIZE;
            int box = boxIndex(row, column);
            int bit = 1 << value;
            if ((rowMasks[row] & bit) != 0 || (columnMasks[column] & bit) != 0 || (boxMasks[box] & bit) != 0) {
                return 0;
            }
            rowMasks[row] |= bit;
            columnMasks[column] |= bit;
            boxMasks[box] |= bit;
        }
        return solve(board, rowMasks, columnMasks, boxMasks, limit, 0);
    }

    private int solve(int[] board, int[] rowMasks, int[] columnMasks, int[] boxMasks, int limit, int count) {
        if (count >= limit) {
            return count;
        }

        int bestIndex = -1;
        int bestMask = 0;
        int bestCount = Integer.MAX_VALUE;
        for (int index = 0; index < CELL_COUNT; index++) {
            if (board[index] != 0) {
                continue;
            }
            int row = index / SIZE;
            int column = index % SIZE;
            int mask = candidates(row, column, rowMasks, columnMasks, boxMasks);
            int optionCount = Integer.bitCount(mask);
            if (optionCount == 0) {
                return count;
            }
            if (optionCount < bestCount) {
                bestCount = optionCount;
                bestIndex = index;
                bestMask = mask;
                if (optionCount == 1) {
                    break;
                }
            }
        }

        if (bestIndex == -1) {
            return count + 1;
        }

        int row = bestIndex / SIZE;
        int column = bestIndex % SIZE;
        int box = boxIndex(row, column);
        int mask = bestMask;
        while (mask != 0 && count < limit) {
            int bit = mask & -mask;
            int value = Integer.numberOfTrailingZeros(bit);
            board[bestIndex] = value;
            rowMasks[row] |= bit;
            columnMasks[column] |= bit;
            boxMasks[box] |= bit;

            count = solve(board, rowMasks, columnMasks, boxMasks, limit, count);

            rowMasks[row] &= ~bit;
            columnMasks[column] &= ~bit;
            boxMasks[box] &= ~bit;
            board[bestIndex] = 0;
            mask &= ~bit;
        }
        return count;
    }

    private int candidates(int row, int column, int[] rowMasks, int[] columnMasks, int[] boxMasks) {
        return ALL_DIGITS_MASK & ~(rowMasks[row] | columnMasks[column] | boxMasks[boxIndex(row, column)]);
    }

    private static int boxIndex(int row, int column) {
        return (row / 3) * 3 + column / 3;
    }
}
