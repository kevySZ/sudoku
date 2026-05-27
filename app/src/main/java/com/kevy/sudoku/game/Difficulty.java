package com.kevy.sudoku.game;

public enum Difficulty {
    BEGINNER(36, 40),
    INTERMEDIATE(42, 46),
    ADVANCED(48, 52),
    NIGHTMARE(52, 56);

    private final int minHoles;
    private final int maxHoles;

    Difficulty(int minHoles, int maxHoles) {
        this.minHoles = minHoles;
        this.maxHoles = maxHoles;
    }

    public int getMinHoles() {
        return minHoles;
    }

    public int getMaxHoles() {
        return maxHoles;
    }
}
