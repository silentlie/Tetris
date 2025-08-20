package com.example.tetris;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.prefs.Preferences;

public class HighScore {

    private static final String KEY = "HighScore";
    private final Preferences prefs = Preferences.userNodeForPackage(HighScore.class);
    @FXML
    private Label highScoreLabel;

    @FXML
    private void initialize() {
        int saved = prefs.getInt(KEY, 0);
        highScoreLabel.setText(Integer.toString(saved));
    }

    @FXML
    private void onBackButtonClick(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(
                Objects.requireNonNull(HighScore.class.getResource("/com/example/tetris/hello-view.fxml"))
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}


