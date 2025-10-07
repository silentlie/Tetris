package com.example.tetris.controllers;

import com.example.tetris.helpers.AppContext;
import com.example.tetris.helpers.FxController;
import com.example.tetris.helpers.HighScoreManager;
import com.example.tetris.helpers.Route;
import com.example.tetris.models.HighScore;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class HighScoreController implements FxController {

    private AppContext ctx;

    @FXML
    private VBox root;

    @FXML
    private void initialize() {
        // ✅ Load saved high scores from JSON file
        HighScoreManager manager = new HighScoreManager();
        List<HighScore> scores = manager.getScores();

        // Insert rows ABOVE the Back button (which is currently the last child)
        int insertIndex = Math.max(root.getChildren().size() - 1, 0);
        for (int i = 0; i < scores.size(); i++) {
            HighScore s = scores.get(i);
            root.getChildren().add(insertIndex + i, makeRow(i + 1, s));
        }
    }

    /**
     * Builds a single row like: "1)  Name ....... Score"
     */
    private Node makeRow(int rank, HighScore s) {
        Label left = new Label(rank + ")  " + s.getName());
        Label right = new Label(String.valueOf(s.getScore()));

        left.setStyle("-fx-font-size: 14px;");
        right.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, left, spacer, right);
        row.setMaxWidth(360);
        return row;
    }

    @FXML
    private void onBackButtonClick() {
        ctx.show(Route.MENU);
    }

    @Override
    public void setContext(AppContext ctx) {
        this.ctx = ctx;
    }
}
