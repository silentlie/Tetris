package com.example.tetris.models;

public class HighScore implements Comparable<HighScore> {
    private final String name;
    private final int score;

    public HighScore(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    @Override
    public int compareTo(HighScore o) {
        return Integer.compare(o.score, this.score); // descending order
    }
}
