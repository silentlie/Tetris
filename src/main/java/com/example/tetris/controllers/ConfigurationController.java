package com.example.tetris.controllers;

import com.example.tetris.helpers.ScenesManager;

// imports for persistence
import com.example.tetris.models.Configuration;
import com.example.tetris.models.Configuration.PlayerType;
import com.example.tetris.services.ConfigurationService;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;



public final class ConfigurationController {

  // sliders + numeric labels
  @FXML private Slider widthSlider;
  @FXML private Label widthValue;
  @FXML private Slider heightSlider;
  @FXML private Label heightValue;
  @FXML private Slider levelSlider;
  @FXML private Label levelValue;

  // toggles
  @FXML private CheckBox musicCheck;
  @FXML private CheckBox soundCheck;
  @FXML private CheckBox aiPlayCheck;
  @FXML private CheckBox extendedCheck;

  @FXML private Button saveButton;
  @FXML private Button resetButton;
  @FXML private Button backButton;

  // Services and cached config
  private ConfigurationService service;
  private Configuration cachedConfig;

  // Setter injection called FXMLLoader factory
  public void setConfigurationService(ConfigurationService service) {
    this.service = service;
  }

  @FXML
  private void initialize() {
    if (service == null) {
      throw new IllegalStateException("ConfigurationService not injected.");
    }


    // Set slider ranges
    widthSlider.setMin(5); widthSlider.setMax(15);
    heightSlider.setMin(15); heightSlider.setMax(30);
    levelSlider.setMin(1); levelSlider.setMax(10);

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



    // load and display config
    cachedConfig = service.load();
    setFormFromConfig(cachedConfig);

    // Disable Player 2 option when extend mode is off
    extendedCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
      aiPlayCheck.setDisable(!newVal);
    });
    aiPlayCheck.setDisable(!extendedCheck.isSelected()); // initialise state

    // Button actions can now use service and cachedConfig
    saveButton.setOnAction(e -> handleSave());
    resetButton.setOnAction(e -> handleReset());
    backButton.setOnAction(e -> handleBack());

  }

  private void handleSave() {
    Configuration updated = formToConfig();
    service.save(updated);
    cachedConfig = updated;
  }

  private void handleReset() {
    Configuration defaults = new Configuration();
    setFormFromConfig(defaults);
    service.save(defaults);
    cachedConfig = defaults;
  }

  private void handleBack() {
    Configuration current = formToConfig();
    if (!current.equals(cachedConfig)) {
      Alert alert = new Alert(AlertType.CONFIRMATION, "You have unsaved changes. Do you want to discard them?", ButtonType.YES, ButtonType.NO);
      alert.setTitle("Unsaved Changes");
      alert.setHeaderText(null);

      alert.showAndWait().ifPresent(response -> {
        if (response == ButtonType.YES) {
          ScenesManager.getInstance().showMenuScene();
        }
        // else do nothing - stay on screen
      });

    } else {
      ScenesManager.getInstance().showMenuScene();
    }
  }

  // Helpers

  private Configuration formToConfig() {
    return new Configuration(
        (int) widthSlider.getValue(),
        (int) heightSlider.getValue(),
        (int) levelSlider.getValue(),
        musicCheck.isSelected(),
        soundCheck.isSelected(),
        extendedCheck.isSelected(),
        Configuration.PlayerType.HUMAN,
        aiPlayCheck.isSelected() ? Configuration.PlayerType.AI : Configuration.PlayerType.HUMAN
    );
  }

  private void setFormFromConfig(Configuration cfg) {
    widthSlider.setValue(cfg.width());
    heightSlider.setValue(cfg.height());
    levelSlider.setValue(cfg.level());
    musicCheck.setSelected(cfg.music());
    soundCheck.setSelected(cfg.sound());
    extendedCheck.setSelected(cfg.extendMode());
    aiPlayCheck.setSelected(cfg.player2() == Configuration.PlayerType.AI);
  }
}
