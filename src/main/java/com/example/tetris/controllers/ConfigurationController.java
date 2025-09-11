package com.example.tetris.controllers;

import com.example.tetris.helpers.AppContext;
import com.example.tetris.helpers.FxController;
import com.example.tetris.helpers.Route;
import com.example.tetris.models.Configuration;
import com.example.tetris.models.Configuration.PlayerType;
import com.example.tetris.services.ConfigurationService;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public final class ConfigurationController implements FxController {

  private AppContext ctx;

  // ---------- UI elements ---------------
  // sliders with numeric labels
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

  // Checkbox toggles
  @FXML
  private CheckBox musicCheck;
  @FXML
  private CheckBox soundCheck;
  @FXML
  private CheckBox extendedCheck;

  // Player selectors - drop-downs for human, AI and external
  @FXML
  private javafx.scene.control.ChoiceBox<PlayerType> player1Choice;
  @FXML
  private javafx.scene.control.ChoiceBox<PlayerType> player2Choice;

  // Container for player 2 controls so they can be enabled/disabled as a group
  @FXML
  private javafx.scene.layout.HBox player2Box;

  // Action buttons
  @FXML
  private Button saveButton;
  @FXML
  private Button resetButton;
  @FXML
  private Button backButton;


  // --------- Internal code --------------
  // Services and cached config
  private ConfigurationService service; // used to load/save config.json
  private Configuration cachedConfig; // last saved/loaded config for change detection

  // Setter injection from FXMLLoader factory
  public void setConfigurationService(ConfigurationService service) {
    this.service = service;
  }

  @Override
  public void setContext(AppContext ctx) {
    this.ctx = ctx;
  }

  @FXML
  private void initialize() {
    if (service == null) {
      throw new IllegalStateException("ConfigurationService not injected.");
    }

    // Set slider ranges
    widthSlider.setMin(5);
    widthSlider.setMax(15);
    heightSlider.setMin(15);
    heightSlider.setMax(30);
    levelSlider.setMin(1);
    levelSlider.setMax(10);

    // Bind slider values to live numeric labels
    widthValue.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf((int) widthSlider.getValue()),
            widthSlider.valueProperty()));
    heightValue.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf((int) heightSlider.getValue()),
            heightSlider.valueProperty()));
    levelValue.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf((int) levelSlider.getValue()),
            levelSlider.valueProperty()));

    // load persisting configuration
    cachedConfig = service.load();

    // Populate player choice boxes with enum values
    player1Choice.getItems().setAll(PlayerType.values());
    player2Choice.getItems().setAll(PlayerType.values());

    // apply loaded config to ui
    setFormFromConfig(cachedConfig);

    // Disable player 2 group if extend mode is off
    player2Box.setDisable(!extendedCheck.isSelected());
    extendedCheck.selectedProperty()
        .addListener((obs, oldVal, newVal) -> player2Box.setDisable(!newVal));

    // Button actions
    saveButton.setOnAction(e -> handleSave());
    resetButton.setOnAction(e -> handleReset());
    backButton.setOnAction(e -> handleBack());

  }

  // --------- Button Handlers ------------

  // Save current form values to config.json and update cachedConfig
  private void handleSave() {
    Configuration updated = formToConfig();
    service.save(updated);
    cachedConfig = updated;
  }

  // Reset form to default values, persist them and updated cachedConfig
  private void handleReset() {
    Configuration defaults = new Configuration();
    setFormFromConfig(defaults);
    service.save(defaults);
    cachedConfig = defaults;
  }

  // If there are any unsaved changes, confirm before returning to menu
  private void handleBack() {
    Configuration current = formToConfig();
    if (!current.equals(cachedConfig)) {
      Alert alert = new Alert(AlertType.CONFIRMATION,
          "You have unsaved changes. Do you want to discard them?", ButtonType.YES, ButtonType.NO);
      alert.setTitle("Unsaved Changes");
      alert.setHeaderText(null);

      alert.showAndWait().ifPresent(response -> {
        if (response == ButtonType.YES) {
          ctx.show(Route.MENU);
        }
        // else do nothing - stay on screen
      });

    } else {
      ctx.show(Route.MENU);
    }
  }

  // --------- Helpers -----------

  // Convert current UI state into a configuration record
  private Configuration formToConfig() {
    PlayerType p1 = player1Choice.getValue() != null ? player1Choice.getValue() : PlayerType.HUMAN;
    PlayerType p2 = player2Choice.getValue() != null ? player2Choice.getValue() : PlayerType.HUMAN;

    return new Configuration((int) widthSlider.getValue(), (int) heightSlider.getValue(),
        (int) levelSlider.getValue(), musicCheck.isSelected(), soundCheck.isSelected(),
        extendedCheck.isSelected(), p1, p2);
  }

  // Apply a configuration record's values to the ui controls
  private void setFormFromConfig(Configuration cfg) {
    widthSlider.setValue(cfg.width());
    heightSlider.setValue(cfg.height());
    levelSlider.setValue(cfg.level());
    musicCheck.setSelected(cfg.music());
    soundCheck.setSelected(cfg.sound());
    extendedCheck.setSelected(cfg.extendMode());
    player1Choice.setValue(cfg.player1());
    player2Choice.setValue(cfg.player2());
    player2Box.setDisable(!cfg.extendMode());
  }
}
