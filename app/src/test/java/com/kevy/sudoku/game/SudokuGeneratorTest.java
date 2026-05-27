package com.kevy.sudoku.game;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SudokuGeneratorTest {

    @Test
    public void generateCreatesValidUniquePuzzleForEveryDifficulty() {
        SudokuGenerator generator = new SudokuGenerator();
        SudokuSolver solver = new SudokuSolver();

        for (Difficulty difficulty : Difficulty.values()) {
            SudokuPuzzle puzzle = generator.generate(difficulty);

            assertEquals(81, puzzle.getGivens().length);
            assertEquals(81, puzzle.getSolution().length);
            assertEquals(difficulty, puzzle.getDifficulty());
            assertConsistentGivens(puzzle);
            assertHoleCountInDifficultyRange(puzzle);
            assertEquals(1, solver.countSolutions(puzzle.getGivens(), 2));
        }
    }

    @Test
    public void puzzleDefensivelyCopiesArrays() {
        int[] givens = solvedGrid();
        int[] solution = solvedGrid();
        givens[0] = 0;

        SudokuPuzzle puzzle = new SudokuPuzzle(givens, solution, Difficulty.BEGINNER);
        givens[1] = 0;
        solution[0] = 9;

        assertEquals(1, puzzle.getGivens()[1]);
        assertEquals(1, puzzle.getSolution()[0]);

        int[] returnedGivens = puzzle.getGivens();
        int[] returnedSolution = puzzle.getSolution();
        returnedGivens[2] = 0;
        returnedSolution[2] = 0;

        assertNotSame(returnedGivens, puzzle.getGivens());
        assertNotSame(returnedSolution, puzzle.getSolution());
        assertEquals(1, puzzle.getGivens()[2]);
        assertEquals(1, puzzle.getSolution()[2]);
    }

    @Test
    public void isCorrectUsesSolvedValueAndRejectsInvalidIndexesOrValues() {
        int[] givens = solvedGrid();
        int[] solution = solvedGrid();
        givens[10] = 0;
        SudokuPuzzle puzzle = new SudokuPuzzle(givens, solution, Difficulty.BEGINNER);

        assertTrue(puzzle.isGiven(0));
        assertFalse(puzzle.isGiven(10));
        assertTrue(puzzle.isCorrect(10, solution[10]));
        assertFalse(puzzle.isCorrect(10, differentValue(solution[10])));
        assertFalse(puzzle.isCorrect(-1, solution[10]));
        assertFalse(puzzle.isCorrect(81, solution[10]));
        assertFalse(puzzle.isCorrect(10, 0));
        assertFalse(puzzle.isCorrect(10, 10));
    }

    @Test
    public void isSolvedRequiresCompleteMatchingGridAndKeepsGivensFixed() {
        int[] givens = solvedGrid();
        int[] solution = solvedGrid();
        givens[10] = 0;
        SudokuPuzzle puzzle = new SudokuPuzzle(givens, solution, Difficulty.BEGINNER);

        assertTrue(puzzle.isSolved(solution));

        int[] incomplete = solution.clone();
        incomplete[10] = 0;
        assertFalse(puzzle.isSolved(incomplete));

        int[] wrongEmptyCell = solution.clone();
        wrongEmptyCell[10] = differentValue(solution[10]);
        assertFalse(puzzle.isSolved(wrongEmptyCell));

        int[] changedGiven = solution.clone();
        changedGiven[0] = differentValue(solution[0]);
        assertFalse(puzzle.isSolved(changedGiven));

        assertFalse(puzzle.isSolved(new int[80]));
        assertFalse(puzzle.isSolved(null));
    }

    private static void assertConsistentGivens(SudokuPuzzle puzzle) {
        int[] givens = puzzle.getGivens();
        int[] solution = puzzle.getSolution();
        for (int i = 0; i < givens.length; i++) {
            if (givens[i] != 0) {
                assertEquals(solution[i], givens[i]);
                assertTrue(puzzle.isGiven(i));
            } else {
                assertFalse(puzzle.isGiven(i));
            }
        }
    }

    private static void assertHoleCountInDifficultyRange(SudokuPuzzle puzzle) {
        int holes = 0;
        for (int value : puzzle.getGivens()) {
            if (value == 0) {
                holes++;
            }
        }
        Difficulty difficulty = puzzle.getDifficulty();
        assertTrue(holes >= difficulty.getMinHoles());
        assertTrue(holes <= difficulty.getMaxHoles());
    }

    private static int[] solvedGrid() {
        int[] values = new int[81];
        for (int i = 0; i < values.length; i++) {
            int row = i / 9;
            int column = i % 9;
            values[i] = (row * 3 + row / 3 + column) % 9 + 1;
        }
        return values;
    }

    private static int differentValue(int value) {
        return value == 9 ? 1 : value + 1;
    }
}
