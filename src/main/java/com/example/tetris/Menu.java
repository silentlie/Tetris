package com.example.tetris;

import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class Menu {

  @FXML
  private void onPlayButtonClick(ActionEvent actionEvent) {
    ScenesManager.getInstance().showPlayScene();
  }

  @FXML
  private void onHighScoresButtonClick(ActionEvent actionEvent) {
    ScenesManager.getInstance().showHighScoresScene();
  }

  @FXML
  private void onSettingsButtonClick(ActionEvent actionEvent) {
    ScenesManager.getInstance().showSettingsScene();
  }

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
