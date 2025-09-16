package com.example.tetris.controllers;

import com.example.tetris.helpers.AppContext;
import com.example.tetris.helpers.FxController;
import com.example.tetris.helpers.Route;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Controller for the main menu scene.
 *
 * <p>This class handles user interaction with the menu buttons and delegates
 * navigation through the shared {@link AppContext}.</p>
 */
public class MenuController implements FxController {

  private AppContext ctx;

  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;
  }

  /**
   * Handles the <strong>Play</strong> button click.
   *
   * <p>Navigates to the gameplay scene.</p>
   *
   * @param actionEvent the event triggered by clicking the Play button
   */
  @FXML
  private void onPlayButtonClick(ActionEvent actionEvent) {
    ctx.show(Route.PLAY);
  }

  /**
   * Handles the <strong>High Scores</strong> button click.
   *
   * <p>Navigates to the high scores scene.</p>
   *
   * @param actionEvent the event triggered by clicking the High Scores button
   */
  @FXML
  private void onHighScoresButtonClick(ActionEvent actionEvent) {
    ctx.show(Route.HIGH_SCORE);
  }

  /**
   * Handles the <strong>Configuration</strong> button click.
   *
   * <p>Navigates to the configuration scene.</p>
   *
   * @param actionEvent the event triggered by clicking the Configuration button
   */
  @FXML
  private void onConfigurationButtonClick(ActionEvent actionEvent) {
    ctx.show(Route.CONFIGURATION);
  }

  /**
   * Handles the <strong>Exit</strong> button click.
   *
   * <p>Shows a confirmation dialog. If the user confirms, the application window is closed.</p>
   *
   * @param actionEvent the event triggered by clicking the Exit button
   */
  @FXML
  private void onExitButtonClick(ActionEvent actionEvent) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Exit Confirmation");
    alert.setHeaderText("Are you sure you want to exit?");
    alert.setContentText("Confirm to close the application.");
    alert.initOwner(ctx.getStage());
    Optional<ButtonType> result = alert.showAndWait();

    Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      stage.close();
    }
  }
}
