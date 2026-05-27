package com.kevy.sudoku.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class SudokuGenerator {
    private static final int SIZE = 9;
    private static final int CELL_COUNT = 81;
    private static final int MAX_ATTEMPTS = 80;

    private final Random random;
    private final SudokuSolver solver = new SudokuSolver();

    public SudokuGenerator() {
        this(new Random());
    }

    public SudokuGenerator(Random random) {
        this.random = random == null ? new Random() : random;
    }

    public SudokuPuzzle generate(Difficulty difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("difficulty is required");
        }
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int[] solution = shuffledSolution();
            int targetHoles = randomBetween(difficulty.getMinHoles(), difficulty.getMaxHoles());
            int[] givens = digHoles(solution, targetHoles);
            int holes = countHoles(givens);
            if (holes >= difficulty.getMinHoles() && holes <= difficulty.getMaxHoles()
                    && solver.countSolutions(givens, 2) == 1) {
                return new SudokuPuzzle(givens, solution, difficulty);
            }
        }

        int[] solution = shuffledSolution();
        int[] givens = digHoles(solution, difficulty.getMinHoles());
        return new SudokuPuzzle(givens, solution, difficulty);
    }

    private int[] digHoles(int[] solution, int targetHoles) {
        int[] puzzle = solution.clone();
        List<Integer> positions = new ArrayList<>(CELL_COUNT);
        for (int i = 0; i < CELL_COUNT; i++) {
            positions.add(i);
        }
        Collections.shuffle(positions, random);

        int holes = 0;
        for (int index : positions) {
            if (holes >= targetHoles) {
                break;
            }
            int oldValue = puzzle[index];
            puzzle[index] = 0;
            if (solver.countSolutions(puzzle, 2) == 1) {
                holes++;
            } else {
                puzzle[index] = oldValue;
            }
        }
        return puzzle;
    }

    private int[] shuffledSolution() {
        int[] rowOrder = shuffledGroupedOrder();
        int[] columnOrder = shuffledGroupedOrder();
        int[] digitMap = shuffledDigits();
        int[] grid = new int[CELL_COUNT];
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                int baseValue = pattern(rowOrder[row], columnOrder[column]);
                grid[row * SIZE + column] = digitMap[baseValue - 1];
            }
        }
        return grid;
    }

    private int[] shuffledGroupedOrder() {
        List<Integer> bands = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            bands.add(i);
        }
        Collections.shuffle(bands, random);

        int[] result = new int[SIZE];
        int offset = 0;
        for (int band : bands) {
            List<Integer> inner = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                inner.add(band * 3 + i);
            }
            Collections.shuffle(inner, random);
            for (int value : inner) {
                result[offset++] = value;
            }
        }
        return result;
    }

    private int[] shuffledDigits() {
        List<Integer> digits = new ArrayList<>(SIZE);
        for (int i = 1; i <= SIZE; i++) {
            digits.add(i);
        }
        Collections.shuffle(digits, random);
        int[] result = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            result[i] = digits.get(i);
        }
        return result;
    }

    private int pattern(int row, int column) {
        return (row * 3 + row / 3 + column) % 9 + 1;
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private int countHoles(int[] puzzle) {
        int holes = 0;
        for (int value : puzzle) {
            if (value == 0) {
                holes++;
            }
        }
        return holes;
    }
}
