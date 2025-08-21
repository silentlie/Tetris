package com.example.tetris;

import java.io.IOException;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;

public final class Configuration {

  // sliders + numeric labels
  @FXML
  private Slider widthSlider;
  @FXML
  private Label widthValue;
  @FXML
  private Slider heightSlider;
  @FXML
  private Label heightValue;
  @FXML
  private Slider levelSlider;
  @FXML
  private Label levelValue;

  // toggles + On/Off labels
  @FXML
  private CheckBox musicCheck;
  @FXML
  private Label musicValue;
  @FXML
  private CheckBox soundCheck;
  @FXML
  private Label soundValue;
  @FXML
  private CheckBox aiPlayCheck;
  @FXML
  private Label aiPlayValue;
  @FXML
  private CheckBox extendedCheck;
  @FXML
  private Label extendedValue;

  @FXML
  private Button saveButton;
  @FXML
  private Button resetButton;
  @FXML
  private Button backButton;

  private static void bindOnOff(Label label, CheckBox box) {
    label.textProperty().bind(Bindings.when(box.selectedProperty()).then("On").otherwise("Off"));
  }

  @FXML
  private void initialize() {
    // ranges
    widthSlider.setMin(5);
    widthSlider.setMax(15);
    heightSlider.setMin(15);
    heightSlider.setMax(30);
    levelSlider.setMin(1);
    levelSlider.setMax(10);

    // friendly defaults (just for UI; NOT wired to gameplay)
    widthSlider.setValue(10);
    heightSlider.setValue(20);
    levelSlider.setValue(1);
    musicCheck.setSelected(true);
    soundCheck.setSelected(true);
    aiPlayCheck.setSelected(false);
    extendedCheck.setSelected(false);

    // live numeric labels
    widthValue.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf((int) widthSlider.getValue()),
            widthSlider.valueProperty()));
    heightValue.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf((int) heightSlider.getValue()),
            heightSlider.valueProperty()));
    levelValue.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf((int) levelSlider.getValue()),
            levelSlider.valueProperty()));

    // live On/Off mirrors
    bindOnOff(musicValue, musicCheck);
    bindOnOff(soundValue, soundCheck);
    bindOnOff(aiPlayValue, aiPlayCheck);
    bindOnOff(extendedValue, extendedCheck);

    // Save = no-op for gameplay in Milestone One (kept simple)
    saveButton.setOnAction(e -> { /* keep values on screen only */ });

    // Reset UI to defaults
    resetButton.setOnAction(e -> {
      widthSlider.setValue(10);
      heightSlider.setValue(20);
      levelSlider.setValue(1);
      musicCheck.setSelected(true);
      soundCheck.setSelected(true);
      aiPlayCheck.setSelected(false);
      extendedCheck.setSelected(false);
    });

    // Back → load your main screen (hello-view.fxml)
    backButton.setOnAction(e -> {
      try {
        Stage stage = (Stage) backButton.getScene().getWindow();
        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("hello-view.fxml")));
        stage.setScene(scene);
        stage.show();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    });
  }
}
