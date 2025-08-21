package com.example.tetris.controllers;

import com.example.tetris.helpers.ScenesManager;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Controller class for the main menu scene.
 *
 * <p>This class handles user interaction with the menu buttons and delegates scene changes
 * to {@link ScenesManager}.</p>
 */
public class Menu {

  /**
   * Handles the Play button click event.
   *
   * <p>Switches to the gameplay scene.</p>
   *
   * @param actionEvent the event triggered by clicking the Play button
   */
  @FXML
  private void onPlayButtonClick(ActionEvent actionEvent) {
    ScenesManager.getInstance().showPlayScene();
  }

  /**
   * Handles the High Scores button click event.
   *
   * <p>Switches to the high scores scene.</p>
   *
   * @param actionEvent the event triggered by clicking the High Scores button
   */
  @FXML
  private void onHighScoresButtonClick(ActionEvent actionEvent) {
    ScenesManager.getInstance().showHighScoresScene();
  }

  /**
   * Handles the Settings button click event.
   *
   * <p>Switches to the settings scene.</p>
   *
   * @param actionEvent the event triggered by clicking the Settings button
   */
  @FXML
  private void onSettingsButtonClick(ActionEvent actionEvent) {
    ScenesManager.getInstance().showSettingsScene();
  }

  /**
   * Handles the Exit button click event.
   *
   * <p>Shows a confirmation dialog. If the user confirms, the application is closed.</p>
   *
   * @param actionEvent the event triggered by clicking the Exit button
   */
  @FXML
  private void onExitButtonClick(ActionEvent actionEvent) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Exit Confirmation");
    alert.setHeaderText("Are you sure you want to exit?");
    alert.setContentText("Confirm to close the application.");
    Optional<ButtonType> result = alert.showAndWait();

    Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      stage.close();
    }
  }
}
