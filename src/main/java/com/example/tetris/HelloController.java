package com.example.tetris;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller for the main menu screen (hello-view.fxml).
 */
public class HelloController {

    @FXML
    private Label welcomeText;

    /**
     * Handler for the "Hello!" button.
     */
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    /**
     * Handler for the "Open Configuration" button.
     * Loads Configuration.fxml and switches the scene.
     */
    @FXML
    protected void openConfiguration() throws Exception {
        Stage stage = (Stage) welcomeText.getScene().getWindow();
        Scene scene = new Scene(FXMLLoader.load(
                getClass().getResource("Configuration.fxml")));
        stage.setScene(scene);
        stage.show();
    }
}
