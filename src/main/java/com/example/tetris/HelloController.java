package com.example.tetris;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloController {

    @FXML
    private void onPlayButtonClick(ActionEvent event) {
        // TODO: switch to your game scene when ready
        System.out.println("Play clicked!");
    }

    @FXML
    private void onHighScores(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(
                HelloController.class.getResource("/com/example/tetris/HighScore.fxml")
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    private void onExitButtonClick(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
