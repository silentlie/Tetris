package com.example.tetris.helpers;

import com.example.tetris.models.HighScore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighScoreManager {
    private static final String FILE_PATH = "highscores.json";
    private List<HighScore> scores = new ArrayList<>();

    public HighScoreManager() {
        load();
    }

    public List<HighScore> getScores() {
        return scores;
    }

    public void addScore(HighScore s) {
        scores.add(s);
        Collections.sort(scores);
        if (scores.size() > 10) scores = scores.subList(0, 10);
        save();
    }

    public void clear() {
        scores.clear();
        save();
    }

    private void load() {
        try (FileReader reader = new FileReader(FILE_PATH)) {
            Type listType = new TypeToken<List<HighScore>>() {
            }.getType();
            scores = new Gson().fromJson(reader, listType);
            if (scores == null) scores = new ArrayList<>();
        } catch (Exception e) {
            scores = new ArrayList<>();
        }
    }

    private void save() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            new Gson().toJson(scores, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
