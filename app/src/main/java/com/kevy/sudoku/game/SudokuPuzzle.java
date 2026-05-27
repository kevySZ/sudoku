package com.kevy.sudoku.game;

public final class SudokuPuzzle {
    private static final int CELL_COUNT = 81;

    private final int[] givens;
    private final int[] solution;
    private final Difficulty difficulty;

    public SudokuPuzzle(int[] givens, int[] solution, Difficulty difficulty) {
        if (givens == null || givens.length != CELL_COUNT) {
            throw new IllegalArgumentException("givens must contain 81 cells");
        }
        if (solution == null || solution.length != CELL_COUNT) {
            throw new IllegalArgumentException("solution must contain 81 cells");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("difficulty is required");
        }
        this.givens = givens.clone();
        this.solution = solution.clone();
        this.difficulty = difficulty;
        validateConsistency();
    }

    public int[] getGivens() {
        return givens.clone();
    }

    public int[] getSolution() {
        return solution.clone();
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public boolean isGiven(int index) {
        return index >= 0 && index < CELL_COUNT && givens[index] != 0;
    }

    public boolean isCorrect(int index, int value) {
        return index >= 0 && index < CELL_COUNT && value >= 1 && value <= 9 && solution[index] == value;
    }

    public boolean isSolved(int[] currentValues) {
        if (currentValues == null || currentValues.length != CELL_COUNT) {
            return false;
        }
        for (int i = 0; i < CELL_COUNT; i++) {
            if (currentValues[i] != solution[i]) {
                return false;
            }
        }
        return true;
    }

    private void validateConsistency() {
        for (int i = 0; i < CELL_COUNT; i++) {
            int given = givens[i];
            int solved = solution[i];
            if (solved < 1 || solved > 9) {
                throw new IllegalArgumentException("solution values must be 1..9");
            }
            if (given < 0 || given > 9) {
                throw new IllegalArgumentException("given values must be 0..9");
            }
            if (given != 0 && given != solved) {
                throw new IllegalArgumentException("givens must match solution");
            }
        }
    }
}
